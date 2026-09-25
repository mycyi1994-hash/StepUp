// SPDX-License-Identifier: MIT
pragma solidity ^0.8.28;

import {IERC20} from "@openzeppelin/contracts/token/ERC20/IERC20.sol";
import {SafeERC20} from "@openzeppelin/contracts/token/ERC20/utils/SafeERC20.sol";
import {Ownable} from "@openzeppelin/contracts/access/Ownable.sol";
import {Ownable2Step} from "@openzeppelin/contracts/access/Ownable2Step.sol";
import {Pausable} from "@openzeppelin/contracts/utils/Pausable.sol";
import {EIP712} from "@openzeppelin/contracts/utils/cryptography/EIP712.sol";
import {ECDSA} from "@openzeppelin/contracts/utils/cryptography/ECDSA.sol";

/**
 * @title RewardDistributor
 * @notice Converts an off-chain StepUp run into on-chain SUP, under a hard
 *         daily emission budget.
 *
 * ## The division of labour
 *
 * The attester decides *who* gets *how much*: it receives the session record
 * the Android client produces — `(steps, distance, boost, payout)` plus the GPS
 * polyline — runs the plausibility checks (cadence, speed, teleport, device
 * attestation) and signs an EIP-712 claim.
 *
 * This contract decides *how much may exist*. It does not trust the attester
 * with the supply: every claim is charged against `dailyBudget(day)`, and once
 * that day's budget is spent, further claims for that day revert no matter who
 * signed them. A compromised attester can misdirect a day's emission; it cannot
 * inflate the token.
 *
 * That split is the whole design. Fraud detection is a moving target and
 * belongs off chain where it can be updated; the supply cap is not, and belongs
 * here where it cannot.
 *
 * ## Emission schedule
 *
 * `dailyBudget` starts at 250,000 SUP and halves every 730 days. The geometric
 * series converges to 365,000,000 SUP — see docs/TOKENOMICS.md §7.2. The pool
 * this contract holds is whatever the treasury funds it with; the budget is the
 * ceiling, never a guarantee.
 *
 * ## What this contract deliberately cannot do
 *
 * There is no sweep, no rescue and no owner withdrawal. SUP that enters this
 * contract can only leave through `claim`, to a runner. The owner can replace
 * the attester (so a lost signing key is recoverable) and pause claims, and
 * that is the entire privileged surface.
 */
contract RewardDistributor is EIP712, Ownable2Step, Pausable {
    using SafeERC20 for IERC20;

    /// @notice Emission budget for the first epoch, per day.
    uint256 public constant INITIAL_DAILY_BUDGET = 250_000 ether;

    /// @notice Days per halving epoch.
    uint64 public constant EPOCH_DAYS = 730;

    /// @notice How far back a session may still be claimed.
    uint64 public constant CLAIM_WINDOW_DAYS = 7;

    /// @notice Beyond this epoch the budget has rounded to nothing.
    uint64 private constant _MAX_EPOCH = 63;

    /// @notice SUP paid out to runners.
    IERC20 public immutable sup;

    /// @notice Day 0 of the emission schedule — the deployment timestamp.
    uint64 public immutable startTimestamp;

    /// @notice Signs run proofs. Replaceable; see the contract-level note.
    address public attester;

    /// Hot key that may pause (never unpause). The attester Worker holds it so it
    /// can stop claims the moment it sees something wrong.
    address public guardian;

    /// Owner-set ceiling below the emission budget — limits the damage if the
    /// attester key ever leaks. 0 = only the emission budget applies.
    uint256 public dailyCap;

    /// Largest single claim. 0 = no per-claim limit.
    uint256 public maxClaim;

    /// Paid out on each calendar day, whatever `day` the claims name. A claim may
    /// name any of the last 8 days, so without this a leaked key could draw 8
    /// days of `dailyCap` at once.
    mapping(uint64 payoutDay => uint256 paid) public paidOnPayoutDay;

    /// @notice Session hashes already paid. Replay protection.
    mapping(bytes32 sessionHash => bool claimed) public sessionClaimed;

    /// @notice SUP already emitted for a given schedule day.
    mapping(uint64 day => uint256 paid) public dailyPaid;

    /// @notice Lifetime SUP emitted by this contract.
    uint256 public totalDistributed;

    /// @notice Lifetime SUP claimed per runner.
    mapping(address runner => uint256 total) public claimedBy;

    bytes32 private constant _CLAIM_TYPEHASH =
        keccak256("Claim(address runner,bytes32 sessionHash,uint256 amount,uint64 day,uint256 deadline)");

    struct Claim {
        address runner;
        bytes32 sessionHash;
        uint256 amount;
        uint64 day;
        uint256 deadline;
    }

    // ── Events ───────────────────────────────────────────────────────────

    event Claimed(
        address indexed runner, bytes32 indexed sessionHash, uint256 amount, uint64 indexed day, uint256 dayRemaining
    );
    event AttesterUpdated(address indexed previous, address indexed current);
    event PoolFunded(address indexed from, uint256 amount);
    event GuardianUpdated(address indexed previous, address indexed current);
    event LimitsUpdated(uint256 dailyCap, uint256 maxClaim);

    // ── Errors ───────────────────────────────────────────────────────────

    error ClaimExpired(uint256 deadline);
    error SessionAlreadyClaimed(bytes32 sessionHash);
    error BadSignature();
    error ZeroAmount();
    error DayInFuture(uint64 day, uint64 currentDay);
    error DayTooOld(uint64 day, uint64 currentDay);
    error DailyBudgetExceeded(uint64 day, uint256 requested, uint256 remaining);
    error PoolExhausted(uint256 requested, uint256 balance);
    error ClaimTooLarge(uint256 amount, uint256 maxClaim);
    error NotGuardian();
    error PayoutCapReached(uint64 payoutDay, uint256 requested, uint256 remaining);
    error CannotRenounce();

    constructor(address supToken, address attester_) EIP712("StepUpRewards", "1") Ownable(msg.sender) {
        require(supToken != address(0), "RD: sup is zero");
        require(attester_ != address(0), "RD: attester is zero");
        sup = IERC20(supToken);
        attester = attester_;
        startTimestamp = uint64(block.timestamp);
    }

    // ── Emission schedule ────────────────────────────────────────────────

    /// @notice Days elapsed since deployment. Day 0 is deployment day.
    function currentDay() public view returns (uint64) {
        return (uint64(block.timestamp) - startTimestamp) / 1 days;
    }

    /// @notice Halving epoch a given day falls in.
    function epochOf(uint64 day) public pure returns (uint64) {
        return day / EPOCH_DAYS;
    }

    /// @notice Emission ceiling for a given schedule day.
    function dailyBudget(uint64 day) public pure returns (uint256) {
        uint64 epoch = day / EPOCH_DAYS;
        if (epoch > _MAX_EPOCH) return 0;
        return INITIAL_DAILY_BUDGET >> epoch;
    }

    /// @notice Emission still available for a given day.
    function dayRemaining(uint64 day) public view returns (uint256) {
        uint256 budget = dailyBudget(day);
        if (dailyCap != 0 && dailyCap < budget) budget = dailyCap;
        uint256 paid = dailyPaid[day];
        return paid >= budget ? 0 : budget - paid;
    }

    /// @notice SUP left in the reward pool.
    function poolBalance() public view returns (uint256) {
        return sup.balanceOf(address(this));
    }

    // ── Claiming ─────────────────────────────────────────────────────────

    /// @notice Hash a claim for off-chain signing by the attester.
    function hashClaim(Claim calldata c) public view returns (bytes32) {
        return _hashTypedDataV4(
            keccak256(abi.encode(_CLAIM_TYPEHASH, c.runner, c.sessionHash, c.amount, c.day, c.deadline))
        );
    }

    /**
     * @notice Pay an attested run.
     * @dev Anyone may submit; the SUP always goes to `c.runner`, so a relayer
     *      can cover gas for a user who has none. That matters: a runner who
     *      has never touched a chain should not need ETH to receive their first
     *      reward.
     */
    function claim(Claim calldata c, bytes calldata signature) external whenNotPaused {
        if (block.timestamp > c.deadline) revert ClaimExpired(c.deadline);
        if (c.amount == 0) revert ZeroAmount();
        if (maxClaim != 0 && c.amount > maxClaim) revert ClaimTooLarge(c.amount, maxClaim);
        if (sessionClaimed[c.sessionHash]) revert SessionAlreadyClaimed(c.sessionHash);

        uint64 today = currentDay();
        if (c.day > today) revert DayInFuture(c.day, today);
        if (today - c.day > CLAIM_WINDOW_DAYS) revert DayTooOld(c.day, today);

        address signer = ECDSA.recover(hashClaim(c), signature);
        if (signer != attester) revert BadSignature();

        uint256 remaining = dayRemaining(c.day);
        if (c.amount > remaining) revert DailyBudgetExceeded(c.day, c.amount, remaining);

        if (dailyCap != 0) {
            uint256 paidToday = paidOnPayoutDay[today];
            uint256 left = paidToday >= dailyCap ? 0 : dailyCap - paidToday;
            if (c.amount > left) revert PayoutCapReached(today, c.amount, left);
        }

        uint256 balance = poolBalance();
        if (c.amount > balance) revert PoolExhausted(c.amount, balance);

        sessionClaimed[c.sessionHash] = true;
        dailyPaid[c.day] += c.amount;
        paidOnPayoutDay[today] += c.amount;
        totalDistributed += c.amount;
        claimedBy[c.runner] += c.amount;

        sup.safeTransfer(c.runner, c.amount);
        emit Claimed(c.runner, c.sessionHash, c.amount, c.day, remaining - c.amount);
    }

    // ── Funding ──────────────────────────────────────────────────────────

    /**
     * @notice Pull SUP into the reward pool.
     * @dev A plain transfer works too; this exists so funding is an event.
     */
    function fund(uint256 amount) external {
        sup.safeTransferFrom(msg.sender, address(this), amount);
        emit PoolFunded(msg.sender, amount);
    }

    // ── Admin ────────────────────────────────────────────────────────────

    function setAttester(address attester_) external onlyOwner {
        require(attester_ != address(0), "RD: attester is zero");
        emit AttesterUpdated(attester, attester_);
        attester = attester_;
    }

    /// @notice Without an owner nobody could unpause after a guardian pause.
    function renounceOwnership() public pure override {
        revert CannotRenounce();
    }

    function setGuardian(address guardian_) external onlyOwner {
        emit GuardianUpdated(guardian, guardian_);
        guardian = guardian_;
    }

    function setLimits(uint256 dailyCap_, uint256 maxClaim_) external onlyOwner {
        dailyCap = dailyCap_;
        maxClaim = maxClaim_;
        emit LimitsUpdated(dailyCap_, maxClaim_);
    }

    /// @notice Owner or guardian can stop claims. Only the owner can resume.
    function pause() external {
        if (msg.sender != owner() && msg.sender != guardian) revert NotGuardian();
        _pause();
    }

    function unpause() external onlyOwner {
        _unpause();
    }
}
