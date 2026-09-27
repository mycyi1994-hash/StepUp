/**
 * StepUpSneakersV3 배포 재료를 만든다 — attester/src/v3-deploy.js 와 deployments/<network>-v3.json.
 *
 *   npx hardhat compile && node scripts/v3-initcode.js [network]
 *
 * 컨트랙트 소스 · 설정 · 컴파일러가 바뀌면 주소가 바뀐다. 이미 배포한 뒤라면 바꾸지 않는다
 * (새 주소에 두 번째 컨트랙트가 생긴다). 바꿨다면 wrangler.toml 의 SNEAKERS_V3_ADDRESS 도 바꾼다.
 */
const fs = require("fs");
const path = require("path");
const { ROOT, loadConfig, v3Deployment, workerModule } = require("./lib/v3-deploy");

const network = process.argv[2] || "giwaSepolia";
const cfg = loadConfig(network);
const d = v3Deployment(cfg);

fs.writeFileSync(path.join(ROOT, "..", "attester", "src", "v3-deploy.js"), workerModule(cfg, d));
fs.writeFileSync(
  path.join(ROOT, "deployments", `${network}-v3.json`),
  JSON.stringify(
    {
      _note:
        "StepUpSneakersV3 — CREATE2 배포기로 올린다(누가 보내도 같은 주소). 어테스터 워커가 이 주소에 코드가 없으면 한 번 보낸다. 관리자는 생성자에서 정해져 acceptOwnership 이 필요 없다.",
      network,
      chainId: cfg.chainId,
      address: d.address,
      proxy: cfg.proxy,
      salt: d.salt,
      saltLabel: cfg.saltLabel,
      roles: {
        owner: cfg.config.owner,
        signer: cfg.config.signer,
        guardian: cfg.config.guardian,
        curator: cfg.config.curator,
        treasury: cfg.config.treasury,
      },
      limits: {
        maxMintsPerDay: cfg.config.maxMintsPerDay,
        maxReleasesPerDay: cfg.config.maxReleasesPerDay,
        maxSyncsPerDay: cfg.config.maxSyncsPerDay,
        maxGenesisNo: cfg.config.maxGenesisNo,
      },
      baseURI: cfg.config.baseURI,
      models: d.args.models.length,
    },
    null,
    2,
  ) + "\n",
);
console.log(`StepUpSneakersV3 @ ${d.address} (${network}) — initcode ${(d.initcode.length - 2) / 2} bytes`);
