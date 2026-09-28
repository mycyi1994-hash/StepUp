const { expect } = require("chai");
const { ethers, network } = require("hardhat");
const { time } = require("@nomicfoundation/hardhat-network-helpers");
const { loadConfig, v3Deployment } = require("../scripts/lib/v3-deploy");

// v3 — StepUpSneakersV3: sneakers drawn in the app are minted straight into the vault
// (vaultMint), upgrades and repairs follow on chain (syncStats), ERC-4906 on stat changes.
// Signing goes through attester/src/typed.js, like the worker does — if the worker and
// the contract drift apart, these tests break.

const op = (n) => ethers.zeroPadValue(ethers.toBeHex(n), 32);
const account = (uuid) => "0x" + uuid.replace(/-/g, "").padStart(64, "0");
const ALICE = account("f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1");
const RUNNER_HASH = ethers.id("runner:alice"); // the server's pseudonym for an account
const FIRST = 1_000_001n;

let typed;
before(async () => {
  typed = await import("../../attester/src/typed.js");
});

async function deploy(over = {}) {
  const [owner, treasury, signer, guardian, curator, runner, buyer, relayer, other] = await ethers.getSigners();
  const config = {
    owner: owner.address,
    signer: signer.address,
    guardian: guardian.address,
    curator: curator.address,
    treasury: treasury.address,
    maxMintsPerDay: 3,
    maxReleasesPerDay: 3,
    maxSyncsPerDay: 3,
    maxGenesisNo: 1000,
    baseURI: "https://meta.example/v3/",
    ...over,
  };
  // FIRE (0) · EPIC (2) · variant 1 → model 21 ; WIND (3) · COMMON (0) · variant 0 → model 300
  const sneakers = await (
    await ethers.getContractFactory("StepUpSneakersV3", relayer)
  ).deploy(config, [21, 300], [2, 0]);
  return { owner, treasury, signer, guardian, curator, runner, buyer, relayer, other, sneakers };
}

async function domain(sneakers) {
  const { chainId } = await ethers.provider.getNetwork();
  return typed.releaseDomain(chainId, await sneakers.getAddress(), "3");
}

/** server payload (attester_jobs_claim) for a vault mint */
async function mintPayload(over = {}) {
  return {
    kind: "VAULT_MINT",
    op_ref: op(1),
    account: RUNNER_HASH,
    faction: "FIRE",
    rarity: "EPIC",
    variant: 1,
    level: 1,
    efficiency_bps: 800,
    comfort_bps: 700,
    durability: "100.00",
    genesis_no: null,
    deadline_unix: (await time.latest()) + 600,
    ...over,
  };
}

async function vaultMint(ctx, over = {}, signer = ctx.signer) {
  const m = typed.vaultMintMessage(await mintPayload(over));
  const sig = await signer.signTypedData(await domain(ctx.sneakers), typed.VAULT_MINT_TYPES, m);
  return { m, sig, tx: () => ctx.sneakers.connect(ctx.relayer).vaultMint(m, sig) };
}

async function syncArgs(ctx, over = {}, signer = ctx.signer) {
  const s = typed.statsSyncMessage({
    kind: "STATS_SYNC",
    op_ref: op(100),
    token_id: FIRST.toString(),
    level: 2,
    durability: "100.00",
    deadline_unix: (await time.latest()) + 600,
    ...over,
  });
  const sig = await signer.signTypedData(await domain(ctx.sneakers), typed.STATS_SYNC_TYPES, s);
  return { s, sig, tx: () => ctx.sneakers.connect(ctx.relayer).syncStats(s, sig) };
}

async function releaseArgs(ctx, over = {}) {
  const r = typed.releaseMessage({
    kind: "SNEAKER_WITHDRAW",
    op_ref: op(200),
    wallet: ctx.runner.address,
    token_id: FIRST.toString(),
    faction: "FIRE",
    rarity: "EPIC",
    variant: 1,
    level: 1,
    efficiency_bps: 800,
    comfort_bps: 700,
    durability: "100.00",
    genesis_no: null,
    transfer_locked: false,
    deadline_unix: (await time.latest()) + 600,
    ...over,
  });
  const sig = await ctx.signer.signTypedData(await domain(ctx.sneakers), typed.RELEASE_TYPES, r);
  return { r, sig, tx: () => ctx.sneakers.connect(ctx.relayer).release(r, sig) };
}

describe("StepUpSneakersV3 — vault mint (a draw in the app)", () => {
  it("mints the server's sneaker into the vault for the account, ids after v2's", async () => {
    const ctx = await deploy();
    const { m, tx } = await vaultMint(ctx);
    await expect(tx()).to.emit(ctx.sneakers, "VaultMinted").withArgs(m.opId, FIRST, RUNNER_HASH);
    expect(await ctx.sneakers.ownerOf(FIRST)).to.equal(await ctx.sneakers.getAddress());
    const s = await ctx.sneakers.statsOf(FIRST);
    expect(s.model).to.equal(21n);
    expect(s.level).to.equal(1n);
    expect(s.efficiencyBps).to.equal(800n);
    expect(s.durability).to.equal(10000n);
    expect(await ctx.sneakers.tokenURI(FIRST)).to.equal(`https://meta.example/v3/${FIRST}`);
    expect(await ctx.sneakers.nextTokenId()).to.equal(FIRST + 1n);
  });

  it("uses each operation once and only the signer's signature", async () => {
    const ctx = await deploy();
    const first = await vaultMint(ctx);
    await first.tx();
    await expect(first.tx()).to.be.revertedWithCustomError(ctx.sneakers, "OpAlreadyUsed");
    const forged = await vaultMint(ctx, { op_ref: op(2) }, ctx.other);
    await expect(forged.tx()).to.be.revertedWithCustomError(ctx.sneakers, "BadSignature");
    const signed = await vaultMint(ctx, { op_ref: op(3) });
    await expect(
      ctx.sneakers.vaultMint({ ...signed.m, level: 20 }, signed.sig),
    ).to.be.revertedWithCustomError(ctx.sneakers, "BadSignature");
  });

  it("rejects an expired operation, an empty account and anything off the catalog", async () => {
    const ctx = await deploy();
    const late = await vaultMint(ctx);
    await time.increase(601);
    await expect(late.tx()).to.be.revertedWithCustomError(ctx.sneakers, "ReleaseExpired");
    for (const [over, err] of [
      [{ variant: 2 }, "UnknownModel"], // FIRE · EPIC · 2 = model 22 is not in this catalog
      [{ level: 21 }, "BadStats"],
      [{ efficiency_bps: 1001 }, "BadStats"],
      [{ comfort_bps: 901 }, "BadStats"],
      [{ genesis_no: 1001 }, "GenesisOutOfRange"],
    ]) {
      const { tx } = await vaultMint(ctx, { op_ref: op(9), ...over });
      await expect(tx()).to.be.revertedWithCustomError(ctx.sneakers, err);
    }
    const m = typed.vaultMintMessage(await mintPayload({ op_ref: op(10) }));
    const zero = { ...m, account: ethers.ZeroHash };
    const sig = await ctx.signer.signTypedData(await domain(ctx.sneakers), typed.VAULT_MINT_TYPES, zero);
    await expect(ctx.sneakers.vaultMint(zero, sig)).to.be.revertedWithCustomError(ctx.sneakers, "ZeroAccount");
  });

  it("shares the daily new-token cap with wallet mints, and keeps Genesis numbers unique", async () => {
    const ctx = await deploy(); // cap 3
    await (await vaultMint(ctx, { op_ref: op(1), genesis_no: 7 })).tx();
    await (await vaultMint(ctx, { op_ref: op(2) })).tx();
    const walletMint = await releaseArgs(ctx, { op_ref: op(3), token_id: null });
    await walletMint.tx();
    const over = await vaultMint(ctx, { op_ref: op(4) });
    await expect(over.tx()).to.be.revertedWithCustomError(ctx.sneakers, "DailyMintCapReached");
    await time.increase(24 * 60 * 60);
    const again = await vaultMint(ctx, { op_ref: op(5), genesis_no: 7 });
    await expect(again.tx()).to.be.revertedWithCustomError(ctx.sneakers, "GenesisTaken");
  });

  it("a vault token is not in anyone's wallet and cannot be pulled out without a release", async () => {
    const ctx = await deploy();
    await (await vaultMint(ctx)).tx();
    const vault = await ctx.sneakers.getAddress();
    await expect(
      ctx.sneakers.connect(ctx.relayer).transferFrom(vault, ctx.relayer.address, FIRST),
    ).to.be.revertedWithCustomError(ctx.sneakers, "ERC721InsufficientApproval");
    expect(await ctx.sneakers.balanceOf(vault)).to.equal(1n);
  });
});

describe("StepUpSneakersV3 — stat sync (upgrade · repair in the app)", () => {
  it("raises the level and restores durability of a vault token, and tells marketplaces", async () => {
    const ctx = await deploy();
    await (await vaultMint(ctx, { durability: "63.40" })).tx();
    const { s, tx } = await syncArgs(ctx, { level: 3, durability: "100.00" });
    await expect(tx())
      .to.emit(ctx.sneakers, "StatsSynced")
      .withArgs(s.opId, FIRST, 3, 10000)
      .and.to.emit(ctx.sneakers, "MetadataUpdate")
      .withArgs(FIRST);
    const st = await ctx.sneakers.statsOf(FIRST);
    expect(st.level).to.equal(3n);
    expect(st.durability).to.equal(10000n);
    expect(st.efficiencyBps).to.equal(800n); // identity untouched
  });

  it("never lowers a level, never goes past the rarity's max, and refuses an empty sync", async () => {
    const ctx = await deploy();
    await (await vaultMint(ctx, { level: 4 })).tx();
    for (const [over, err] of [
      [{ level: 3 }, "LevelWentDown"],
      [{ level: 21 }, "BadStats"],
      [{ level: 4, durability: "100.00" }, "NoChange"],
    ]) {
      const { tx } = await syncArgs(ctx, { op_ref: op(101), ...over });
      await expect(tx()).to.be.revertedWithCustomError(ctx.sneakers, err);
    }
    // wear alone (durability down) is a change
    await (await syncArgs(ctx, { op_ref: op(102), level: 4, durability: "80.00" })).tx();
  });

  it("only syncs tokens in the vault — a sneaker in a wallet keeps the stats it left with", async () => {
    const ctx = await deploy();
    await (await vaultMint(ctx)).tx();
    await (await releaseArgs(ctx)).tx();
    expect(await ctx.sneakers.ownerOf(FIRST)).to.equal(ctx.runner.address);
    const { tx } = await syncArgs(ctx, { level: 2 });
    await expect(tx()).to.be.revertedWithCustomError(ctx.sneakers, "NotInVault");
  });

  it("caps syncs per day and uses each operation once", async () => {
    const ctx = await deploy(); // sync cap 3
    await (await vaultMint(ctx)).tx();
    for (let i = 0; i < 3; i++) await (await syncArgs(ctx, { op_ref: op(110 + i), level: 2 + i })).tx();
    const over = await syncArgs(ctx, { op_ref: op(120), level: 6 });
    await expect(over.tx()).to.be.revertedWithCustomError(ctx.sneakers, "DailySyncCapReached");
    const reused = await syncArgs(ctx, { op_ref: op(110), level: 7 });
    await expect(reused.tx()).to.be.revertedWithCustomError(ctx.sneakers, "OpAlreadyUsed");
  });
});

describe("StepUpSneakersV3 — release and deposit keep v2's rules", () => {
  it("hands a vault-minted sneaker out with its synced stats, and takes it back", async () => {
    const ctx = await deploy();
    await (await vaultMint(ctx)).tx();
    await (await syncArgs(ctx, { level: 5, durability: "90.00" })).tx();
    const out = await releaseArgs(ctx, { level: 5, durability: "90.00" });
    await expect(out.tx())
      .to.emit(ctx.sneakers, "Released")
      .withArgs(out.r.opId, FIRST, ctx.runner.address, false)
      .and.to.emit(ctx.sneakers, "MetadataUpdate")
      .withArgs(FIRST);
    await expect(ctx.sneakers.connect(ctx.runner).deposit(FIRST, ALICE))
      .to.emit(ctx.sneakers, "Deposited")
      .withArgs(FIRST, ctx.runner.address, ALICE);
    // back in the vault — it can follow the app again
    await (await syncArgs(ctx, { op_ref: op(101), level: 6, durability: "90.00" })).tx();
  });

  it("a release cannot change a vault token's identity or lower its level", async () => {
    const ctx = await deploy();
    await (await vaultMint(ctx, { level: 3 })).tx();
    for (const [over, err] of [
      [{ efficiency_bps: 900, level: 3 }, "IdentityChanged"],
      [{ genesis_no: 9, level: 3 }, "IdentityChanged"],
      [{ level: 2 }, "LevelWentDown"],
    ]) {
      const { tx } = await releaseArgs(ctx, over);
      await expect(tx()).to.be.revertedWithCustomError(ctx.sneakers, err);
    }
  });

  it("a locked sneaker can only go back to the app", async () => {
    const ctx = await deploy();
    await (await vaultMint(ctx)).tx();
    await (await releaseArgs(ctx, { transfer_locked: true })).tx();
    await expect(
      ctx.sneakers.connect(ctx.runner).transferFrom(ctx.runner.address, ctx.buyer.address, FIRST),
    ).to.be.revertedWithCustomError(ctx.sneakers, "TransferIsLocked");
    await ctx.sneakers.connect(ctx.runner).deposit(FIRST, ALICE);
  });

  it("a plain transferFrom into the vault is refused — only deposit() credits an account", async () => {
    const ctx = await deploy();
    await (await releaseArgs(ctx, { token_id: null })).tx();
    await expect(
      ctx.sneakers
        .connect(ctx.runner)
        .transferFrom(ctx.runner.address, await ctx.sneakers.getAddress(), FIRST),
    ).to.be.revertedWithCustomError(ctx.sneakers, "UseDeposit");
  });
});

describe("StepUpSneakersV3 — roles, pause, interfaces", () => {
  it("the owner comes from the config — whoever sent the deployment has no rights", async () => {
    const ctx = await deploy();
    expect(await ctx.sneakers.owner()).to.equal(ctx.owner.address);
    await expect(ctx.sneakers.connect(ctx.relayer).setSigner(ctx.relayer.address)).to.be.revertedWithCustomError(
      ctx.sneakers,
      "OwnableUnauthorizedAccount",
    );
    await expect(ctx.sneakers.connect(ctx.relayer).renounceOwnership()).to.be.revertedWithCustomError(
      ctx.sneakers,
      "CannotRenounce",
    );
  });

  it("the curator can add catalog models — never overwrite them, nothing else", async () => {
    const ctx = await deploy();
    await ctx.sneakers.connect(ctx.curator).addModels([22], [2]);
    expect(await ctx.sneakers.modelCount()).to.equal(3n);
    await expect(ctx.sneakers.connect(ctx.curator).addModels([21], [3])).to.be.revertedWith("SNK: model exists");
    await expect(ctx.sneakers.connect(ctx.other).addModels([23], [2])).to.be.revertedWithCustomError(
      ctx.sneakers,
      "NotCurator",
    );
    await expect(ctx.sneakers.connect(ctx.curator).setMaxMintsPerDay(99)).to.be.revertedWithCustomError(
      ctx.sneakers,
      "OwnableUnauthorizedAccount",
    );
    await ctx.sneakers.connect(ctx.owner).setCurator(ctx.other.address);
    await ctx.sneakers.connect(ctx.other).addModels([23], [2]);
  });

  it("guardian pauses mints and syncs; only the owner resumes", async () => {
    const ctx = await deploy();
    await (await vaultMint(ctx)).tx();
    await ctx.sneakers.connect(ctx.guardian).pause();
    await expect((await vaultMint(ctx, { op_ref: op(2) })).tx()).to.be.revertedWithCustomError(
      ctx.sneakers,
      "EnforcedPause",
    );
    await expect((await syncArgs(ctx)).tx()).to.be.revertedWithCustomError(ctx.sneakers, "EnforcedPause");
    await expect(ctx.sneakers.connect(ctx.guardian).unpause()).to.be.revertedWithCustomError(
      ctx.sneakers,
      "OwnableUnauthorizedAccount",
    );
    await ctx.sneakers.connect(ctx.owner).unpause();
    await (await syncArgs(ctx)).tx();
  });

  it("guardian can cancel a signed operation before it is used", async () => {
    const ctx = await deploy();
    const { m, tx } = await vaultMint(ctx);
    await expect(ctx.sneakers.connect(ctx.guardian).cancelOp(m.opId)).to.emit(ctx.sneakers, "OpCancelled");
    await expect(tx()).to.be.revertedWithCustomError(ctx.sneakers, "OpAlreadyUsed");
  });

  it("announces ERC-721, ERC-2981, ERC-5192 and ERC-4906, and refreshes all metadata on a new base URI", async () => {
    const ctx = await deploy();
    for (const id of ["0x80ac58cd", "0x2a55205a", "0xb45a3c0e", "0x49064906", "0x01ffc9a7"]) {
      expect(await ctx.sneakers.supportsInterface(id), id).to.equal(true);
    }
    await (await vaultMint(ctx)).tx();
    await expect(ctx.sneakers.connect(ctx.owner).setBaseURI("https://meta.example/v3b/"))
      .to.emit(ctx.sneakers, "BatchMetadataUpdate")
      .withArgs(FIRST, FIRST);
  });
});

describe("StepUpSneakersV3 — new shoe catalog (0045, 70 models)", () => {
  it("the curator adds the whole catalog in one transaction, then a drawn new model mints and releases by its number", async () => {
    const ctx = await deploy();
    const { SHOE_MODELS } = await import("../../attester/src/shoe-catalog.js");
    const ids = Object.keys(SHOE_MODELS).map(Number);
    const rarities = ids.map((id) => ["COMMON", "RARE", "EPIC", "LEGENDARY"].indexOf(SHOE_MODELS[id].rarity));
    await ctx.sneakers.connect(ctx.curator).addModels(ids, rarities);
    expect(await ctx.sneakers.modelCount()).to.equal(2n + BigInt(ids.length));
    expect(await ctx.sneakers.modelExists(1330)).to.equal(true);

    // a free draw that rolled LEGENDARY · model 1317 (Redline) — the vault mint carries the catalog number
    const mint = await vaultMint(ctx, { op_ref: op(45), model_id: 1317, rarity: "LEGENDARY", efficiency_bps: 1100, comfort_bps: 1000 });
    await expect(mint.tx()).to.emit(ctx.sneakers, "VaultMinted");
    expect((await ctx.sneakers.statsOf(FIRST)).model).to.equal(1317n);

    // the model's rarity is fixed on chain — a payload with another rarity is refused before signing
    await expect(mintPayload({ model_id: 1317, rarity: "EPIC" }).then((p) => typed.vaultMintMessage(p))).to.be.rejectedWith(/등급/);

    // a gift (bonus) draw of a new model is minted straight to the wallet by release (token 0)
    const gift = await releaseArgs(ctx, {
      kind: "BONUS_MINT",
      op_ref: op(46),
      token_id: null,
      model_id: 1210,
      rarity: "EPIC",
      transfer_locked: true,
    });
    await expect(gift.tx()).to.emit(ctx.sneakers, "Released");
    expect(await ctx.sneakers.ownerOf(FIRST + 1n)).to.equal(ctx.runner.address);
    expect((await ctx.sneakers.statsOf(FIRST + 1n)).model).to.equal(1210n);
  });
});

describe("StepUpSneakersV3 — deterministic deployment", () => {
  // Nick's CREATE2 deployer — a preinstall on OP Stack chains (GIWA included)
  const PROXY = "0x4e59b44847b379578588920cA78FbF26c0B4956C";
  const PROXY_CODE =
    "0x7fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffe03601600081602082378035828234f58015156039578182fd5b8082525050506014600cf3";

  it("the committed worker module deploys the configured contract at the configured address", async () => {
    await network.provider.send("hardhat_setCode", [PROXY, PROXY_CODE]);
    const cfg = loadConfig("giwaSepolia");
    const d = v3Deployment(cfg);
    const committed = (await import("../../attester/src/v3-deploy.js")).V3_DEPLOYMENTS[cfg.chainId];
    expect(committed.address, "run: node scripts/v3-initcode.js").to.equal(d.address);
    expect(committed.initcode, "run: node scripts/v3-initcode.js").to.equal(d.initcode);
    expect(committed.salt).to.equal(d.salt);

    const [, , , , , , , relayer] = await ethers.getSigners();
    await relayer.sendTransaction({ to: PROXY, data: ethers.concat([committed.salt, committed.initcode]) });
    const deployed = await ethers.getContractAt("StepUpSneakersV3", committed.address);
    expect(await deployed.owner()).to.equal(cfg.config.owner);
    expect(await deployed.signer()).to.equal(cfg.config.signer);
    expect(await deployed.curator()).to.equal(cfg.config.curator);
    expect(await deployed.modelCount()).to.equal(52n);
    expect(await deployed.maxSyncsPerDay()).to.equal(BigInt(cfg.config.maxSyncsPerDay));
    // the same deployment again cannot land anywhere else
    await expect(
      relayer.sendTransaction({ to: PROXY, data: ethers.concat([committed.salt, committed.initcode]) }),
    ).to.be.reverted;
  });

  it("wrangler.toml points the worker at the same address", async () => {
    const fs = require("fs");
    const path = require("path");
    const toml = fs.readFileSync(path.join(__dirname, "..", "..", "attester", "wrangler.toml"), "utf8");
    const m = toml.match(/^SNEAKERS_V3_ADDRESS\s*=\s*"(0x[0-9a-fA-F]{40})"/m);
    expect(m, "SNEAKERS_V3_ADDRESS in attester/wrangler.toml").to.not.equal(null);
    expect(m[1]).to.equal(v3Deployment(loadConfig("giwaSepolia")).address);
  });
});
