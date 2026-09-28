// SPDX-License-Identifier: MIT
pragma solidity ^0.8.28;

import {ERC721} from "@openzeppelin/contracts/token/ERC721/ERC721.sol";
import {ERC721Royalty} from "@openzeppelin/contracts/token/ERC721/extensions/ERC721Royalty.sol";
import {IERC4906} from "@openzeppelin/contracts/interfaces/IERC4906.sol";
import {IERC165} from "@openzeppelin/contracts/utils/introspection/IERC165.sol";
import {Ownable} from "@openzeppelin/contracts/access/Ownable.sol";
import {Ownable2Step} from "@openzeppelin/contracts/access/Ownable2Step.sol";
import {Pausable} from "@openzeppelin/contracts/utils/Pausable.sol";
import {EIP712} from "@openzeppelin/contracts/utils/cryptography/EIP712.sol";
import {ECDSA} from "@openzeppelin/contracts/utils/cryptography/ECDSA.sol";

/**
 * @title StepUpSneakersV3
 * @notice StepUp sneakers on the GIWA chain, v3 — every sneaker drawn in the app
 *         exists on chain from the moment it is drawn, while it stays in the app.
 *
 * v2 minted a sneaker only when its owner took it out to a wallet. v3 adds:
 *
 *   vaultMint  the server's draw → a new token minted straight into this contract
 *              (the vault), credited to a StepUp account. The runner keeps running
 *              in it in the app; the token is the public record that it exists.
 *   syncStats  an upgrade or repair in the app → the vault token's level and
 *              durability follow. Levels only go up, and a sync must change
 *              something (no empty transactions).
 *   ERC-4906   MetadataUpdate on every stat change, so marketplaces re-read the
 *              token instead of showing stale stats.
 *
 * Everything else is v2: release (vault → wallet, or a fresh mint to a wallet),
 * deposit (wallet → vault), the transfer lock for free sneakers, daily caps, the
 * guardian pause and operation cancel, and a catalog that only grows.
 *
 * ## Who decides what
 *
 * The server (Supabase) is the source of truth while a sneaker is in the app. This
 * contract only mints, releases or syncs a sneaker from an operation signed by
 * `signer` (the attester Worker); every operation id succeeds at most once. Anyone
 * may submit a signed operation — a relayer pays the gas.
 *
 * ## Limits that hold even if the signing key leaks
 *
 *   - identity fields (model, rarity, base stats, Genesis number) never change,
 *     and a level never goes down
 *   - at most `maxMintsPerDay` new tokens, `maxReleasesPerDay` vault hand-backs
 *     and `maxSyncsPerDay` stat syncs per day
 *   - a vault mint lands in the vault, not in anyone's wallet — taking it out is a
 *     release, capped like every other hand-back
 *   - base stats must sit inside the rarity's range; Genesis numbers are capped
 *   - `guardian` (a hot key) can pause or cancel an operation; only `owner` can
 *     unpause or rotate keys, and ownership cannot be renounced
 *
 * ## Token ids
 *
 * Ids start at FIRST_TOKEN_ID so they never meet v2's ids — the server keeps one
 * token id per sneaker across both contracts.
 */
contract StepUpSneakersV3 is ERC721Royalty, IERC4906, EIP712, Ownable2Step, Pausable {
    /// Royalty on secondary sales, paid to the treasury (5%).
    uint96 public constant ROYALTY_BPS = 500;

    /// Stat bounds — the same ranges the server uses (supabase/migrations/0022).
    uint16 public constant MAX_EFFICIENCY_BPS = 5000;
    uint16 public constant MAX_COMFORT_BPS = 2000;
    /// Durability is stored in hundredths: 10000 = 100.00.
    uint16 public constant MAX_DURABILITY = 10000;

    uint8 public constant RARITY_COUNT = 4; // 0 Common .. 3 Legendary

    /// v2 ids are far below this (v2 mints at most a few thousand a day).
    uint256 public constant FIRST_TOKEN_ID = 1_000_001;

    struct Stats {
        uint32 model;
        uint8 rarity;
        uint16 level;
        uint16 efficiencyBps; // at level 1; the level adds on top off-chain
        uint16 comfortBps; // at level 1
        uint16 durability; // hundredths
        uint32 genesisNo; // 0 = not Genesis
    }

    /// Vault → wallet (tokenId != 0), or a fresh mint to a wallet (tokenId == 0). Same as v2.
    struct Release {
        bytes32 opId;
        address to;
        uint256 tokenId;
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

    /// A sneaker drawn in the app, minted into the vault for `account`.
    struct VaultMint {
        bytes32 opId;
        bytes32 account;
        uint32 model;
        uint8 rarity;
        uint16 level;
        uint16 efficiencyBps;
        uint16 comfortBps;
        uint16 durability;
        uint32 genesisNo;
        uint64 deadline;
    }

    /// An upgrade or repair in the app, applied to the token in the vault.
    struct StatsSync {
        bytes32 opId;
        uint256 tokenId;
        uint16 level;
        uint16 durability;
        uint64 deadline;
    }

    /// Deployment settings. A struct so the deployment can be one deterministic call.
    struct Config {
        address owner;
        address signer;
        address guardian;
        address curator;
        address treasury;
        uint256 maxMintsPerDay;
        uint256 maxReleasesPerDay;
        uint256 maxSyncsPerDay;
        uint32 maxGenesisNo;
        string baseURI;
    }

    bytes32 private constant _RELEASE_TYPEHASH = keccak256(
        "Release(bytes32 opId,address to,uint256 tokenId,uint32 model,uint8 rarity,uint16 level,"
        "uint16 efficiencyBps,uint16 comfortBps,uint16 durability,uint32 genesisNo,bool locked,uint64 deadline)"
    );

    bytes32 private constant _VAULT_MINT_TYPEHASH = keccak256(
        "VaultMint(bytes32 opId,bytes32 account,uint32 model,uint8 rarity,uint16 level,"
        "uint16 efficiencyBps,uint16 comfortBps,uint16 durability,uint32 genesisNo,uint64 deadline)"
    );

    bytes32 private constant _STATS_SYNC_TYPEHASH =
        keccak256("StatsSync(bytes32 opId,uint256 tokenId,uint16 level,uint16 durability,uint64 deadline)");

    address public signer;
    address public guardian;
    /// May add catalog models (and nothing else) — new shoe designs without an owner transaction.
    address public curator;

    uint256 public nextTokenId = FIRST_TOKEN_ID;
    uint256 public maxMintsPerDay;
    mapping(uint64 day => uint256 count) public mintedOn;
    uint256 public maxReleasesPerDay;
    mapping(uint64 day => uint256 count) public releasedOn;
    uint256 public maxSyncsPerDay;
    mapping(uint64 day => uint256 count) public syncedOn;
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
    event VaultMinted(bytes32 indexed opId, uint256 indexed tokenId, bytes32 indexed account);
    event StatsSynced(bytes32 indexed opId, uint256 indexed tokenId, uint16 level, uint16 durability);
    event Deposited(uint256 indexed tokenId, address indexed from, bytes32 indexed account);
    event SignerUpdated(address indexed previous, address indexed current);
    event GuardianUpdated(address indexed previous, address indexed current);
    event CuratorUpdated(address indexed previous, address indexed current);
    event MintCapUpdated(uint256 perDay);
    event ReleaseCapUpdated(uint256 perDay);
    event SyncCapUpdated(uint256 perDay);
    event GenesisCapUpdated(uint32 maxGenesisNo);
    event OpCancelled(bytes32 indexed opId);
    /// ERC-5192
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
    error NoChange(uint256 tokenId);
    error GenesisTaken(uint32 genesisNo);
    error DailyMintCapReached(uint64 day);
    error DailyReleaseCapReached(uint64 day);
    error DailySyncCapReached(uint64 day);
    error GenesisOutOfRange(uint32 genesisNo);
    error UseDeposit();
    error LockedDepositByOperator(uint256 tokenId);
    error CannotRenounce();
    error TransferIsLocked(uint256 tokenId);
    error ZeroAccount();
    error NotGuardian();
    error NotCurator();

    constructor(Config memory c, uint32[] memory models, uint8[] memory rarities)
        ERC721("StepUp Sneakers", "SUPSNK")
        EIP712("StepUpSneakers", "3")
        Ownable(c.owner)
    {
        require(c.signer != address(0), "SNK: signer is zero");
        require(c.treasury != address(0), "SNK: treasury is zero");
        signer = c.signer;
        guardian = c.guardian;
        curator = c.curator;
        maxMintsPerDay = c.maxMintsPerDay;
        maxReleasesPerDay = c.maxReleasesPerDay;
        maxSyncsPerDay = c.maxSyncsPerDay;
        maxGenesisNo = c.maxGenesisNo;
        _base = c.baseURI;
        _setDefaultRoyalty(c.treasury, ROYALTY_BPS);
        _addModels(models, rarities);
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

    function hashVaultMint(VaultMint calldata m) public view returns (bytes32) {
        return _hashTypedDataV4(
            keccak256(
                abi.encode(
                    _VAULT_MINT_TYPEHASH,
                    m.opId,
                    m.account,
                    m.model,
                    m.rarity,
                    m.level,
                    m.efficiencyBps,
                    m.comfortBps,
                    m.durability,
                    m.genesisNo,
                    m.deadline
                )
            )
        );
    }

    function hashStatsSync(StatsSync calldata s) public view returns (bytes32) {
        return _hashTypedDataV4(
            keccak256(abi.encode(_STATS_SYNC_TYPEHASH, s.opId, s.tokenId, s.level, s.durability, s.deadline))
        );
    }

    // ── App → chain ──────────────────────────────────────────────────────

    /**
     * @notice Mint a sneaker drawn in the app straight into the vault, credited to
     *         `m.account`. It stays usable in the app; a later release hands it out.
     */
    function vaultMint(VaultMint calldata m, bytes calldata signature) external whenNotPaused {
        _useOp(m.opId, m.deadline, hashVaultMint(m), signature);
        if (m.account == bytes32(0)) revert ZeroAccount();
        _checkNew(m.model, m.rarity, m.level, m.efficiencyBps, m.comfortBps, m.durability);

        uint256 tokenId = _nextMint(m.genesisNo);
        _stats[tokenId] = Stats({
            model: m.model,
            rarity: m.rarity,
            level: m.level,
            efficiencyBps: m.efficiencyBps,
            comfortBps: m.comfortBps,
            durability: m.durability,
            genesisNo: m.genesisNo
        });
        _mint(address(this), tokenId);
        emit VaultMinted(m.opId, tokenId, m.account);
    }

    /**
     * @notice Apply an upgrade or repair to a sneaker in the vault. The level can
     *         only go up, and something must change.
     */
    function syncStats(StatsSync calldata s, bytes calldata signature) external whenNotPaused {
        _useOp(s.opId, s.deadline, hashStatsSync(s), signature);
        if (_ownerOf(s.tokenId) != address(this)) revert NotInVault(s.tokenId);
        Stats storage st = _stats[s.tokenId];
        if (s.level > maxLevel(st.rarity) || s.durability > MAX_DURABILITY) revert BadStats();
        if (s.level < st.level) revert LevelWentDown(s.tokenId, st.level, s.level);
        if (s.level == st.level && s.durability == st.durability) revert NoChange(s.tokenId);

        uint64 d = today();
        if (syncedOn[d] >= maxSyncsPerDay) revert DailySyncCapReached(d);
        syncedOn[d] += 1;

        st.level = s.level;
        st.durability = s.durability;
        emit StatsSynced(s.opId, s.tokenId, s.level, s.durability);
        emit MetadataUpdate(s.tokenId);
    }

    /**
     * @notice Mint or hand back a sneaker exactly as the server signed it. Anyone
     *         may submit (a relayer pays the gas); the sneaker always goes to `r.to`.
     */
    function release(Release calldata r, bytes calldata signature) external whenNotPaused {
        _useOp(r.opId, r.deadline, hashRelease(r), signature);
        if (r.to == address(0) || r.to == address(this)) revert BadStats();
        _checkNew(r.model, r.rarity, r.level, r.efficiencyBps, r.comfortBps, r.durability);

        uint256 tokenId = r.tokenId;
        bool minted = tokenId == 0;
        if (minted) {
            tokenId = _nextMint(r.genesisNo);
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
            emit MetadataUpdate(tokenId);
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

    // ── Internals ────────────────────────────────────────────────────────

    /// Deadline, one use per operation id, and the signer's signature over `digest`.
    function _useOp(bytes32 opId, uint64 deadline, bytes32 digest, bytes calldata signature) private {
        if (block.timestamp > deadline) revert ReleaseExpired(deadline);
        if (opUsed[opId]) revert OpAlreadyUsed(opId);
        if (ECDSA.recover(digest, signature) != signer) revert BadSignature();
        opUsed[opId] = true;
    }

    /// A catalog model at its catalog rarity, with stats inside the rarity's range.
    function _checkNew(uint32 model, uint8 rarity, uint16 level, uint16 efficiencyBps, uint16 comfortBps, uint16 durability)
        private
        view
    {
        if (!modelExists[model]) revert UnknownModel(model);
        if (modelRarity[model] != rarity) revert RarityMismatch(model, rarity);
        if (
            level == 0 || level > maxLevel(rarity) || efficiencyBps > maxEfficiencyBps(rarity)
                || comfortBps > maxComfortBps(rarity) || durability > MAX_DURABILITY
        ) revert BadStats();
    }

    /// The next new token id, within the daily mint cap and the Genesis rules.
    function _nextMint(uint32 genesisNo) private returns (uint256 tokenId) {
        uint64 d = today();
        if (mintedOn[d] >= maxMintsPerDay) revert DailyMintCapReached(d);
        mintedOn[d] += 1;
        if (genesisNo != 0) {
            if (genesisNo > maxGenesisNo) revert GenesisOutOfRange(genesisNo);
            if (genesisTaken[genesisNo]) revert GenesisTaken(genesisNo);
            genesisTaken[genesisNo] = true;
        }
        tokenId = nextTokenId++;
    }

    function _addModels(uint32[] memory models, uint8[] memory rarities) private {
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

    // ── Transfer lock ────────────────────────────────────────────────────

    function _update(address to, uint256 tokenId, address auth) internal override returns (address) {
        address from = _ownerOf(tokenId);
        // Into the vault only through deposit() or a vault mint (auth == 0): a plain
        // transferFrom would park the token with no Deposited event and no account to credit.
        if (to == address(this) && auth != address(0)) revert UseDeposit();
        if (transferLocked[tokenId] && from != address(0) && from != address(this) && to != address(this)) {
            revert TransferIsLocked(tokenId);
        }
        return super._update(to, tokenId, auth);
    }

    /// ERC-5192 (0xb45a3c0e) and ERC-4906 (0x49064906) on top of ERC-721 and ERC-2981.
    function supportsInterface(bytes4 interfaceId) public view override(ERC721Royalty, IERC165) returns (bool) {
        return interfaceId == 0xb45a3c0e || interfaceId == 0x49064906 || super.supportsInterface(interfaceId);
    }

    function _baseURI() internal view override returns (string memory) {
        return _base;
    }

    // ── Admin ────────────────────────────────────────────────────────────

    /// @notice Add catalog models. Existing models cannot be changed. Owner or curator.
    function addModels(uint32[] calldata models, uint8[] calldata rarities) external {
        if (msg.sender != owner() && msg.sender != curator) revert NotCurator();
        _addModels(models, rarities);
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

    function setCurator(address curator_) external onlyOwner {
        emit CuratorUpdated(curator, curator_);
        curator = curator_;
    }

    function setMaxMintsPerDay(uint256 perDay) external onlyOwner {
        maxMintsPerDay = perDay;
        emit MintCapUpdated(perDay);
    }

    function setMaxReleasesPerDay(uint256 perDay) external onlyOwner {
        maxReleasesPerDay = perDay;
        emit ReleaseCapUpdated(perDay);
    }

    function setMaxSyncsPerDay(uint256 perDay) external onlyOwner {
        maxSyncsPerDay = perDay;
        emit SyncCapUpdated(perDay);
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
        if (nextTokenId > FIRST_TOKEN_ID) emit BatchMetadataUpdate(FIRST_TOKEN_ID, nextTokenId - 1);
    }

    /// @notice Owner or guardian can stop every operation and deposit at once.
    function pause() external {
        if (msg.sender != owner() && msg.sender != guardian) revert NotGuardian();
        _pause();
    }

    /// @notice Only the owner can resume — a leaked hot key cannot undo a pause.
    function unpause() external onlyOwner {
        _unpause();
    }
}
