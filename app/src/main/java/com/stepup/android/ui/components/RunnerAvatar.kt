package com.stepup.android.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.stepup.android.domain.AvatarGender
import com.stepup.android.domain.AvatarLook
import com.stepup.android.domain.AvatarSkin
import com.stepup.android.domain.Outfit
import com.stepup.android.domain.Sneaker
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * 러너 캐릭터.
 *
 * 둥근 머리, 발광하는 눈, 작고 단단한 몸, 큼직한 운동화. 사실적인 사람이
 * 아니라 게임 캐릭터다.
 *
 * ── 그림이 아니라 코드로 그리는 이유 ──
 *
 * 입힌 것이 바로 바뀌어야 하기 때문이다. 옷을 입은 그림 한 장이면 갈아입힐
 * 수 없고, 옷마다 그림을 따로 두면 신발 52종 × 의상 × 성별만큼 그림이
 * 필요하다. 층을 나눠 그리면 성별 · 의상 · 신발이 각각 따로 바뀐다.
 *
 * ── 피부 ──
 *
 * 노출된 부위는 **전부** [skinCapsule] 과 [skinCircle] 두 함수로만 그린다.
 * 두 함수는 [AvatarSkin] 의 한 값만 읽는다. 얼굴 · 귀 · 목 · 손 · 팔뚝 ·
 * 허벅지 · 무릎 · 종아리가 모두 같은 색일 수밖에 없는 구조다. 부위마다 색을
 * 넘기게 두면, 언젠가 한 군데만 살구색이 된다.
 *
 * 설계 좌표는 가로 100 × 세로 142 이다. 캔버스에 비율을 지켜 맞춘다.
 */
enum class AvatarPose { IDLE, RUN, CHEER }

private const val BOX_W = 100f
private const val BOX_H = 142f

private val SKIN = Color(AvatarSkin.BASE)
private val SKIN_LIGHT = Color(AvatarSkin.LIGHT)
private val RIM = Color(AvatarSkin.RIM)
private val EYE = Color(0xFF5CF2FF)
private val HAIR = Color(0xFF0B0E16)

/** 기본 운동화 — NFT 신발을 안 신었을 때 */
private val PLAIN_UPPER = Color(0xFFE9EEF6)
private val PLAIN_STRIPE = Color(0xFF1677FF)
private val PLAIN_SOLE = Color(0xFF0E1320)

/**
 * 자세 하나. 각도는 도(°)이고 0 이 아래, 양수가 화면 오른쪽으로 돈다.
 * 왼쪽·오른쪽은 **화면 기준**이다.
 */
private data class Pose(
    val tilt: Float = 0f,
    val lift: Float = 0f,
    val lUpper: Float, val lFore: Float,
    val rUpper: Float, val rFore: Float,
    val lThigh: Float, val lThighLen: Float = 12f, val lCalf: Float, val lCalfLen: Float = 13f,
    val rThigh: Float, val rThighLen: Float = 12f, val rCalf: Float, val rCalfLen: Float = 13f,
    /** 몸 앞으로 오는 팔 — 몸통 뒤가 아니라 위에 그린다 */
    val lArmFront: Boolean = false,
    val rArmFront: Boolean = false,
    val happyEyes: Boolean = false,
)

private val IDLE = Pose(
    lUpper = -14f, lFore = -6f, rUpper = 14f, rFore = 6f,
    lThigh = -5f, lCalf = -2f, rThigh = 5f, rCalf = 2f,
)

private val RUN = Pose(
    tilt = 5f, lift = -2f,
    // 왼팔은 뒤로 흔들어 주먹이 엉덩이 옆에 오고,
    // 오른팔은 팔꿈치를 굽혀 주먹이 가슴 앞으로 온다
    lUpper = -25f, lFore = -35f,
    rUpper = 12f, rFore = 200f, rArmFront = true,
    // 왼무릎을 앞으로 들어 올리고(짧게 보인다), 오른발로 땅을 딛는다
    lThigh = -28f, lThighLen = 9f, lCalf = 22f, lCalfLen = 11f,
    rThigh = 8f, rCalf = -4f,
)

private val CHEER = Pose(
    lift = -1f,
    lUpper = -150f, lFore = -172f, rUpper = 150f, rFore = 172f,
    lArmFront = true, rArmFront = true,
    lThigh = -14f, lCalf = -6f, rThigh = 14f, rCalf = 6f,
    happyEyes = true,
)

@Composable
fun RunnerAvatar(
    look: AvatarLook,
    modifier: Modifier = Modifier,
    pose: AvatarPose = AvatarPose.IDLE,
    /** 발밑의 파란 원판 */
    showGround: Boolean = true,
    /** 숨 쉬듯 살짝 오르내린다. 모션 줄이기가 켜져 있으면 멈춰 있다. */
    animate: Boolean = true,
    contentDescription: String? = null,
) {
    val spec = when (pose) {
        AvatarPose.IDLE -> IDLE
        AvatarPose.RUN -> RUN
        AvatarPose.CHEER -> CHEER
    }
    // ambientPhase 는 모션 줄이기일 때 0.5 로 멈춘 값을 준다
    val phase by ambientPhase(if (pose == AvatarPose.RUN) 700 else 2600, reverse = true)
    val bob = if (animate) (phase - 0.5f) * (if (pose == AvatarPose.RUN) 3f else 1.2f) else 0f

    Canvas(
        modifier = modifier.semantics {
            if (contentDescription != null) this.contentDescription = contentDescription
        },
    ) {
        val rig = Rig(this)
        if (showGround) drawGround(rig)
        rotate(spec.tilt, pivot = rig.p(50f, 134f)) {
            drawRunner(rig, look, spec, bob + spec.lift)
        }
    }
}

// ── 좌표 ────────────────────────────────────────────────────────────

private class Rig(scope: DrawScope) {
    val s = min(scope.size.width / BOX_W, scope.size.height / BOX_H)
    val ox = (scope.size.width - BOX_W * s) / 2f
    val oy = scope.size.height - BOX_H * s
    fun p(x: Float, y: Float) = Offset(ox + x * s, oy + y * s)
    fun p(o: Offset) = Offset(ox + o.x * s, oy + o.y * s)
    fun u(v: Float) = v * s
}

/** [from] 에서 [angle] 방향으로 [len] 만큼 간 점 (설계 좌표) */
private fun reach(from: Offset, angle: Float, len: Float): Offset {
    val r = angle * PI.toFloat() / 180f
    return Offset(from.x + len * sin(r), from.y + len * cos(r))
}

// ── 피부 — 이 두 함수 말고는 피부를 그리는 곳이 없다 ──────────────

private fun DrawScope.skinCapsule(rig: Rig, a: Offset, b: Offset, width: Float) {
    // 파란 테두리 조명 한 겹, 그 위에 피부
    drawLine(RIM.copy(alpha = 0.38f), rig.p(a), rig.p(b), rig.u(width + 2.2f), StrokeCap.Round)
    drawLine(SKIN, rig.p(a), rig.p(b), rig.u(width), StrokeCap.Round)
}

private fun DrawScope.skinCircle(rig: Rig, c: Offset, r: Float, lit: Boolean = false) {
    drawCircle(RIM.copy(alpha = 0.40f), rig.u(r + 1.1f), rig.p(c))
    if (lit) {
        // 빛을 받는 쪽이 조금 밝을 뿐, 같은 피부색이다
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(SKIN_LIGHT, SKIN),
                center = rig.p(c.x - r * 0.35f, c.y - r * 0.35f),
                radius = rig.u(r * 1.3f),
            ),
            radius = rig.u(r),
            center = rig.p(c),
        )
    } else {
        drawCircle(SKIN, rig.u(r), rig.p(c))
    }
}

// ── 옷 ──────────────────────────────────────────────────────────────

private fun DrawScope.cloth(rig: Rig, a: Offset, b: Offset, width: Float, color: Color) {
    drawLine(RIM.copy(alpha = 0.22f), rig.p(a), rig.p(b), rig.u(width + 1.6f), StrokeCap.Round)
    drawLine(color, rig.p(a), rig.p(b), rig.u(width), StrokeCap.Round)
}

// ── 그리기 ──────────────────────────────────────────────────────────

private fun DrawScope.drawGround(rig: Rig) {
    val c = rig.p(50f, 135f)
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(RIM.copy(alpha = 0.45f), Color.Transparent),
            center = c,
            radius = rig.u(34f),
        ),
        topLeft = Offset(c.x - rig.u(34f), c.y - rig.u(6f)),
        size = Size(rig.u(68f), rig.u(12f)),
    )
    drawOval(
        color = RIM.copy(alpha = 0.55f),
        topLeft = Offset(c.x - rig.u(24f), c.y - rig.u(3.6f)),
        size = Size(rig.u(48f), rig.u(7.2f)),
        style = Stroke(width = rig.u(0.8f)),
    )
}

private fun DrawScope.drawRunner(rig: Rig, look: AvatarLook, pose: Pose, dy: Float) {
    val o = look.outfit
    val top = Color(o.top)
    val topShade = Color(o.topShade)
    val trim = Color(o.trim)

    fun at(x: Float, y: Float) = Offset(x, y + dy)

    val head = at(50f, 40f)
    val lShoulder = at(36f, 70f)
    val rShoulder = at(64f, 70f)
    val lHip = at(44f, 100f)
    val rHip = at(56f, 100f)

    // 머리카락(여자) — 머리 뒤로 늘어진 포니테일
    if (look.gender == AvatarGender.FEMALE) {
        val tail = Path().apply {
            moveTo(rig.p(at(66f, 30f)).x, rig.p(at(66f, 30f)).y)
            cubicTo(
                rig.p(at(84f, 34f)).x, rig.p(at(84f, 34f)).y,
                rig.p(at(86f, 56f)).x, rig.p(at(86f, 56f)).y,
                rig.p(at(78f, 70f)).x, rig.p(at(78f, 70f)).y,
            )
            cubicTo(
                rig.p(at(76f, 58f)).x, rig.p(at(76f, 58f)).y,
                rig.p(at(72f, 44f)).x, rig.p(at(72f, 44f)).y,
                rig.p(at(62f, 38f)).x, rig.p(at(62f, 38f)).y,
            )
            close()
        }
        drawPath(tail, RIM.copy(alpha = 0.35f), style = Stroke(rig.u(1.6f)))
        drawPath(tail, HAIR)
    }

    // 팔 — 몸 뒤로 가는 것 먼저
    if (!pose.lArmFront) drawArm(rig, lShoulder, pose.lUpper, pose.lFore, o, top, trim)
    if (!pose.rArmFront) drawArm(rig, rShoulder, pose.rUpper, pose.rFore, o, top, trim)

    // 다리
    drawLeg(rig, lHip, pose.lThigh, pose.lThighLen, pose.lCalf, pose.lCalfLen, o, look.shoe, left = true)
    drawLeg(rig, rHip, pose.rThigh, pose.rThighLen, pose.rCalf, pose.rCalfLen, o, look.shoe, left = false)

    // 반바지 — 허벅지 위쪽을 덮는다
    val shortsTop = rig.p(at(35f, 92f))
    drawRoundRect(
        color = Color(o.shorts),
        topLeft = shortsTop,
        size = Size(rig.u(30f), rig.u(14f)),
        cornerRadius = CornerRadius(rig.u(5f)),
    )
    drawLine(
        topShade,
        rig.p(at(50f, 97f)),
        rig.p(at(50f, 106f)),
        rig.u(0.9f),
    )

    // 목
    skinCapsule(rig, at(50f, 58f), at(50f, 66f), 9f)

    // 몸통 — 후디
    val torsoTop = rig.p(at(34f, 63f))
    drawRoundRect(
        color = RIM.copy(alpha = 0.25f),
        topLeft = Offset(torsoTop.x - rig.u(0.9f), torsoTop.y - rig.u(0.9f)),
        size = Size(rig.u(33.8f), rig.u(35.8f)),
        cornerRadius = CornerRadius(rig.u(11f)),
    )
    drawRoundRect(
        brush = Brush.verticalGradient(
            listOf(top, topShade),
            startY = torsoTop.y,
            endY = torsoTop.y + rig.u(34f),
        ),
        topLeft = torsoTop,
        size = Size(rig.u(32f), rig.u(34f)),
        cornerRadius = CornerRadius(rig.u(10f)),
    )
    // 모자 달린 목둘레
    val hood = Path().apply {
        val a = rig.p(at(40f, 64f)); moveTo(a.x, a.y)
        val c1 = rig.p(at(42f, 74f)); val c2 = rig.p(at(58f, 74f)); val b = rig.p(at(60f, 64f))
        cubicTo(c1.x, c1.y, c2.x, c2.y, b.x, b.y)
    }
    drawPath(hood, topShade, style = Stroke(rig.u(3.2f), cap = StrokeCap.Round))
    drawPath(hood, trim.copy(alpha = 0.55f), style = Stroke(rig.u(0.9f), cap = StrokeCap.Round))
    // 끈
    drawLine(trim, rig.p(at(46f, 71f)), rig.p(at(45.5f, 78f)), rig.u(1.0f), StrokeCap.Round)
    drawLine(trim, rig.p(at(54f, 71f)), rig.p(at(54.5f, 78f)), rig.u(1.0f), StrokeCap.Round)
    // 가슴의 UP 화살표
    val logo = Path().apply {
        val l = rig.p(at(46f, 88f)); moveTo(l.x, l.y)
        val t = rig.p(at(50f, 83f)); lineTo(t.x, t.y)
        val r = rig.p(at(54f, 88f)); lineTo(r.x, r.y)
    }
    drawPath(logo, trim, style = Stroke(rig.u(1.6f), cap = StrokeCap.Round))
    // 옆선
    drawLine(trim.copy(alpha = 0.8f), rig.p(at(35.5f, 74f)), rig.p(at(35.5f, 94f)), rig.u(0.9f))
    drawLine(trim.copy(alpha = 0.8f), rig.p(at(64.5f, 74f)), rig.p(at(64.5f, 94f)), rig.u(0.9f))

    // 머리
    skinCircle(rig, at(27f, 44f), 4.4f)
    skinCircle(rig, at(73f, 44f), 4.4f)
    skinCircle(rig, head, 23f, lit = true)
    drawEyes(rig, head, pose.happyEyes)
    drawCap(rig, head, o, look.gender)

    // 팔 — 몸 앞으로 오는 것
    if (pose.lArmFront) drawArm(rig, lShoulder, pose.lUpper, pose.lFore, o, top, trim)
    if (pose.rArmFront) drawArm(rig, rShoulder, pose.rUpper, pose.rFore, o, top, trim)
}

private fun DrawScope.drawArm(
    rig: Rig,
    shoulder: Offset,
    upperAngle: Float,
    foreAngle: Float,
    outfit: Outfit,
    top: Color,
    trim: Color,
) {
    val elbow = reach(shoulder, upperAngle, 13f)
    val wrist = reach(elbow, foreAngle, 11.5f)
    cloth(rig, shoulder, elbow, 8.6f, top)
    if (outfit.longSleeve) {
        cloth(rig, elbow, wrist, 7.8f, top)
        // 소매 끝단
        val cuff = reach(elbow, foreAngle, 10f)
        drawLine(trim, rig.p(cuff), rig.p(wrist), rig.u(7.8f), StrokeCap.Butt)
    } else {
        // 반소매 — 팔뚝이 보인다. 같은 피부다.
        skinCapsule(rig, elbow, wrist, 6.6f)
        val hem = reach(shoulder, upperAngle, 11f)
        drawLine(trim, rig.p(hem), rig.p(elbow), rig.u(8.6f), StrokeCap.Butt)
    }
    // 손
    skinCircle(rig, reach(wrist, foreAngle, 2.2f), 4.8f)
}

private fun DrawScope.drawLeg(
    rig: Rig,
    hip: Offset,
    thighAngle: Float,
    thighLen: Float,
    calfAngle: Float,
    calfLen: Float,
    outfit: Outfit,
    shoe: Sneaker?,
    left: Boolean,
) {
    val knee = reach(hip, thighAngle, thighLen)
    val ankle = reach(knee, calfAngle, calfLen)
    // 허벅지 · 무릎 · 종아리 — 모두 피부
    skinCapsule(rig, hip, knee, 8.4f)
    skinCircle(rig, knee, 4.3f)
    skinCapsule(rig, knee, ankle, 7.6f)
    // 양말
    val sockTop = reach(knee, calfAngle, calfLen - 3.4f)
    drawLine(Color(outfit.socks), rig.p(sockTop), rig.p(ankle), rig.u(7.8f), StrokeCap.Butt)
    drawShoe(rig, ankle, calfAngle, shoe, left)
}

/**
 * 운동화 — 앞에서 본 큼직한 한 켤레.
 *
 * NFT 신발을 신었으면 그 신발의 속성 색을 쓴다. 보관함의 신발 그림과
 * 똑같이 그리지는 않지만, 같은 색이라 "그 신발"로 읽힌다.
 */
private fun DrawScope.drawShoe(rig: Rig, ankle: Offset, calfAngle: Float, shoe: Sneaker?, left: Boolean) {
    val upper = shoe?.let { Color(it.faction.upper) } ?: PLAIN_UPPER
    val stripe = shoe?.let { Color(it.faction.accent) } ?: PLAIN_STRIPE
    val sole = shoe?.let { Color(it.faction.accentSoft) } ?: PLAIN_SOLE
    // 발끝은 바깥으로 조금 벌어진다
    val center = Offset(ankle.x + if (left) -2.2f else 2.2f, ankle.y + 3.6f)
    val c = rig.p(center)
    val w = rig.u(17f)
    val h = rig.u(9.6f)
    rotate(-calfAngle * 0.6f, pivot = c) {
        // 발광 테두리
        drawRoundRect(
            color = stripe.copy(alpha = 0.35f),
            topLeft = Offset(c.x - w / 2 - rig.u(1f), c.y - h / 2 - rig.u(1f)),
            size = Size(w + rig.u(2f), h + rig.u(2f)),
            cornerRadius = CornerRadius(rig.u(5.8f)),
        )
        // 갑피
        drawRoundRect(
            color = upper,
            topLeft = Offset(c.x - w / 2, c.y - h / 2),
            size = Size(w, h),
            cornerRadius = CornerRadius(rig.u(5f)),
        )
        // 밑창
        drawRoundRect(
            color = sole,
            topLeft = Offset(c.x - w / 2, c.y + h / 2 - rig.u(2.6f)),
            size = Size(w, rig.u(2.6f)),
            cornerRadius = CornerRadius(rig.u(1.3f)),
        )
        // 번개 줄무늬
        val bolt = Path().apply {
            moveTo(c.x - rig.u(4.5f), c.y - rig.u(1.5f))
            lineTo(c.x + rig.u(0.5f), c.y - rig.u(2.4f))
            lineTo(c.x - rig.u(0.8f), c.y + rig.u(0.2f))
            lineTo(c.x + rig.u(4.5f), c.y - rig.u(0.6f))
        }
        drawPath(bolt, stripe, style = Stroke(rig.u(1.3f), cap = StrokeCap.Round))
        // 발목 입구
        drawOval(
            color = Color(0xFF0B0E16),
            topLeft = Offset(c.x - rig.u(3.6f), c.y - h / 2 - rig.u(0.6f)),
            size = Size(rig.u(7.2f), rig.u(2.4f)),
        )
    }
}

private fun DrawScope.drawEyes(rig: Rig, head: Offset, happy: Boolean) {
    val y = head.y + 4f
    for (dx in listOf(-8.5f, 8.5f)) {
        val c = rig.p(head.x + dx, y)
        if (happy) {
            // ^ ^ — 웃는 눈
            val arc = Path().apply {
                moveTo(c.x - rig.u(3.4f), c.y + rig.u(1.4f))
                lineTo(c.x, c.y - rig.u(2.4f))
                lineTo(c.x + rig.u(3.4f), c.y + rig.u(1.4f))
            }
            drawPath(arc, EYE.copy(alpha = 0.35f), style = Stroke(rig.u(3.4f), cap = StrokeCap.Round))
            drawPath(arc, EYE, style = Stroke(rig.u(1.7f), cap = StrokeCap.Round))
        } else {
            // 세로로 긴 둥근 눈 — 번지는 빛 한 겹, 가운데 심 한 겹
            drawRoundRect(
                color = EYE.copy(alpha = 0.28f),
                topLeft = Offset(c.x - rig.u(4.2f), c.y - rig.u(6.4f)),
                size = Size(rig.u(8.4f), rig.u(12.8f)),
                cornerRadius = CornerRadius(rig.u(4.2f)),
            )
            drawRoundRect(
                color = EYE,
                topLeft = Offset(c.x - rig.u(2.5f), c.y - rig.u(4.6f)),
                size = Size(rig.u(5f), rig.u(9.2f)),
                cornerRadius = CornerRadius(rig.u(2.5f)),
            )
            drawCircle(Color.White.copy(alpha = 0.85f), rig.u(0.9f), Offset(c.x - rig.u(0.8f), c.y - rig.u(2.4f)))
        }
    }
}

private fun DrawScope.drawCap(rig: Rig, head: Offset, outfit: Outfit, gender: AvatarGender) {
    val cap = Color(outfit.cap)
    val logo = Color(outfit.capLogo)
    // 옆머리(여자) — 모자 밑으로 살짝
    if (gender == AvatarGender.FEMALE) {
        drawCircle(HAIR, rig.u(5.2f), rig.p(head.x - 19f, head.y - 6f))
        drawCircle(HAIR, rig.u(5.2f), rig.p(head.x + 19f, head.y - 6f))
    }
    // 둥근 윗부분
    val domeTop = rig.p(head.x - 24.5f, head.y - 26f)
    val dome = Path().apply {
        addArcCompat(
            left = domeTop.x,
            top = domeTop.y,
            right = domeTop.x + rig.u(49f),
            bottom = domeTop.y + rig.u(49f),
            startDeg = 180f,
            sweepDeg = 180f,
        )
        close()
    }
    drawPath(dome, RIM.copy(alpha = 0.35f), style = Stroke(rig.u(1.8f)))
    drawPath(dome, cap)
    // 챙 — 앞에서 보면 납작한 타원
    val brimC = rig.p(head.x + 2f, head.y - 1.5f)
    drawOval(
        color = cap,
        topLeft = Offset(brimC.x - rig.u(28f), brimC.y - rig.u(4.8f)),
        size = Size(rig.u(56f), rig.u(9.6f)),
    )
    drawOval(
        color = Color.Black.copy(alpha = 0.25f),
        topLeft = Offset(brimC.x - rig.u(26f), brimC.y + rig.u(0.2f)),
        size = Size(rig.u(52f), rig.u(4f)),
    )
    // 모자 앞의 UP
    val l = rig.p(head.x - 5f, head.y - 10f)
    val t = rig.p(head.x, head.y - 15f)
    val r = rig.p(head.x + 5f, head.y - 10f)
    val arrow = Path().apply { moveTo(l.x, l.y); lineTo(t.x, t.y); lineTo(r.x, r.y) }
    drawPath(arrow, logo.copy(alpha = 0.35f), style = Stroke(rig.u(3.4f), cap = StrokeCap.Round))
    drawPath(arrow, logo, style = Stroke(rig.u(1.8f), cap = StrokeCap.Round))
}

/** 반원 윤곽을 Path 에 더한다 — Rect 를 만들지 않고 좌표로 */
private fun Path.addArcCompat(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    startDeg: Float,
    sweepDeg: Float,
) {
    arcTo(
        rect = androidx.compose.ui.geometry.Rect(left, top, right, bottom),
        startAngleDegrees = startDeg,
        sweepAngleDegrees = sweepDeg,
        forceMoveTo = true,
    )
}
