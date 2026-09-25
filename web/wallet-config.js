// 지갑 페이지 공개 설정. 컨트랙트 v2 와 어테스터 워커를 배포한 뒤 주소를 채운다
// (contracts/deployments/giwaSepolia-v2.json). 비어 있으면 페이지는 "준비 중"만 보여준다.
// supabaseKey 는 앱에도 들어 있는 공개(publishable) 키다. 비밀 키는 절대 넣지 않는다.
window.STEPUP_WALLET_CONFIG = Object.freeze({
  chainId: 91342,
  rpcUrl: 'https://sepolia-rpc.giwa.io',
  explorer: 'https://sepolia-explorer.giwa.io',
  supabaseUrl: 'https://pupjzcmybuoyhzfwrsdf.supabase.co',
  supabaseKey: 'sb_publishable_jt74AKM32zdqnJlsFHEo2g_MNHa-WRO',
  attesterUrl: '',
  sup: '',
  sneakers: '',
  vault: '',
  startBlock: 0,
})
