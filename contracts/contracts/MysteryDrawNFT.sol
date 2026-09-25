// SPDX-License-Identifier: MIT
pragma solidity ^0.8.28;

import {ERC721} from "@openzeppelin/contracts/token/ERC721/ERC721.sol";
import {ERC721Enumerable} from "@openzeppelin/contracts/token/ERC721/extensions/ERC721Enumerable.sol";
import {Ownable} from "@openzeppelin/contracts/access/Ownable.sol";
import {EIP712} from "@openzeppelin/contracts/utils/cryptography/EIP712.sol";
import {ECDSA} from "@openzeppelin/contracts/utils/cryptography/ECDSA.sol";
import {ERC20Burnable} from "@openzeppelin/contracts/token/ERC20/extensions/ERC20Burnable.sol";

/**
 * A signed result is fixed for the next nonce before the user pays. The roller service
 * must derive the result deterministically from a secret seed, recipient, category and
 * nextNonce so repeated authorization requests cannot reroll for free. The contract
 * verifies the signature, burns SUP and mints in one transaction.
 *
 * The roller is trusted for draw fairness. An independent VRF can replace it later.
 * No contract is deployed by this source change.
 */
contract MysteryDrawNFT is ERC721, ERC721Enumerable, EIP712, Ownable {
    uint8 public constant SHOE = 0;
    uint8 public constant TRACKSUIT = 1;

    struct DrawAuth {
        address to;
        uint8 category;
        uint8 faction;
        uint8 rarity;
        uint8 variant;
        uint8 outfitId;
        uint256 nonce;
        uint256 deadline;
    }

    struct Prize {
        uint8 category;
        uint8 faction;
        uint8 rarity;
        uint8 variant;
        uint8 outfitId;
    }

    bytes32 private constant DRAW_AUTH_TYPEHASH = keccak256(
        "DrawAuth(address to,uint8 category,uint8 faction,uint8 rarity,uint8 variant,uint8 outfitId,uint256 nonce,uint256 deadline)"
    );

    ERC20Burnable public immutable sup;
    uint256 public immutable shoeCost;
    uint256 public immutable tracksuitCost;
    address public roller;
    mapping(address => uint256) public nextNonce;
    mapping(uint256 => Prize) private _prizes;
    uint256 private _nextTokenId = 1;
    string private _baseTokenURI;

    event Drawn(uint256 indexed tokenId, address indexed owner, uint8 indexed category,
        uint8 faction, uint8 rarity, uint8 variant, uint8 outfitId, uint256 costBurned);
    event RollerUpdated(address indexed previous, address indexed current);

    error InvalidCategory();
    error InvalidPrize();
    error Expired();
    error WrongRecipient();
    error WrongNonce();
    error InvalidSignature();

    constructor(
        address supToken,
        address roller_,
        uint256 shoeCost_,
        uint256 tracksuitCost_,
        string memory baseURI_
    ) ERC721("StepUp Mystery Prize", "SUMB") EIP712("StepUpMysteryDraw", "1") Ownable(msg.sender) {
        require(supToken != address(0) && roller_ != address(0), "zero address");
        require(shoeCost_ > 0 && tracksuitCost_ > 0, "zero cost");
        sup = ERC20Burnable(supToken);
        roller = roller_;
        shoeCost = shoeCost_;
        tracksuitCost = tracksuitCost_;
        _baseTokenURI = baseURI_;
    }

    function hashDrawAuth(DrawAuth calldata a) public view returns (bytes32) {
        return _hashTypedDataV4(keccak256(abi.encode(
            DRAW_AUTH_TYPEHASH, a.to, a.category, a.faction, a.rarity,
            a.variant, a.outfitId, a.nonce, a.deadline
        )));
    }

    function drawWithAuth(DrawAuth calldata a, bytes calldata signature) external returns (uint256 tokenId) {
        if (a.to != msg.sender) revert WrongRecipient();
        if (block.timestamp > a.deadline) revert Expired();
        if (a.nonce != nextNonce[msg.sender]) revert WrongNonce();
        if (a.category > TRACKSUIT) revert InvalidCategory();
        if (a.category == SHOE) {
            // 4 factions × (4 common + 4 rare + 3 epic + 2 legendary) = 52 catalogue designs.
            if (a.faction >= 4 || a.rarity >= 4 ||
                a.variant >= (a.rarity == 3 ? 2 : a.rarity == 2 ? 3 : 4) || a.outfitId != 0
            ) revert InvalidPrize();
        } else if (a.outfitId < 1 || a.outfitId > 5 ||
            a.faction != 0 || a.rarity != 0 || a.variant != 0
        ) revert InvalidPrize();
        if (ECDSA.recover(hashDrawAuth(a), signature) != roller) revert InvalidSignature();

        nextNonce[msg.sender] = a.nonce + 1;
        uint256 cost = a.category == SHOE ? shoeCost : tracksuitCost;
        sup.burnFrom(msg.sender, cost);

        tokenId = _nextTokenId++;
        _prizes[tokenId] = Prize(a.category, a.faction, a.rarity, a.variant, a.outfitId);
        _safeMint(msg.sender, tokenId);
        emit Drawn(tokenId, msg.sender, a.category, a.faction, a.rarity, a.variant, a.outfitId, cost);
    }

    function prizeOf(uint256 tokenId) external view returns (Prize memory) {
        _requireOwned(tokenId);
        return _prizes[tokenId];
    }

    function setRoller(address next) external onlyOwner {
        require(next != address(0), "zero roller");
        emit RollerUpdated(roller, next);
        roller = next;
    }

    function _baseURI() internal view override returns (string memory) {
        return _baseTokenURI;
    }

    function _update(address to, uint256 tokenId, address auth)
        internal override(ERC721, ERC721Enumerable) returns (address)
    {
        return super._update(to, tokenId, auth);
    }

    function _increaseBalance(address account, uint128 value)
        internal override(ERC721, ERC721Enumerable)
    {
        super._increaseBalance(account, value);
    }

    function supportsInterface(bytes4 interfaceId)
        public view override(ERC721, ERC721Enumerable) returns (bool)
    {
        return super.supportsInterface(interfaceId);
    }
}
