package com.stepup.android.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.provider.Settings
import com.stepup.android.data.prefs.UserPrefs
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * TYPE_STEP_COUNTER(부팅 이후 누적 걸음 수) 센서를 감싸
 * "오늘 걸음 수"를 [todaySteps] StateFlow로 노출한다.
 *
 * - 날짜가 바뀌면 현재 누적값을 새 기준점으로 저장해 0부터 다시 센다.
 * - 재부팅으로 누적값이 초기화되면 기준점을 0으로 되돌린다.
 * - 센서가 없는 기기/에뮬레이터를 위해 [simulateSteps]를 제공한다(디버그용).
 */
class StepTracker(
    context: Context,
    private val prefs: UserPrefs,
    /** 오늘 이미 저장된 걸음 수 조회 (재부팅 시 오프셋 보존용) */
    private val todayPersistedSteps: suspend (Long) -> Int = { 0 },
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val resolver = context.contentResolver

    /** 기기가 켜진 횟수. 바뀌었으면 그 사이 재부팅했다. 못 읽으면 -1 */
    private fun bootCount(): Int =
        runCatching { Settings.Global.getInt(resolver, Settings.Global.BOOT_COUNT, -1) }.getOrDefault(-1)
    private val stepSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val events = Channel<Long>(capacity = Channel.CONFLATED)

    private val _todaySteps = MutableStateFlow(0)
    val todaySteps: StateFlow<Int> = _todaySteps

    private val _reading = MutableStateFlow(StepReading(LocalDate.now().toEpochDay(), 0))

    /**
     * [todaySteps] 와 같은 값에 그 값이 어느 날의 것인지를 붙인 것 — 화면용(점검 2026-11-17 #24). 자정이 지나도 다음 센서
     * 이벤트 전까지 [todaySteps] 는 어제 값이라, 화면은 날짜가 다르면 오늘 걸음을 0 으로 보인다(StepReading.shownOn).
     * 걸음을 세는 방법(기준점 · 재부팅 · 넘겨받기)은 바꾸지 않는다.
     */
    val reading: StateFlow<StepReading> = _reading

    /** 디버그 시뮬레이션으로 더한 걸음 (센서 미지원 환경용) */
    private var simulatedSteps = 0

    val isAvailable: Boolean get() = stepSensor != null

    /**
     * [todaySteps]가 실제 측정값을 반영하는지 여부.
     * StateFlow 초기값 0은 센서 이벤트가 처리되기 전까지는 신뢰할 수 없으므로,
     * 세션 걸음 집계는 이 플래그가 true가 된 뒤의 방출부터 사용해야 한다.
     * (센서가 없는 기기는 시뮬레이션 값만 반영되므로 처음부터 신뢰 가능)
     */
    @Volatile
    var hasReading: Boolean = stepSensor == null
        private set

    private var started = false

    init {
        // 센서 이벤트를 순서대로 처리해 DataStore 기준점 갱신 경합을 피한다.
        scope.launch {
            for (cumulative in events) {
                processCumulative(cumulative)
            }
        }
    }

    /** 권한(ACTIVITY_RECOGNITION)이 허용된 뒤에 호출해야 한다. 중복 호출은 무시된다. */
    fun start() {
        if (started || stepSensor == null) return
        started = true
        sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI)
    }

    fun stop() {
        if (!started) return
        started = false
        sensorManager.unregisterListener(this)
    }

    /** 에뮬레이터/센서 미지원 기기에서 데모용으로 걸음을 더한다. */
    fun simulateSteps(count: Int) {
        hasReading = true
        simulatedSteps += count.coerceAtLeast(0)
        _todaySteps.value = _todaySteps.value + count.coerceAtLeast(0)
        _reading.value = StepReading(LocalDate.now().toEpochDay(), _todaySteps.value)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val cumulative = event.values.firstOrNull()?.toLong() ?: return
        events.trySend(cumulative)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private suspend fun processCumulative(cumulative: Long) {
        val today = LocalDate.now().toEpochDay()
        val (savedDay, savedBaseline) = prefs.baseline()
        val boot = bootCount()
        val savedBoot = prefs.baselineBoot()
        val baseline = when {
            savedDay != today -> {
                // 날짜가 바뀐 경우. 여기서 기준점을 그냥 지금 누적값으로 잡으면
                // **앱을 열기 전에 걸은 오늘 걸음이 통째로 사라진다** — 아침에
                // 5천 보를 걷고 점심에 앱을 열면 0보로 시작한다.
                //
                // 센서 누적값은 마지막 측정 이후의 걸음을 그대로 들고 있으므로,
                // 그 차이를 오늘로 넘겨준다. 다만 두 가지로 묶는다.
                //  - 어제 측정한 경우에만 넘긴다. 며칠 만에 열었다면 그 사이
                //    걸음이 전부 오늘로 쏟아지므로 그냥 새로 센다.
                //  - [CARRY_OVER_CAP]으로 막는다. 넘어오는 몫에는 어제 저녁
                //    걸음이 섞여 있어, 하루치를 넘게 얹어줄 이유가 없다.
                val carried = carryOver(savedDay, savedBaseline, today, cumulative)
                val newBaseline = cumulative - carried
                prefs.setBaseline(today, newBaseline, boot)
                newBaseline
            }
            // 재부팅으로 센서 누적값이 초기화된 경우:
            // 오늘 이미 기록된 걸음 수를 오프셋으로 보존해 이어서 센다.
            //
            // "누적값 < 기준점"만으로는 못 잡는다. 오늘 한 번 재부팅한 뒤에는
            // 기준점이 음수라 두 번째 재부팅이 안 보이고, 기준점이 작으면 부팅 뒤
            // 쌓인 누적값이 금방 기준점을 넘는다. 그래서 부팅 횟수를 함께 적어 두고,
            // 바뀌었으면 재부팅으로 본다(확실한 신호만 쓴다 — 저장된 걸음과 비교하면
            // 자정·시간대 변경 때 재부팅으로 오인해 걸음이 부풀어 오른다).
            (boot >= 0 && savedBoot >= 0 && boot != savedBoot) || cumulative < savedBaseline -> {
                val preBoot = todayPersistedSteps(today).coerceAtLeast(0)
                // 부팅 뒤 누적값은 0부터 다시 쌓인 것이다 — 부팅 뒤 걸음까지 그대로 센다.
                val newBaseline = -preBoot.toLong()
                prefs.setBaseline(today, newBaseline, boot)
                newBaseline
            }
            else -> {
                // 부팅 횟수를 아직 안 적어 둔 기준점(이전 버전)이면 지금 값을 적어 둔다
                if (savedBoot < 0 && boot >= 0) prefs.setBaseline(today, savedBaseline, boot)
                savedBaseline
            }
        }
        // 오늘 이미 적힌 걸음(앱이 꺼진 동안 구글 Play 서비스가 기록해 둔 것을 걸음 일꾼이 채운 값 — StepHistory)보다 작게 세지 않는다.
        // 기준점을 내려 센서가 그 값부터 이어 세게 한다(예전엔 작은 값은 버려져 앱을 연 뒤 걸음이 그 값을 넘을 때까지 멈춰 보였다)
        lastCumulative = cumulative
        val floor = floorOf(today) - simulatedSteps
        val base = if (floor > 0 && cumulative - baseline < floor) {
            val lowered = cumulative - floor
            prefs.setBaseline(today, lowered, boot)
            lowered
        } else {
            baseline
        }
        val sensorSteps = (cumulative - base).toInt().coerceAtLeast(0)
        _todaySteps.value = sensorSteps + simulatedSteps
        _reading.value = StepReading(today, _todaySteps.value)
        hasReading = true
    }

    /** 마지막 센서 누적값 — 바닥이 오르면 다시 센다. 아직 못 받았으면 -1 */
    @Volatile private var lastCumulative = -1L
    @Volatile private var floorDay = Long.MIN_VALUE
    @Volatile private var floorSteps = 0

    /** 오늘 걸음의 바닥 — 이 프로세스에서 처음 볼 때 폰 기록에서 읽고, [raiseFloor] 가 올린다 */
    private suspend fun floorOf(today: Long): Int {
        if (floorDay != today) {
            floorSteps = todayPersistedSteps(today).coerceAtLeast(0)
            floorDay = today
        }
        return floorSteps
    }

    /**
     * 오늘([day]) 걸음이 적어도 [steps] 라고 알린다(앱이 꺼진 동안의 기록 — [StepHistory]). 지금 센 값보다 크면 센서가 그 값부터 이어 센다.
     * 오늘이 아니면 무시한다.
     */
    fun raiseFloor(day: Long, steps: Int) {
        if (day != LocalDate.now().toEpochDay() || steps <= 0) return
        if (floorDay == day && steps <= floorSteps) return
        floorSteps = if (floorDay == day) maxOf(floorSteps, steps) else steps
        floorDay = day
        val last = lastCumulative
        if (last >= 0) events.trySend(last)
    }

    /**
     * 날짜가 바뀔 때 오늘로 넘겨줄 걸음 수.
     *
     * 어제 마지막으로 본 누적값은 `어제 기준점 + 어제 저장된 걸음`으로 되살릴 수
     * 있다. 지금 누적값에서 그걸 빼면 "마지막 측정 이후 걸은 수"가 나온다.
     */
    private suspend fun carryOver(
        savedDay: Long,
        savedBaseline: Long,
        today: Long,
        cumulative: Long,
    ): Long {
        if (savedDay != today - 1 || savedBaseline < 0) return 0L
        val lastSeen = savedBaseline + todayPersistedSteps(savedDay).coerceAtLeast(0)
        if (cumulative < lastSeen) return 0L
        return (cumulative - lastSeen).coerceAtMost(CARRY_OVER_CAP)
    }

    private companion object {
        /** 자정을 넘겨 넘겨받을 수 있는 걸음 상한 — 활동적인 하루치 */
        const val CARRY_OVER_CAP = 12_000L
    }
}

/** 걸음 수와 그것을 잰 날(epoch day) */
data class StepReading(val day: Long, val steps: Int) {
    /** [today] 에 보일 오늘 걸음 — 다른 날에 잰 값이면 0(새 날의 첫 센서 값이 오기 전) */
    fun shownOn(today: Long): Int = if (day == today) steps else 0
}
