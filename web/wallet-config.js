// 지갑 페이지 공개 설정. 컨트랙트 v2 와 어테스터 워커를 배포한 뒤 주소를 채운다
// (contracts/deployments/giwaSepolia-v2.json). 비어 있으면 페이지는 "준비 중"만 보여준다.
// supabaseKey 는 앱에도 들어 있는 공개(publishable) 키다. 비밀 키는 절대 넣지 않는다.
window.STEPUP_WALLET_CONFIG = Object.freeze({
  chainId: 91342,
  rpcUrl: 'https://sepolia-rpc.giwa.io',
  explorer: 'https://sepolia-explorer.giwa.io',
  supabaseUrl: 'https://pupjzcmybuoyhzfwrsdf.supabase.co',
  supabaseKey: 'sb_publishable_jt74AKM32zdqnJlsFHEo2g_MNHa-WRO',
  attesterUrl: 'https://stepup-attester.gana003.workers.dev',
  sup: '0x55B4882797a365FEAa29F3267437b17F38Dbb1F6',
  sneakers: '0x3Da82CF9d749B0cCcA3DB1b9cA3C68B7c6080019',
  vault: '0x76fDAc77a9fb4Ec6c5Abb96941eFd8D93FBBbd5c',
  startBlock: 36979816,
  // v3 — 앱에서 뽑은 신발을 금고로 발행하는 컨트랙트(contracts/deployments/giwaSepolia-v3.json).
  // 금고의 신발을 지갑으로 꺼내면 이 컨트랙트의 번호(1000001~)로 온다.
  sneakersV3: '0xf57674209f73E4bA8444a5d75693004eE0e85177',
  v3StartBlock: 37205000,
})
