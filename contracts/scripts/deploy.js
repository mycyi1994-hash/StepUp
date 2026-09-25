/**
 * StepUp v2 컨트랙트 배포 — 키를 역할별로 나누고, 배포가 끝나면 배포 키는 버린다.
 *
 *   1. SUPToken          10억 SUP 전량을 금고(treasury)로 발행
 *   2. RewardDistributor 서버가 확정한 꺼내기(SUP)를 지급. 하루 상한 · 1회 상한
 *   3. StepUpSneakers    신발 NFT — 서버 서명으로 발행 · 반환, 앱으로 넣기
 *   4. SupVault          SUP 를 앱으로 넣는 금고 (나가는 길은 보상 풀뿐)
 *   5. CourseRegistry    코스 작성자 · 완주 기록
 *
 * 역할 (주소만 .env 에 — 개인키는 배포 키 하나뿐, 그것도 대표님 PC 에만)
 *   OWNER_ADDRESS            관리자. 배포 뒤 소유권을 넘겨받는다(acceptOwnership 필요)
 *   TREASURY_ADDRESS         10억 SUP 를 받고 보상 풀을 채운다
 *   ATTESTER_ADDRESS         SUP 꺼내기 서명 (Cloudflare 비밀)
 *   SNEAKER_SIGNER_ADDRESS   신발 발행 · 반환 서명 (Cloudflare 비밀)
 *   GUARDIAN_ADDRESS         긴급 정지만 할 수 있는 키 (Cloudflare 비밀)
 *   RECORDER_ADDRESS         코스 완주 기록 (Cloudflare 비밀)
 *
 * 먼저 드라이런(로컬 체인, 가스 0):
 *   npx hardhat run scripts/deploy.js --network hardhat
 * 실제 배포 — 요약을 확인한 뒤 CONFIRM_DEPLOY=yes 를 붙여야만 진행한다:
 *   CONFIRM_DEPLOY=yes npx hardhat run scripts/deploy.js --network giwaSepolia
 *
 * 결과는 deployments/<network>-v2.json 에 남는다. 옛 배포 기록(<network>.json)은 건드리지 않는다.
 */
const fs = require("fs");
const path = require("path");
const hre = require("hardhat");
const { allModels } = require("./lib/catalog");

const e = (n) => hre.ethers.parseEther(String(n));

async function main() {
  const [deployer] = await hre.ethers.getSigners();
  const net = hre.network.name;
  const live = net !== "hardhat" && net !== "localhost";
  const chainId = (await hre.ethers.provider.getNetwork()).chainId;

  // 드라이런에서는 빈 역할을 배포 키가 겸한다. 실제 배포에서는 전부 따로 있어야 한다.
  const role = (key) => {
    const v = process.env[key];
    if (v) return hre.ethers.getAddress(v);
    if (live) throw new Error(`${key} 가 비어 있습니다. .env 에 주소를 넣어 주세요.`);
    return deployer.address;
  };
  const owner = role("OWNER_ADDRESS");
  const treasury = role("TREASURY_ADDRESS");
  const attester = role("ATTESTER_ADDRESS");
  const sneakerSigner = role("SNEAKER_SIGNER_ADDRESS");
  const guardian = role("GUARDIAN_ADDRESS");
  const recorder = role("RECORDER_ADDRESS");
  const baseURI = process.env.SNEAKER_BASE_URI || "ipfs://REPLACE_WITH_CID/";

  const poolAmount = e(process.env.POOL_AMOUNT || "50000000");
  const dailyCap = e(process.env.DAILY_CAP || "50000");
  const maxClaim = e(process.env.MAX_CLAIM || "1000");
  // 서버 한도: 꺼내기 발행 500 + 보너스 발행 2,000
  const maxMintsPerDay = BigInt(process.env.MAX_MINTS_PER_DAY || "2500");

  if (live) {
    const roles = { owner, treasury, attester, sneakerSigner, guardian, recorder };
    for (const [name, addr] of Object.entries(roles)) {
      if (addr === deployer.address) {
        throw new Error(`${name} 가 배포 키와 같습니다. 배포 키는 배포 뒤 버리므로 역할을 맡길 수 없습니다.`);
      }
    }
    const signers = [attester, sneakerSigner, guardian, recorder];
    if (new Set(signers).size !== signers.length) {
      throw new Error("서명 키(ATTESTER · SNEAKER_SIGNER · GUARDIAN · RECORDER)는 서로 달라야 합니다.");
    }
    if (baseURI.includes("REPLACE_WITH_CID")) {
      throw new Error("SNEAKER_BASE_URI 에 IPFS 주소를 넣어 주세요 (끝에 / 포함).");
    }
  }

  const balance = await hre.ethers.provider.getBalance(deployer.address);
  const line = "─".repeat(72);
  console.log(line);
  console.log(`network        ${net} (chainId ${chainId})${live ? "" : "  — 드라이런"}`);
  console.log(`deployer       ${deployer.address}  (${hre.ethers.formatEther(balance)} ETH)`);
  console.log(`owner          ${owner}`);
  console.log(`treasury       ${treasury}`);
  console.log(`attester       ${attester}`);
  console.log(`sneakerSigner  ${sneakerSigner}`);
  console.log(`guardian       ${guardian}`);
  console.log(`recorder       ${recorder}`);
  console.log(`baseURI        ${baseURI}`);
  console.log(`limits         하루 ${hre.ethers.formatEther(dailyCap)} SUP · 1회 ${hre.ethers.formatEther(maxClaim)} SUP · 하루 발행 ${maxMintsPerDay}`);
  console.log(line);

  if (live && process.env.CONFIRM_DEPLOY !== "yes") {
    console.log("위 내용이 맞으면 CONFIRM_DEPLOY=yes 를 붙여 다시 실행하세요. 아무것도 배포하지 않았습니다.");
    return;
  }
  if (balance === 0n) {
    throw new Error("배포 계정에 가스가 없습니다. https://faucet.giwa.io 에서 테스트 ETH 를 받으세요.");
  }

  const deploy = async (name, args) => {
    const c = await (await hre.ethers.getContractFactory(name)).deploy(...args);
    await c.waitForDeployment();
    console.log(`✓ ${name.padEnd(18)} ${await c.getAddress()}`);
    return c;
  };
  const wait = async (tx) => (await tx).wait();

  const sup = await deploy("SUPToken", [treasury]);
  const supAddr = await sup.getAddress();
  const distributor = await deploy("RewardDistributor", [supAddr, attester]);
  const distributorAddr = await distributor.getAddress();
  const sneakers = await deploy("StepUpSneakers", [sneakerSigner, guardian, treasury, maxMintsPerDay, baseURI]);
  const vault = await deploy("SupVault", [supAddr, distributorAddr, guardian]);
  const registry = await deploy("CourseRegistry", [recorder]);

  // 설정 — 소유권을 넘기기 전에 배포 키로 해 둔다
  await wait(distributor.setGuardian(guardian));
  await wait(distributor.setLimits(dailyCap, maxClaim));
  const { ids, rarities } = allModels();
  await wait(sneakers.addModels(ids, rarities));
  console.log(`✓ 도감 ${ids.length}종 등록, 한도 설정`);

  // 보상 풀 — 드라이런(금고 = 배포 키)에서만 자동. 실제는 금고 지갑이 직접 넣는다.
  let funded = "0";
  if (treasury === deployer.address) {
    await wait(sup.approve(distributorAddr, poolAmount));
    await wait(distributor.fund(poolAmount));
    funded = hre.ethers.formatEther(poolAmount);
    console.log(`✓ 보상 풀 ${funded} SUP`);
  }

  // 소유권 — 2단계. 관리자 지갑이 acceptOwnership 을 눌러야 끝난다.
  if (owner !== deployer.address) {
    for (const c of [distributor, sneakers, vault, registry]) {
      await wait(c.transferOwnership(owner));
    }
    console.log(`✓ 소유권 이전 요청 → ${owner} (관리자 지갑에서 acceptOwnership 4번)`);
  }

  const record = {
    _note: "v2 — 역할 분리 배포. 소유권은 관리자 지갑이 acceptOwnership 해야 넘어간다.",
    network: net,
    chainId: Number(chainId),
    deployedAt: new Date().toISOString(),
    deployer: deployer.address,
    roles: { owner, treasury, attester, sneakerSigner, guardian, recorder },
    baseURI,
    limits: {
      dailyCap: hre.ethers.formatEther(dailyCap),
      maxClaim: hre.ethers.formatEther(maxClaim),
      maxMintsPerDay: Number(maxMintsPerDay),
    },
    rewardPoolFunded: funded,
    contracts: {
      SUPToken: { address: supAddr, args: [treasury] },
      RewardDistributor: { address: distributorAddr, args: [supAddr, attester] },
      StepUpSneakers: {
        address: await sneakers.getAddress(),
        args: [sneakerSigner, guardian, treasury, maxMintsPerDay.toString(), baseURI],
      },
      SupVault: { address: await vault.getAddress(), args: [supAddr, distributorAddr, guardian] },
      CourseRegistry: { address: await registry.getAddress(), args: [recorder] },
    },
  };

  if (live) {
    const outDir = path.join(__dirname, "..", "deployments");
    fs.mkdirSync(outDir, { recursive: true });
    const outFile = path.join(outDir, `${net}-v2.json`);
    if (fs.existsSync(outFile)) {
      throw new Error(`${outFile} 가 이미 있습니다. 덮어쓰지 않습니다 — 옮긴 뒤 다시 실행하세요.`);
    }
    fs.writeFileSync(outFile, `${JSON.stringify(record, null, 2)}\n`);
    console.log(line);
    console.log(`배포 기록 → ${path.relative(process.cwd(), outFile)}`);
  }

  console.log(line);
  console.log("다음 단계");
  if (treasury !== deployer.address) {
    console.log(`  1. 금고 지갑: SUPToken.approve(${distributorAddr}, 금액) → RewardDistributor.fund(금액)`);
  }
  if (owner !== deployer.address) {
    console.log("  2. 관리자 지갑: 4개 컨트랙트에서 acceptOwnership()");
  }
  console.log(`  3. 소스 검증: DEPLOYMENT=${net}-v2 npx hardhat run scripts/verify-blockscout.js --network ${net}`);
  console.log("  4. Cloudflare: wrangler secret put 으로 서명 키 등록 → wrangler deploy");
  console.log("  5. 배포 키는 PC 에서 지우고 .env 도 지운다");
  console.log(line);
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
