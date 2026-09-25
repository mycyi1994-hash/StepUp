const { expect } = require("chai");
const { ethers } = require("hardhat");
const { time } = require("@nomicfoundation/hardhat-network-helpers");

// v2 — StepUpSneakers (app ⇄ wallet sneakers), SupVault (wallet → app SUP),
// and the RewardDistributor limits that bound a leaked attester key.

const eth = (n) => ethers.parseEther(String(n));
const op = (n) => ethers.zeroPadValue(ethers.toBeHex(n), 32);
const account = (uuid) => "0x" + uuid.replace(/-/g, "").padStart(64, "0");
const ALICE = account("f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1");

const RELEASE_TYPES = {
  Release: [
    { name: "opId", type: "bytes32" },
    { name: "to", type: "address" },
    { name: "tokenId", type: "uint256" },
    { name: "model", type: "uint32" },
    { name: "rarity", type: "uint8" },
    { name: "level", type: "uint16" },
    { name: "efficiencyBps", type: "uint16" },
    { name: "comfortBps", type: "uint16" },
    { name: "durability", type: "uint16" },
    { name: "genesisNo", type: "uint32" },
    { name: "locked", type: "bool" },
    { name: "deadline", type: "uint64" },
  ],
};

async function signRelease(signer, sneakers, r) {
  const { chainId } = await ethers.provider.getNetwork();
  return signer.signTypedData(
    { name: "StepUpSneakers", version: "2", chainId, verifyingContract: await sneakers.getAddress() },
    RELEASE_TYPES,
    r,
  );
}

async function deploy() {
  const [owner, treasury, signer, guardian, runner, buyer, relayer, other] = await ethers.getSigners();
  const sup = await (await ethers.getContractFactory("SUPToken")).deploy(treasury.address);
  const distributor = await (
    await ethers.getContractFactory("RewardDistributor")
  ).deploy(await sup.getAddress(), signer.address);
  const sneakers = await (
    await ethers.getContractFactory("StepUpSneakers")
  ).deploy(signer.address, guardian.address, treasury.address, 3, "ipfs://cid/");
  const vault = await (
    await ethers.getContractFactory("SupVault")
  ).deploy(await sup.getAddress(), await distributor.getAddress(), guardian.address);
  // FIRE (0) · EPIC (2) · variant 1 → model 21 ; WIND (3) · COMMON (0) · variant 0 → model 300
  await sneakers.addModels([21, 300], [2, 0]);
  return { owner, treasury, signer, guardian, runner, buyer, relayer, other, sup, distributor, sneakers, vault };
}

async function releaseArgs(ctx, over = {}) {
  const deadline = (await time.latest()) + 600;
  return {
    opId: op(1),
    to: ctx.runner.address,
    tokenId: 0,
    model: 21,
    rarity: 2,
    level: 1,
    efficiencyBps: 800,
    comfortBps: 700,
    durability: 10000,
    genesisNo: 0,
    locked: false,
    deadline,
    ...over,
  };
}

describe("StepUpSneakers — release (app → wallet)", () => {
  it("mints exactly what the server signed, to the runner, whoever submits", async () => {
    const ctx = await deploy();
    const r = await releaseArgs(ctx);
    const sig = await signRelease(ctx.signer, ctx.sneakers, r);
    await expect(ctx.sneakers.connect(ctx.relayer).release(r, sig))
      .to.emit(ctx.sneakers, "Released")
      .withArgs(r.opId, 1n, ctx.runner.address, true);
    expect(await ctx.sneakers.ownerOf(1)).to.equal(ctx.runner.address);
    const s = await ctx.sneakers.statsOf(1);
    expect(s.model).to.equal(21n);
    expect(s.efficiencyBps).to.equal(800n);
    expect(await ctx.sneakers.tokenURI(1)).to.equal("ipfs://cid/1");
  });

  it("uses each server operation once", async () => {
    const ctx = await deploy();
    const r = await releaseArgs(ctx);
    const sig = await signRelease(ctx.signer, ctx.sneakers, r);
    await ctx.sneakers.release(r, sig);
    await expect(ctx.sneakers.release(r, sig)).to.be.revertedWithCustomError(ctx.sneakers, "OpAlreadyUsed");
  });

  it("rejects anyone else's signature and any tampering", async () => {
    const ctx = await deploy();
    const r = await releaseArgs(ctx);
    const bad = await signRelease(ctx.other, ctx.sneakers, r);
    await expect(ctx.sneakers.release(r, bad)).to.be.revertedWithCustomError(ctx.sneakers, "BadSignature");
    const sig = await signRelease(ctx.signer, ctx.sneakers, r);
    await expect(ctx.sneakers.release({ ...r, level: 20 }, sig)).to.be.revertedWithCustomError(
      ctx.sneakers,
      "BadSignature",
    );
  });

  it("rejects an expired release", async () => {
    const ctx = await deploy();
    const r = await releaseArgs(ctx);
    const sig = await signRelease(ctx.signer, ctx.sneakers, r);
    await time.increase(601);
    await expect(ctx.sneakers.release(r, sig)).to.be.revertedWithCustomError(ctx.sneakers, "ReleaseExpired");
  });

  it("only mints catalog models at their catalog rarity, within stat bounds", async () => {
    const ctx = await deploy();
    for (const [over, err] of [
      [{ model: 999 }, "UnknownModel"],
      [{ rarity: 3 }, "RarityMismatch"],
      [{ level: 21 }, "BadStats"],
      [{ efficiencyBps: 5001 }, "BadStats"],
      [{ durability: 10001 }, "BadStats"],
    ]) {
      const r = await releaseArgs(ctx, over);
      const sig = await signRelease(ctx.signer, ctx.sneakers, r);
      await expect(ctx.sneakers.release(r, sig)).to.be.revertedWithCustomError(ctx.sneakers, err);
    }
  });

  it("caps new tokens per day, even with a valid signature", async () => {
    const ctx = await deploy(); // cap 3
    for (let i = 1; i <= 3; i++) {
      const r = await releaseArgs(ctx, { opId: op(i) });
      await ctx.sneakers.release(r, await signRelease(ctx.signer, ctx.sneakers, r));
    }
    const r = await releaseArgs(ctx, { opId: op(4) });
    await expect(
      ctx.sneakers.release(r, await signRelease(ctx.signer, ctx.sneakers, r)),
    ).to.be.revertedWithCustomError(ctx.sneakers, "DailyMintCapReached");
    await time.increase(24 * 60 * 60);
    const r2 = await releaseArgs(ctx, { opId: op(5) });
    await ctx.sneakers.release(r2, await signRelease(ctx.signer, ctx.sneakers, r2));
  });

  it("never mints the same Genesis number twice", async () => {
    const ctx = await deploy();
    const a = await releaseArgs(ctx, { opId: op(1), genesisNo: 1 });
    await ctx.sneakers.release(a, await signRelease(ctx.signer, ctx.sneakers, a));
    const b = await releaseArgs(ctx, { opId: op(2), genesisNo: 1 });
    await expect(
      ctx.sneakers.release(b, await signRelease(ctx.signer, ctx.sneakers, b)),
    ).to.be.revertedWithCustomError(ctx.sneakers, "GenesisTaken");
  });
});

describe("StepUpSneakers — deposit (wallet → app) and back", () => {
  async function minted(ctx, over = {}) {
    const r = await releaseArgs(ctx, over);
    await ctx.sneakers.release(r, await signRelease(ctx.signer, ctx.sneakers, r));
    return 1n;
  }

  it("moves the token into the vault and names the account to credit", async () => {
    const ctx = await deploy();
    const id = await minted(ctx);
    await expect(ctx.sneakers.connect(ctx.runner).deposit(id, ALICE))
      .to.emit(ctx.sneakers, "Deposited")
      .withArgs(id, ctx.runner.address, ALICE);
    expect(await ctx.sneakers.ownerOf(id)).to.equal(await ctx.sneakers.getAddress());
  });

  it("only the holder (or someone they approved) can deposit", async () => {
    const ctx = await deploy();
    const id = await minted(ctx);
    await expect(ctx.sneakers.connect(ctx.other).deposit(id, ALICE)).to.be.revertedWithCustomError(
      ctx.sneakers,
      "ERC721InsufficientApproval",
    );
  });

  it("hands the same token back out with the server's new level, keeping its identity", async () => {
    const ctx = await deploy();
    const id = await minted(ctx, { genesisNo: 7 });
    await ctx.sneakers.connect(ctx.runner).deposit(id, ALICE);
    const back = await releaseArgs(ctx, { opId: op(2), tokenId: id, level: 5, durability: 8120, genesisNo: 7 });
    await expect(ctx.sneakers.release(back, await signRelease(ctx.signer, ctx.sneakers, back)))
      .to.emit(ctx.sneakers, "Released")
      .withArgs(back.opId, id, ctx.runner.address, false);
    const s = await ctx.sneakers.statsOf(id);
    expect(s.level).to.equal(5n);
    expect(s.genesisNo).to.equal(7n);
    expect(await ctx.sneakers.nextTokenId()).to.equal(2n);
  });

  it("refuses to change identity or lower the level of an existing token", async () => {
    const ctx = await deploy();
    const id = await minted(ctx, { level: 3 });
    await ctx.sneakers.connect(ctx.runner).deposit(id, ALICE);
    for (const [over, err] of [
      [{ efficiencyBps: 1000 }, "IdentityChanged"],
      [{ genesisNo: 9 }, "IdentityChanged"],
      [{ level: 2 }, "LevelWentDown"],
    ]) {
      const r = await releaseArgs(ctx, { opId: op(9), tokenId: id, level: 3, ...over });
      await expect(
        ctx.sneakers.release(r, await signRelease(ctx.signer, ctx.sneakers, r)),
      ).to.be.revertedWithCustomError(ctx.sneakers, err);
    }
  });

  it("cannot hand out a token that is not in the vault", async () => {
    const ctx = await deploy();
    const id = await minted(ctx);
    const r = await releaseArgs(ctx, { opId: op(2), tokenId: id });
    await expect(
      ctx.sneakers.release(r, await signRelease(ctx.signer, ctx.sneakers, r)),
    ).to.be.revertedWithCustomError(ctx.sneakers, "NotInVault");
  });
});

describe("StepUpSneakers — transfer lock, pause, admin", () => {
  it("a locked sneaker cannot be sold or given away, but can go back to the app", async () => {
    const ctx = await deploy();
    const r = await releaseArgs(ctx, { locked: true });
    await ctx.sneakers.release(r, await signRelease(ctx.signer, ctx.sneakers, r));
    await expect(
      ctx.sneakers.connect(ctx.runner).transferFrom(ctx.runner.address, ctx.buyer.address, 1),
    ).to.be.revertedWithCustomError(ctx.sneakers, "TransferIsLocked");
    await ctx.sneakers.connect(ctx.runner).deposit(1, ALICE);

    // after 50 km in the app the server releases it unlocked
    const back = await releaseArgs(ctx, { opId: op(2), tokenId: 1, locked: false });
    await ctx.sneakers.release(back, await signRelease(ctx.signer, ctx.sneakers, back));
    await ctx.sneakers.connect(ctx.runner).transferFrom(ctx.runner.address, ctx.buyer.address, 1);
    expect(await ctx.sneakers.ownerOf(1)).to.equal(ctx.buyer.address);
  });

  it("guardian can pause, only the owner can unpause", async () => {
    const ctx = await deploy();
    await expect(ctx.sneakers.connect(ctx.other).pause()).to.be.revertedWithCustomError(ctx.sneakers, "NotGuardian");
    await ctx.sneakers.connect(ctx.guardian).pause();
    const r = await releaseArgs(ctx);
    await expect(
      ctx.sneakers.release(r, await signRelease(ctx.signer, ctx.sneakers, r)),
    ).to.be.revertedWithCustomError(ctx.sneakers, "EnforcedPause");
    await expect(ctx.sneakers.connect(ctx.guardian).unpause()).to.be.revertedWithCustomError(
      ctx.sneakers,
      "OwnableUnauthorizedAccount",
    );
    await ctx.sneakers.unpause();
  });

  it("catalog models can be added, never overwritten, only by the owner", async () => {
    const ctx = await deploy();
    await expect(ctx.sneakers.connect(ctx.other).addModels([22], [2])).to.be.revertedWithCustomError(
      ctx.sneakers,
      "OwnableUnauthorizedAccount",
    );
    await expect(ctx.sneakers.addModels([21], [3])).to.be.revertedWith("SNK: model exists");
    await ctx.sneakers.addModels([22], [2]);
    expect(await ctx.sneakers.modelCount()).to.equal(3n);
  });

  it("ownership moves in two steps — a typo cannot lose the contract", async () => {
    const ctx = await deploy();
    await ctx.sneakers.transferOwnership(ctx.other.address);
    expect(await ctx.sneakers.owner()).to.equal(ctx.owner.address);
    await ctx.sneakers.connect(ctx.other).acceptOwnership();
    expect(await ctx.sneakers.owner()).to.equal(ctx.other.address);
  });

  it("pays a 5% royalty to the treasury", async () => {
    const ctx = await deploy();
    const [to, amount] = await ctx.sneakers.royaltyInfo(1, eth(100));
    expect(to).to.equal(ctx.treasury.address);
    expect(amount).to.equal(eth(5));
  });
});

describe("SupVault — wallet → app", () => {
  it("takes SUP and names the account to credit", async () => {
    const ctx = await deploy();
    await ctx.sup.connect(ctx.treasury).transfer(ctx.runner.address, eth(100));
    await ctx.sup.connect(ctx.runner).approve(await ctx.vault.getAddress(), eth(40));
    await expect(ctx.vault.connect(ctx.runner).deposit(eth(40), ALICE))
      .to.emit(ctx.vault, "Deposited")
      .withArgs(ctx.runner.address, ALICE, eth(40));
    expect(await ctx.sup.balanceOf(await ctx.vault.getAddress())).to.equal(eth(40));
  });

  it("accepts a permit instead of a separate approve", async () => {
    const ctx = await deploy();
    await ctx.sup.connect(ctx.treasury).transfer(ctx.runner.address, eth(10));
    const deadline = (await time.latest()) + 600;
    const { chainId } = await ethers.provider.getNetwork();
    const sig = ethers.Signature.from(
      await ctx.runner.signTypedData(
        { name: await ctx.sup.name(), version: "1", chainId, verifyingContract: await ctx.sup.getAddress() },
        {
          Permit: [
            { name: "owner", type: "address" },
            { name: "spender", type: "address" },
            { name: "value", type: "uint256" },
            { name: "nonce", type: "uint256" },
            { name: "deadline", type: "uint256" },
          ],
        },
        {
          owner: ctx.runner.address,
          spender: await ctx.vault.getAddress(),
          value: eth(10),
          nonce: 0,
          deadline,
        },
      ),
    );
    await ctx.vault.connect(ctx.runner).depositWithPermit(eth(10), ALICE, deadline, sig.v, sig.r, sig.s);
    expect(await ctx.vault.totalDeposited()).to.equal(eth(10));
  });

  it("refuses dust and deposits without an account", async () => {
    const ctx = await deploy();
    await expect(ctx.vault.deposit(eth(0.5), ALICE)).to.be.revertedWithCustomError(ctx.vault, "BelowMinimum");
    await expect(ctx.vault.deposit(eth(5), ethers.ZeroHash)).to.be.revertedWithCustomError(ctx.vault, "ZeroAccount");
  });

  it("the only way out is back into the reward pool", async () => {
    const ctx = await deploy();
    const outs = ctx.vault.interface.fragments
      .filter((f) => f.type === "function")
      .map((f) => f.name)
      .filter((n) => /withdraw|sweep|rescue|recover|drain|transfer(?!Ownership)/i.test(n));
    expect(outs).to.deep.equal([]);

    await ctx.sup.connect(ctx.treasury).transfer(ctx.runner.address, eth(100));
    await ctx.sup.connect(ctx.runner).approve(await ctx.vault.getAddress(), eth(100));
    await ctx.vault.connect(ctx.runner).deposit(eth(100), ALICE);
    await expect(ctx.vault.connect(ctx.other).recycle(eth(100))).to.be.revertedWithCustomError(
      ctx.vault,
      "OwnableUnauthorizedAccount",
    );
    await ctx.vault.recycle(eth(100));
    expect(await ctx.distributor.poolBalance()).to.equal(eth(100));
  });
});

describe("RewardDistributor — limits for a leaked attester key", () => {
  async function claimFor(ctx, amount, hash = 1) {
    const c = {
      runner: ctx.runner.address,
      sessionHash: op(hash),
      amount,
      day: await ctx.distributor.currentDay(),
      deadline: (await time.latest()) + 600,
    };
    const { chainId } = await ethers.provider.getNetwork();
    const sig = await ctx.signer.signTypedData(
      { name: "StepUpRewards", version: "1", chainId, verifyingContract: await ctx.distributor.getAddress() },
      {
        Claim: [
          { name: "runner", type: "address" },
          { name: "sessionHash", type: "bytes32" },
          { name: "amount", type: "uint256" },
          { name: "day", type: "uint64" },
          { name: "deadline", type: "uint256" },
        ],
      },
      c,
    );
    return [c, sig];
  }

  it("enforces the owner's per-claim and per-day limits below the emission budget", async () => {
    const ctx = await deploy();
    await ctx.sup.connect(ctx.treasury).approve(await ctx.distributor.getAddress(), eth(1_000_000));
    await ctx.distributor.connect(ctx.treasury).fund(eth(1_000_000));
    await ctx.distributor.setLimits(eth(1500), eth(1000));

    const [big, bigSig] = await claimFor(ctx, eth(1001), 1);
    await expect(ctx.distributor.claim(big, bigSig)).to.be.revertedWithCustomError(ctx.distributor, "ClaimTooLarge");

    const [a, aSig] = await claimFor(ctx, eth(1000), 2);
    await ctx.distributor.claim(a, aSig);
    const [b, bSig] = await claimFor(ctx, eth(600), 3);
    await expect(ctx.distributor.claim(b, bSig)).to.be.revertedWithCustomError(
      ctx.distributor,
      "DailyBudgetExceeded",
    );
    expect(await ctx.distributor.dayRemaining(a.day)).to.equal(eth(500));
  });

  it("guardian can pause claims, not resume them", async () => {
    const ctx = await deploy();
    await ctx.distributor.setGuardian(ctx.guardian.address);
    await ctx.distributor.connect(ctx.guardian).pause();
    await expect(ctx.distributor.connect(ctx.guardian).unpause()).to.be.revertedWithCustomError(
      ctx.distributor,
      "OwnableUnauthorizedAccount",
    );
  });
});
