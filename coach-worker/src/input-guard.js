/**
 * 공급자에게 보내기 직전의 마지막 문 — 입력(coaching_jobs.input)이 정해진 모양 그대로인지 본다.
 *
 * 서버(0059)가 좌표 · 이름 · 사용자 번호 · 날짜 · 시간대 없이 만든 입력이지만, 서버 함수가 나중에 바뀌어 무엇이 섞여도
 * 여기서 멈춘다. 칸 이름은 허용 목록, 값은 정수(초 · 미터 · 횟수)와 정해진 글자(종류 · 언어 · 목표 · id)뿐이다.
 * 하나라도 다르면 보내지 않는다(다시 해도 같으니 재시도하지 않는다).
 */

const KINDS = new Set(['run', 'weekly', 'recommendation'])
const LOCALES = new Set(['ko', 'en', 'ja', 'zh'])
const GOALS = new Set(['steady', 'finish_5k', 'improve'])
const ID = /^(run|week|pace|plan)\.[a-z_]+$|^template\.[a-z_0-9]+$/
const VALUE_KEY = /^[a-z_]+$/

const SHAPES = {
  run: { distance_m: 'int', duration_sec: 'int', avg_pace_sec: 'int', splits_sec: 'ints', partial: 'partial' },
  weekly: { runs: 'int', distance_m: 'int', duration_sec: 'int', avg_pace_sec: 'int?' },
  recommendation: { runs_so_far: 'int' },
}

const isInt = (v) => Number.isSafeInteger(v) && v >= 0

function checkObject(obj, shape, where) {
  if (obj === null || typeof obj !== 'object' || Array.isArray(obj)) return `${where}: 객체가 아니다`
  for (const [key, value] of Object.entries(obj)) {
    const type = shape[key]
    if (!type) return `${where}.${key}: 허용하지 않는 칸`
    if (type === 'int' && !isInt(value)) return `${where}.${key}: 정수가 아니다`
    if (type === 'int?' && value !== null && !isInt(value)) return `${where}.${key}: 정수가 아니다`
    if (type === 'ints' && !(Array.isArray(value) && value.every(isInt))) return `${where}.${key}: 정수 목록이 아니다`
    if (type === 'goal' && value !== null && !GOALS.has(value)) return `${where}.${key}: 모르는 목표`
    if (type === 'partial' && value !== null) {
      const problem = checkObject(value, { meters: 'int', seconds: 'int' }, `${where}.${key}`)
      if (problem) return problem
    }
  }
  return null
}

function checkItems(items, where) {
  if (!Array.isArray(items)) return `${where}: 목록이 아니다`
  for (const item of items) {
    if (item === null || typeof item !== 'object' || Object.keys(item).some((k) => k !== 'id' && k !== 'values')) {
      return `${where}: 허용하지 않는 칸`
    }
    if (typeof item.id !== 'string' || !ID.test(item.id)) return `${where}: 모르는 id 모양`
    if (item.values === null || typeof item.values !== 'object' || Array.isArray(item.values)) return `${where}: 값이 객체가 아니다`
    for (const [key, value] of Object.entries(item.values)) {
      if (!VALUE_KEY.test(key) || !Number.isSafeInteger(value)) return `${where}.${item.id}.${key}: 정수가 아니다`
    }
  }
  return null
}

/** 문제가 없으면 null, 있으면 까닭 한 줄 */
export function inputProblem(input) {
  if (input === null || typeof input !== 'object' || Array.isArray(input)) return '입력이 객체가 아니다'
  const allowed = new Set(['kind', 'locale', 'schema_version', 'run', 'week', 'goal', 'facts', 'candidates'])
  for (const key of Object.keys(input)) if (!allowed.has(key)) return `${key}: 허용하지 않는 칸`
  if (!KINDS.has(input.kind)) return '모르는 종류'
  if (!LOCALES.has(input.locale)) return '모르는 언어'
  if (typeof input.schema_version !== 'string' || !/^coach-out-v\d+$/.test(input.schema_version)) return '모르는 형식 판'
  const body = input.kind === 'run' ? 'run' : 'week'
  if (input.kind === 'run' && 'week' in input) return 'week: 기록 하나의 입력에 주 칸'
  if (input.kind !== 'run' && 'run' in input) return 'run: 주 입력에 기록 칸'
  const problem = checkObject(input[body], SHAPES[input.kind], body)
  if (problem) return problem
  if ('goal' in input) {
    const goalProblem = checkObject(input.goal, { goal: 'goal', runs_per_week: 'int?', minutes: 'int?' }, 'goal')
    if (goalProblem) return goalProblem
  }
  return checkItems(input.facts, 'facts') ?? checkItems(input.candidates, 'candidates')
}
