// SPDX-License-Identifier: MIT
pragma solidity ^0.8.28;

import {ERC721} from "@openzeppelin/contracts/token/ERC721/ERC721.sol";
import {ERC721Royalty} from "@openzeppelin/contracts/token/ERC721/extensions/ERC721Royalty.sol";
import {Ownable} from "@openzeppelin/contracts/access/Ownable.sol";
import {Ownable2Step} from "@openzeppelin/contracts/access/Ownable2Step.sol";
import {Pausable} from "@openzeppelin/contracts/utils/Pausable.sol";
import {EIP712} from "@openzeppelin/contracts/utils/cryptography/EIP712.sol";
import {ECDSA} from "@openzeppelin/contracts/utils/cryptography/ECDSA.sol";

/**
 * @title StepUpSneakers
 * @notice StepUp sneakers on the GIWA chain — the wallet side of the in-app shoe
 *         locker. Replaces SneakerNFT and MysteryDrawNFT for the v2 deployment.
 *
 * ## Who decides what
 *
 * The server (Supabase) is the source of truth for a sneaker while it is in the
 * app: draws, upgrades and wear all happen there, where the phone cannot forge
 * them. This contract only ever mints or updates a sneaker from a **release**
 * signed by `signer` (the attester Worker), and every release carries a server
 * operation id that can succeed exactly once.
 *
 *   release  app → wallet   first time: mint a new token with the server's stats
 *                           again:      hand the token back out of the vault,
 *                                       with the stats the server has now
 *   deposit  wallet → app   the token moves into this contract (the vault) and
 *                           the event tells the server which account to credit
 *
 * A token in the vault still exists and keeps its id, its Genesis number and its
 * history, so a sneaker bought on a marketplace can be deposited by its new owner.
 *
 * ## Limits that hold even if the signing key leaks
 *
 *   - identity fields (model, rarity, base stats, Genesis number) of an existing
 *     token can never change, and a level can never go down
 *   - at most `maxMintsPerDay` new tokens and `maxReleasesPerDay` vault
 *     hand-backs per day — a leaked signer cannot empty the vault in one go
 *   - base stats must sit inside the rarity's range; Genesis numbers are capped
 *   - `guardian` (a hot key) can pause or cancel a signed operation; only
 *     `owner` can unpause or rotate keys, and ownership cannot be renounced
 *
 * ## Transfer lock
 *
 * Sneakers from free draws are locked until the runner has run 50 km with them
 * (server-side). A locked token can only move into the vault — that is how it
 * goes back to the app to be run in — and the lock is lifted by the next release.
 */
contract StepUpSneakers is ERC721Royalty, EIP712, Ownable2Step, Pausable {
    /// Royalty on secondary sales, paid to the treasury (5%).
    uint96 public constant ROYALTY_BPS = 500;

    /// Stat bounds — the same ranges the server uses (supabase/migrations/0022).
    uint16 public constant MAX_EFFICIENCY_BPS = 5000;
    uint16 public constant MAX_COMFORT_BPS = 2000;
    /// Durability is stored in hundredths: 10000 = 100.00.
    uint16 public constant MAX_DURABILITY = 10000;

    uint8 public constant RARITY_COUNT = 4; // 0 Common .. 3 Legendary

    struct Stats {
        uint32 model;
        uint8 rarity;
        uint16 level;
        uint16 efficiencyBps; // at level 1; the level adds on top off-chain
        uint16 comfortBps; // at level 1
        uint16 durability; // hundredths
        uint32 genesisNo; // 0 = not Genesis
    }

    struct Release {
        bytes32 opId;
        address to;
        uint256 tokenId; // 0 = mint a new token
        uint32 model;
        uint8 rarity;
        uint16 level;
        uint16 efficiencyBps;
        uint16 comfortBps;
        uint16 durability;
        uint32 genesisNo;
        bool locked;
        uint64 deadline;
    }

    bytes32 private constant _RELEASE_TYPEHASH = keccak256(
        "Release(bytes32 opId,address to,uint256 tokenId,uint32 model,uint8 rarity,uint16 level,"
        "uint16 efficiencyBps,uint16 comfortBps,uint16 durability,uint32 genesisNo,bool locked,uint64 deadline)"
    );

    address public signer;
    address public guardian;

    uint256 public nextTokenId = 1;
    uint256 public maxMintsPerDay;
    mapping(uint64 day => uint256 count) public mintedOn;
    uint256 public maxReleasesPerDay;
    mapping(uint64 day => uint256 count) public releasedOn;
    /// Highest Genesis number that may be minted. 0 = no Genesis at all.
    uint32 public maxGenesisNo;

    /// Catalog. A model can be added but never changed or removed.
    mapping(uint32 model => bool) public modelExists;
    mapping(uint32 model => uint8) public modelRarity;
    uint256 public modelCount;

    mapping(uint256 tokenId => Stats) private _stats;
    mapping(uint256 tokenId => bool) public transferLocked;
    mapping(bytes32 opId => bool) public opUsed;
    mapping(uint32 genesisNo => bool) public genesisTaken;

    string private _base;

    event ModelAdded(uint32 indexed model, uint8 rarity);
    event Released(bytes32 indexed opId, uint256 indexed tokenId, address indexed to, bool minted);
    event Deposited(uint256 indexed tokenId, address indexed from, bytes32 indexed account);
    event SignerUpdated(address indexed previous, address indexed current);
    event GuardianUpdated(address indexed previous, address indexed current);
    event MintCapUpdated(uint256 perDay);
    event ReleaseCapUpdated(uint256 perDay);
    event GenesisCapUpdated(uint32 maxGenesisNo);
    event OpCancelled(bytes32 indexed opId);
    /// ERC-5192 — lets marketplaces show that a token cannot be traded yet.
    event Locked(uint256 tokenId);
    event Unlocked(uint256 tokenId);
    event BaseURIUpdated(string baseURI);

    error ReleaseExpired(uint64 deadline);
    error OpAlreadyUsed(bytes32 opId);
    error BadSignature();
    error UnknownModel(uint32 model);
    error RarityMismatch(uint32 model, uint8 rarity);
    error BadStats();
    error IdentityChanged(uint256 tokenId);
    error LevelWentDown(uint256 tokenId, uint16 was, uint16 now_);
    error NotInVault(uint256 tokenId);
    error GenesisTaken(uint32 genesisNo);
    error DailyMintCapReached(uint64 day);
    error DailyReleaseCapReached(uint64 day);
    error GenesisOutOfRange(uint32 genesisNo);
    error UseDeposit();
    error LockedDepositByOperator(uint256 tokenId);
    error CannotRenounce();
    error TransferIsLocked(uint256 tokenId);
    error ZeroAccount();
    error NotGuardian();

    constructor(
        address signer_,
        address guardian_,
        address treasury,
        uint256 maxMintsPerDay_,
        uint256 maxReleasesPerDay_,
        uint32 maxGenesisNo_,
        string memory baseURI_
    )
        ERC721("StepUp Sneakers", "SUPSNK")
        EIP712("StepUpSneakers", "2")
        Ownable(msg.sender)
    {
        require(signer_ != address(0), "SNK: signer is zero");
        require(treasury != address(0), "SNK: treasury is zero");
        signer = signer_;
        guardian = guardian_;
        maxMintsPerDay = maxMintsPerDay_;
        maxReleasesPerDay = maxReleasesPerDay_;
        maxGenesisNo = maxGenesisNo_;
        _base = baseURI_;
        _setDefaultRoyalty(treasury, ROYALTY_BPS);
    }

    // ── Views ────────────────────────────────────────────────────────────

    function statsOf(uint256 tokenId) external view returns (Stats memory) {
        _requireOwned(tokenId);
        return _stats[tokenId];
    }

    /// ERC-5192
    function locked(uint256 tokenId) external view returns (bool) {
        _requireOwned(tokenId);
        return transferLocked[tokenId];
    }

    /// Base-stat ceilings per rarity — the top of the server's draw ranges (0022).
    function maxEfficiencyBps(uint8 rarity) public pure returns (uint16) {
        if (rarity == 0) return 400;
        if (rarity == 1) return 700;
        if (rarity == 2) return 1000;
        if (rarity == 3) return 1300;
        return 0;
    }

    function maxComfortBps(uint8 rarity) public pure returns (uint16) {
        if (rarity == 0) return 300;
        if (rarity == 1) return 600;
        if (rarity == 2) return 900;
        if (rarity == 3) return 1200;
        return 0;
    }

    function maxLevel(uint8 rarity) public pure returns (uint16) {
        if (rarity == 0) return 10;
        if (rarity == 1) return 15;
        if (rarity == 2) return 20;
        if (rarity == 3) return 30;
        return 0;
    }

    function today() public view returns (uint64) {
        return uint64(block.timestamp / 1 days);
    }

    function hashRelease(Release calldata r) public view returns (bytes32) {
        return _hashTypedDataV4(
            keccak256(
                abi.encode(
                    _RELEASE_TYPEHASH,
                    r.opId,
                    r.to,
                    r.tokenId,
                    r.model,
                    r.rarity,
                    r.level,
                    r.efficiencyBps,
                    r.comfortBps,
                    r.durability,
                    r.genesisNo,
                    r.locked,
                    r.deadline
                )
            )
        );
    }

    // ── App → wallet ─────────────────────────────────────────────────────

    /**
     * @notice Mint or hand back a sneaker exactly as the server signed it. Anyone
     *         may submit (a relayer pays the gas); the sneaker always goes to `r.to`.
     */
    function release(Release calldata r, bytes calldata signature) external whenNotPaused {
        if (block.timestamp > r.deadline) revert ReleaseExpired(r.deadline);
        if (opUsed[r.opId]) revert OpAlreadyUsed(r.opId);
        if (ECDSA.recover(hashRelease(r), signature) != signer) revert BadSignature();
        if (!modelExists[r.model]) revert UnknownModel(r.model);
        if (modelRarity[r.model] != r.rarity) revert RarityMismatch(r.model, r.rarity);
        if (
            r.to == address(0) || r.to == address(this) || r.level == 0 || r.level > maxLevel(r.rarity)
                || r.efficiencyBps > maxEfficiencyBps(r.rarity) || r.comfortBps > maxComfortBps(r.rarity)
                || r.durability > MAX_DURABILITY
        ) revert BadStats();

        opUsed[r.opId] = true;

        uint256 tokenId = r.tokenId;
        bool minted = tokenId == 0;
        if (minted) {
            uint64 d = today();
            if (mintedOn[d] >= maxMintsPerDay) revert DailyMintCapReached(d);
            mintedOn[d] += 1;
            if (r.genesisNo != 0) {
                if (r.genesisNo > maxGenesisNo) revert GenesisOutOfRange(r.genesisNo);
                if (genesisTaken[r.genesisNo]) revert GenesisTaken(r.genesisNo);
                genesisTaken[r.genesisNo] = true;
            }
            tokenId = nextTokenId++;
        } else {
            if (_ownerOf(tokenId) != address(this)) revert NotInVault(tokenId);
            uint64 d = today();
            if (releasedOn[d] >= maxReleasesPerDay) revert DailyReleaseCapReached(d);
            releasedOn[d] += 1;
            Stats storage s = _stats[tokenId];
            if (
                s.model != r.model || s.rarity != r.rarity || s.efficiencyBps != r.efficiencyBps
                    || s.comfortBps != r.comfortBps || s.genesisNo != r.genesisNo
            ) revert IdentityChanged(tokenId);
            if (r.level < s.level) revert LevelWentDown(tokenId, s.level, r.level);
        }

        _stats[tokenId] = Stats({
            model: r.model,
            rarity: r.rarity,
            level: r.level,
            efficiencyBps: r.efficiencyBps,
            comfortBps: r.comfortBps,
            durability: r.durability,
            genesisNo: r.genesisNo
        });
        transferLocked[tokenId] = r.locked;
        if (r.locked) emit Locked(tokenId);
        else emit Unlocked(tokenId);

        if (minted) {
            _mint(r.to, tokenId);
        } else {
            _transfer(address(this), r.to, tokenId);
        }
        emit Released(r.opId, tokenId, r.to, minted);
    }

    // ── Wallet → app ─────────────────────────────────────────────────────

    /**
     * @notice Put a sneaker back into the app. `account` is the StepUp account
     *         (server user id as bytes32) that receives it. Works for locked tokens.
     */
    function deposit(uint256 tokenId, bytes32 account) external whenNotPaused {
        if (account == bytes32(0)) revert ZeroAccount();
        address holder = ownerOf(tokenId);
        // A locked (free) sneaker may only be put back by its holder — an approved
        // operator must not be able to move it into some other StepUp account.
        if (transferLocked[tokenId] && msg.sender != holder) revert LockedDepositByOperator(tokenId);
        _checkAuthorized(holder, msg.sender, tokenId);
        _transfer(holder, address(this), tokenId);
        emit Deposited(tokenId, holder, account);
    }

    // ── Transfer lock ────────────────────────────────────────────────────

    function _update(address to, uint256 tokenId, address auth) internal override returns (address) {
        address from = _ownerOf(tokenId);
        // Into the vault only through deposit() (auth == 0): a plain transferFrom
        // would park the token with no Deposited event and no account to credit.
        if (to == address(this) && auth != address(0)) revert UseDeposit();
        if (transferLocked[tokenId] && from != address(0) && from != address(this) && to != address(this)) {
            revert TransferIsLocked(tokenId);
        }
        return super._update(to, tokenId, auth);
    }

    /// ERC-5192 (0xb45a3c0e) on top of ERC-721 and ERC-2981.
    function supportsInterface(bytes4 interfaceId) public view override returns (bool) {
        return interfaceId == 0xb45a3c0e || super.supportsInterface(interfaceId);
    }

    function _baseURI() internal view override returns (string memory) {
        return _base;
    }

    // ── Admin ────────────────────────────────────────────────────────────

    /// @notice Add catalog models. Existing models cannot be changed.
    function addModels(uint32[] calldata models, uint8[] calldata rarities) external onlyOwner {
        require(models.length == rarities.length, "SNK: length mismatch");
        for (uint256 i = 0; i < models.length; i++) {
            require(rarities[i] < RARITY_COUNT, "SNK: bad rarity");
            require(!modelExists[models[i]], "SNK: model exists");
            modelExists[models[i]] = true;
            modelRarity[models[i]] = rarities[i];
            modelCount += 1;
            emit ModelAdded(models[i], rarities[i]);
        }
    }

    function setSigner(address signer_) external onlyOwner {
        require(signer_ != address(0), "SNK: signer is zero");
        emit SignerUpdated(signer, signer_);
        signer = signer_;
    }

    function setGuardian(address guardian_) external onlyOwner {
        emit GuardianUpdated(guardian, guardian_);
        guardian = guardian_;
    }

    function setMaxMintsPerDay(uint256 perDay) external onlyOwner {
        maxMintsPerDay = perDay;
        emit MintCapUpdated(perDay);
    }

    function setMaxReleasesPerDay(uint256 perDay) external onlyOwner {
        maxReleasesPerDay = perDay;
        emit ReleaseCapUpdated(perDay);
    }

    function setMaxGenesisNo(uint32 maxGenesisNo_) external onlyOwner {
        maxGenesisNo = maxGenesisNo_;
        emit GenesisCapUpdated(maxGenesisNo_);
    }

    function setDefaultRoyalty(address receiver) external onlyOwner {
        _setDefaultRoyalty(receiver, ROYALTY_BPS);
    }

    /// @notice Kill a signed operation before it is submitted (e.g. the server expired it).
    function cancelOp(bytes32 opId) external {
        if (msg.sender != owner() && msg.sender != guardian) revert NotGuardian();
        opUsed[opId] = true;
        emit OpCancelled(opId);
    }

    /// @notice Ownership can move (two steps) but never disappear — without an
    ///         owner nobody could unpause after a guardian pause.
    function renounceOwnership() public pure override {
        revert CannotRenounce();
    }

    function setBaseURI(string calldata baseURI_) external onlyOwner {
        _base = baseURI_;
        emit BaseURIUpdated(baseURI_);
    }

    /// @notice Owner or guardian can stop releases and deposits at once.
    function pause() external {
        if (msg.sender != owner() && msg.sender != guardian) revert NotGuardian();
        _pause();
    }

    /// @notice Only the owner can resume — a leaked hot key cannot undo a pause.
    function unpause() external onlyOwner {
        _unpause();
    }
}
