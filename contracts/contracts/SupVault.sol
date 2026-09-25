// SPDX-License-Identifier: MIT
pragma solidity ^0.8.28;

import {IERC20} from "@openzeppelin/contracts/token/ERC20/IERC20.sol";
import {IERC20Permit} from "@openzeppelin/contracts/token/ERC20/extensions/IERC20Permit.sol";
import {SafeERC20} from "@openzeppelin/contracts/token/ERC20/utils/SafeERC20.sol";
import {Ownable} from "@openzeppelin/contracts/access/Ownable.sol";
import {Ownable2Step} from "@openzeppelin/contracts/access/Ownable2Step.sol";
import {Pausable} from "@openzeppelin/contracts/utils/Pausable.sol";

interface IRewardPool {
    function fund(uint256 amount) external;
}

/**
 * @title SupVault
 * @notice Wallet → app for SUP. A runner deposits SUP here with their StepUp
 *         account id; the attester sees `Deposited` (after it is final) and the
 *         server credits the in-app balance once per (tx, log).
 *
 * The vault has exactly one way out: `recycle`, which sends SUP into the reward
 * pool. There is no withdraw, no sweep and no arbitrary transfer, so even the
 * owner key cannot take deposited SUP anywhere but back to runners via claims.
 */
contract SupVault is Ownable2Step, Pausable {
    using SafeERC20 for IERC20;

    IERC20 public immutable sup;
    address public immutable rewardPool;

    /// Smallest deposit — keeps the event log free of dust spam.
    uint256 public minDeposit = 1 ether;
    address public guardian;
    uint256 public totalDeposited;

    event Deposited(address indexed from, bytes32 indexed account, uint256 amount);
    event Recycled(uint256 amount);
    event MinDepositUpdated(uint256 amount);
    event GuardianUpdated(address indexed previous, address indexed current);

    error ZeroAccount();
    error BelowMinimum(uint256 amount, uint256 minimum);
    error NotGuardian();

    constructor(address supToken, address rewardPool_, address guardian_) Ownable(msg.sender) {
        require(supToken != address(0) && rewardPool_ != address(0), "VAULT: zero address");
        sup = IERC20(supToken);
        rewardPool = rewardPool_;
        guardian = guardian_;
    }

    function deposit(uint256 amount, bytes32 account) public whenNotPaused {
        if (account == bytes32(0)) revert ZeroAccount();
        if (amount < minDeposit) revert BelowMinimum(amount, minDeposit);
        sup.safeTransferFrom(msg.sender, address(this), amount);
        totalDeposited += amount;
        emit Deposited(msg.sender, account, amount);
    }

    /// @notice Deposit with an EIP-2612 signature instead of a separate approve tx.
    function depositWithPermit(uint256 amount, bytes32 account, uint256 deadline, uint8 v, bytes32 r, bytes32 s)
        external
    {
        // A front-runner may have used the permit already; the allowance is what matters.
        try IERC20Permit(address(sup)).permit(msg.sender, address(this), amount, deadline, v, r, s) {} catch {}
        deposit(amount, account);
    }

    /// @notice Send deposited SUP back into the reward pool — the only way out.
    function recycle(uint256 amount) external onlyOwner {
        sup.forceApprove(rewardPool, amount);
        IRewardPool(rewardPool).fund(amount);
        emit Recycled(amount);
    }

    function setMinDeposit(uint256 amount) external onlyOwner {
        minDeposit = amount;
        emit MinDepositUpdated(amount);
    }

    /// @notice Ownership can move (two steps) but never disappear.
    function renounceOwnership() public pure override {
        revert("renounce disabled");
    }

    function setGuardian(address guardian_) external onlyOwner {
        emit GuardianUpdated(guardian, guardian_);
        guardian = guardian_;
    }

    function pause() external {
        if (msg.sender != owner() && msg.sender != guardian) revert NotGuardian();
        _pause();
    }

    function unpause() external onlyOwner {
        _unpause();
    }
}
