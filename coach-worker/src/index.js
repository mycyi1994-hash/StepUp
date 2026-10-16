import { makeRpc } from './supabase.js'
import { runOnce } from './worker.js'

/**
 * Cloudflare Worker — 정해진 시각마다 한 바퀴. 아직 배포하지 않았다(배포 워크플로 없음).
 * 배포 전에 정할 것: 공급자 · 모델 · 예산 · 한도 · 동의 문구(docs/redesign/ai-coaching-2026-10/README.md "남은 결정").
 * 지금 서버 설정(coaching_provider)이 none 이면 빌려 줄 작업이 없어 아무것도 하지 않는다.
 */
export default {
  async scheduled(_event, env, ctx) {
    ctx.waitUntil(
      runOnce({ rpc: makeRpc(env) }).then(
        (outcomes) => console.log('coach worker', outcomes.length, outcomes.map((o) => o.outcome).join(',')),
        (error) => console.error('coach worker failed', String(error?.message ?? error)),
      ),
    )
  },
}
