const { expect } = require("chai");
const { ethers } = require("hardhat");

const types = {
  DrawAuth: [
    { name: "to", type: "address" },
    { name: "category", type: "uint8" },
    { name: "faction", type: "uint8" },
    { name: "rarity", type: "uint8" },
    { name: "variant", type: "uint8" },
    { name: "outfitId", type: "uint8" },
    { name: "nonce", type: "uint256" },
    { name: "deadline", type: "uint256" },
  ],
};

async function setup() {
  const [owner, roller, runner, stranger] = await ethers.getSigners();
  const sup = await (await ethers.getContractFactory("SUPToken")).deploy(owner.address);
  const draw = await (await ethers.getContractFactory("MysteryDrawNFT")).deploy(
    await sup.getAddress(), roller.address, ethers.parseEther("500"), ethers.parseEther("300"), "ipfs://prizes/",
  );
  await sup.transfer(runner.address, ethers.parseEther("1000"));
  await sup.connect(runner).approve(await draw.getAddress(), ethers.parseEther("1000"));
  const { chainId } = await ethers.provider.getNetwork();
  const domain = { name: "StepUpMysteryDraw", version: "1", chainId, verifyingContract: await draw.getAddress() };
  const sign = auth => roller.signTypedData(domain, types, auth);
  return { sup, draw, roller, runner, stranger, sign };
}

describe("MysteryDrawNFT", function () {
  it("burns the configured shoe cost and records a catalogue-valid prize", async function () {
    const { sup, draw, runner, sign } = await setup();
    const auth = { to: runner.address, category: 0, faction: 3, rarity: 3, variant: 1,
      outfitId: 0, nonce: 0, deadline: 9999999999 };
    const sig = await sign(auth);
    await expect(draw.connect(runner).drawWithAuth(auth, sig)).to.emit(draw, "Drawn");
    expect(await draw.ownerOf(1)).to.equal(runner.address);
    expect((await draw.prizeOf(1)).category).to.equal(0);
    expect((await draw.prizeOf(1)).faction).to.equal(3);
    expect(await sup.balanceOf(runner.address)).to.equal(ethers.parseEther("500"));
    expect(await draw.nextNonce(runner.address)).to.equal(1);
    await expect(draw.connect(runner).drawWithAuth(auth, sig)).to.be.revertedWithCustomError(draw, "WrongNonce");
  });

  it("mints an outfit only to the paying recipient and rejects altered results", async function () {
    const { sup, draw, runner, stranger, sign } = await setup();
    const auth = { to: runner.address, category: 1, faction: 0, rarity: 0, variant: 0,
      outfitId: 5, nonce: 0, deadline: 9999999999 };
    const sig = await sign(auth);
    await expect(draw.connect(stranger).drawWithAuth(auth, sig)).to.be.revertedWithCustomError(draw, "WrongRecipient");
    await expect(draw.connect(runner).drawWithAuth({ ...auth, outfitId: 4 }, sig))
      .to.be.revertedWithCustomError(draw, "InvalidSignature");
    await draw.connect(runner).drawWithAuth(auth, sig);
    expect((await draw.prizeOf(1)).outfitId).to.equal(5);
    expect(await sup.balanceOf(runner.address)).to.equal(ethers.parseEther("700"));
  });
});
