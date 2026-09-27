/**
 * StepUpSneakersV3 결정적 배포 재료 — 설정 파일 + 컴파일 결과 → initcode · 주소.
 *
 * 배포는 체인에 이미 있는 CREATE2 배포기(0x4e59…956C, OP 스택 기본 탑재)로 한다. 주소는
 * 배포기 · salt · initcode(바이트코드 + 생성자 인자)만으로 정해진다 — 누가 언제 보내도
 * 같은 주소에 같은 컨트랙트가 생긴다. 관리자(owner)는 생성자에서 정하므로 배포를 보낸
 * 키(어테스터 워커의 릴레이어)는 아무 권한도 갖지 않는다.
 *
 * scripts/v3-initcode.js 가 이것으로 attester/src/v3-deploy.js 를 만들고,
 * test/V3Deploy.test.js 가 그 파일이 지금 소스와 같은지 확인한다.
 */
const fs = require("fs");
const path = require("path");
const { ethers } = require("ethers");
const { allModels } = require("./catalog");

const ROOT = path.resolve(__dirname, "..", "..");

function loadConfig(network = "giwaSepolia") {
  return JSON.parse(fs.readFileSync(path.join(ROOT, "deployments", `${network}-v3.config.json`), "utf8"));
}

function loadArtifact() {
  const file = path.join(ROOT, "artifacts", "contracts", "StepUpSneakersV3.sol", "StepUpSneakersV3.json");
  return JSON.parse(fs.readFileSync(file, "utf8"));
}

/** 설정 → { address, salt, initcode, args } */
function v3Deployment(cfg, artifact = loadArtifact()) {
  const { ids, rarities } = allModels();
  const c = cfg.config;
  const config = [
    c.owner,
    c.signer,
    c.guardian,
    c.curator,
    c.treasury,
    BigInt(c.maxMintsPerDay),
    BigInt(c.maxReleasesPerDay),
    BigInt(c.maxSyncsPerDay),
    Number(c.maxGenesisNo),
    c.baseURI,
  ];
  const factory = new ethers.ContractFactory(artifact.abi, artifact.bytecode);
  const initcode = factory.interface.encodeDeploy([config, ids, rarities]);
  const full = ethers.concat([artifact.bytecode, initcode]);
  const salt = ethers.id(cfg.saltLabel);
  const address = ethers.getCreate2Address(cfg.proxy, salt, ethers.keccak256(full));
  return { address, salt, initcode: full, args: { config: c, models: ids, rarities } };
}

/** attester/src/v3-deploy.js 의 내용 */
function workerModule(cfg, d) {
  return `// 자동 생성 — contracts/scripts/v3-initcode.js. 손으로 고치지 않는다.
// StepUpSneakersV3 를 CREATE2 배포기로 올리는 재료. 설정: contracts/deployments/${cfg.network}-v3.config.json
// 주소는 이 initcode 로만 정해진다 — 워커는 이 주소에 코드가 없을 때만 한 번 보낸다(src/v3.js).
export const V3_DEPLOYMENTS = {
  ${cfg.chainId}: {
    address: '${d.address}',
    proxy: '${cfg.proxy}',
    salt: '${d.salt}',
    initcode:
      '${d.initcode}',
  },
}
`;
}

module.exports = { ROOT, loadConfig, loadArtifact, v3Deployment, workerModule };
