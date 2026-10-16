/**
 * 공급자 — 입력 하나를 받아 결과 형식(coach-out-v1) 하나를 돌려준다.
 *
 *   mock       시험용. 입력의 근거 · 후보 중 하나를 규칙으로 고르고, 미리 검토한 고정 문장을 쓴다. 바깥으로 아무것도 보내지 않는다.
 *              앱은 이 결과를 "모의 응답"으로 표시한다(provider = mock).
 *   anthropic  아직 부르지 않는다. 모델 · 계약 · 예산 · 동의 문구가 정해진 뒤(지시서 14절)에 구조화 출력(JSON Schema,
 *              ../schema/coach-out-v1.json)으로 붙인다. 그 전에는 보내지 않고 실패로 끝낸다(다시 하지 않는다).
 */

export class ProviderError extends Error {
  /**
   * @param {string} code 서버에 남길 까닭(http_429 · http_5xx · timeout · refusal · truncated · bad_json · provider_not_approved)
   * @param {boolean} retryable 같은 작업을 다시 할 만한가
   * @param {object|null} usage 이미 과금됐을 수 있는 사용량(모르면 null)
   */
  constructor(code, retryable, usage = null) {
    super(code)
    this.code = code
    this.retryable = retryable
    this.usage = usage
  }
}

// 미리 검토한 고정 문장 — 숫자 · 진단 · 보상 · 남과의 비교 없이, 언어마다(서버 coaching.text_problem 을 통과해야 한다)
const LINES = {
  ko: {
    'run.split_last_vs_first+': '후반으로 갈수록 페이스가 느려졌어요.',
    'run.split_last_vs_first-': '후반에 페이스를 끌어올렸어요.',
    'run.split_even': '처음부터 끝까지 고르게 달렸어요.',
    'run.avg_pace': '오늘 러닝을 잘 마쳤어요.',
    'week.streak': '꾸준히 이어가고 있어요.',
    'week.runs': '한 주를 잘 마쳤어요.',
    'week.distance': '한 주를 잘 마쳤어요.',
    'pace.start_easier': '처음을 조금 천천히 시작하면 끝까지 편해요.',
    'pace.keep': '지금처럼 편하게 이어가요.',
    'plan.next_week': '다음 주도 가볍게 이어가요.',
    template: '걷기로 몸을 풀고 시작해요.',
  },
  en: {
    'run.split_last_vs_first+': 'Your pace eased off later in the run.',
    'run.split_last_vs_first-': 'You picked up the pace toward the end.',
    'run.split_even': 'You kept an even pace throughout.',
    'run.avg_pace': 'Nice work finishing this run.',
    'week.streak': 'You are keeping your routine going.',
    'week.runs': 'You wrapped up the week well.',
    'week.distance': 'You wrapped up the week well.',
    'pace.start_easier': 'Starting a little easier can keep the finish comfortable.',
    'pace.keep': 'Keep this comfortable rhythm going.',
    'plan.next_week': 'Keep next week light and steady.',
    template: 'Warm up with a walk before you run.',
  },
  ja: {
    'run.split_last_vs_first+': '後半にかけてペースが落ちました。',
    'run.split_last_vs_first-': '後半にペースを上げられました。',
    'run.split_even': '最初から最後まで安定したペースでした。',
    'run.avg_pace': '今日のランをしっかり終えました。',
    'week.streak': 'コツコツ続けています。',
    'week.runs': '一週間をしっかり終えました。',
    'week.distance': '一週間をしっかり終えました。',
    'pace.start_easier': '最初を少しゆっくり始めると最後まで楽です。',
    'pace.keep': 'このまま楽なペースで続けましょう。',
    'plan.next_week': '来週も軽く続けましょう。',
    template: 'ウォーキングで体をほぐしてから始めましょう。',
  },
  zh: {
    'run.split_last_vs_first+': '后半程配速慢了下来。',
    'run.split_last_vs_first-': '后半程配速提了上来。',
    'run.split_even': '从头到尾配速都很均匀。',
    'run.avg_pace': '今天的跑步顺利完成了。',
    'week.streak': '你在坚持不懈地跑。',
    'week.runs': '这一周顺利完成了。',
    'week.distance': '这一周顺利完成了。',
    'pace.start_easier': '开头放慢一点，后面会更轻松。',
    'pace.keep': '保持现在轻松的节奏。',
    'plan.next_week': '下周也轻松地继续吧。',
    template: '先走路热身再开始跑。',
  },
}

/** 고를 근거 — 더 많이 말해 주는 것부터(구간 비교 · 고름 · 이어진 주), 없으면 첫 근거 */
function pickFact(facts) {
  const order = ['run.split_last_vs_first', 'run.split_even', 'week.streak', 'week.runs', 'run.avg_pace', 'week.distance']
  for (const id of order) {
    const fact = facts.find((f) => f.id === id)
    if (fact) return fact
  }
  return facts[0] ?? null
}

export const mockProvider = {
  name: 'mock',
  async generate(job) {
    const input = job.input
    const lines = LINES[input.locale]
    if (!lines) throw new ProviderError('bad_locale', false)
    const fact = pickFact(input.facts)
    const candidate = input.candidates[0] ?? null
    const factLine = fact && (fact.id === 'run.split_last_vs_first'
      ? lines[`${fact.id}${fact.values.delta_sec > 0 ? '+' : '-'}`]
      : lines[fact.id])
    const reason = candidate && (candidate.id.startsWith('template.') ? lines.template : lines[candidate.id])
    return {
      output: {
        schema_version: input.schema_version,
        kind: input.kind,
        observation_fact_id: fact?.id ?? null,
        explanation: factLine ?? null,
        next_action_candidate_id: candidate?.id ?? null,
        action_reason: reason ?? null,
        limitation_code: null,
      },
      usage: { provider: 'mock', model: null, input_tokens: 0, output_tokens: 0, cost_micro: 0 },
    }
  },
}

/** 아직 승인되지 않은 실제 공급자 — 보내지 않는다 */
export const notApprovedProvider = (name) => ({
  name,
  async generate() {
    throw new ProviderError('provider_not_approved', false)
  },
})

export function providerFor(name) {
  if (name === 'mock') return mockProvider
  return notApprovedProvider(name)
}
