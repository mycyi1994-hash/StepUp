package com.stepup.android.ui.screens.splash

import android.content.Context
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.view.Surface
import android.view.TextureView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.stepup.android.R

/**
 * 앱을 켜면 처음 나오는 인트로(2026-10-27 사용자 영상 — [docs/redesign/launch-intro-2026-10-27]).
 * 밤 다리 · 빛나는 발자국 · 번개로 STEPUP 로고 · 아래 로딩 줄, 4.6초 한 번.
 *
 * 영상(`res/raw/launch_intro.mp4`, 900 × 1950)과 마지막 장면 그림(`launch_intro_still` — 로딩 줄만 지웠다)은
 * 같은 틀에 같은 크기로 놓는다. 영상이 끝났는데 아직 준비 중이면 그림 위 같은 자리에 진짜 로딩 줄이 돈다.
 */
internal object LaunchIntroFrame {
    const val WIDTH = 900f
    const val HEIGHT = 1950f

    /** 영상 속 로딩 줄 — 가운데 · 높이의 85.5% · 너비의 30.5%(원본 1080 × 2340 의 y 2000 · 329px) */
    const val LINE_Y = 2000f / 2340f
    const val LINE_WIDTH = 329f / 1080f

    /** 영상 첫 장면 · 가장자리 바탕(#050A10) — 시스템 시작 화면에서 넘어올 때 번쩍이지 않게 같은 색 */
    val Ground = Color(0xFF050A10)

    /** 폰 세로 화면은 꽉 채우고(잘라 냄), 태블릿 · 가로처럼 넓은 화면은 다 보이게 넣는다(로고가 잘리지 않게) */
    fun fills(boxWidth: Float, boxHeight: Float): Boolean = boxHeight > 0f && boxWidth / boxHeight <= 0.6f

    /** 틀 안 영상 배율 — 꽉 채우면 큰 쪽, 넣으면 작은 쪽 */
    fun scale(boxWidth: Float, boxHeight: Float): Float {
        val sx = boxWidth / WIDTH
        val sy = boxHeight / HEIGHT
        return if (fills(boxWidth, boxHeight)) maxOf(sx, sy) else minOf(sx, sy)
    }
}

/**
 * 인트로 영상 한 번 재생. 끝나거나 · 재생하지 못하면 [onEnd] 를 한 번 부른다.
 * 소리는 [sound] 일 때만 — 오디오 포커스를 받지 않아 듣던 음악을 멈추지 않는다.
 * 앱이 뒤로 가면 멈추고, 돌아오면 이어서 재생한다.
 */
@Composable
internal fun LaunchIntroVideo(sound: Boolean, onEnd: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val end by rememberUpdatedState(onEnd)
    val player = remember { IntroPlayer(context.applicationContext, sound) { end() } }
    DisposableEffect(lifecycle, player) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> player.pause()
                Lifecycle.Event.ON_RESUME -> player.resume()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) player.pause()
        onDispose {
            lifecycle.removeObserver(observer)
            player.release()
        }
    }
    AndroidView(
        factory = { TextureView(it).apply { isOpaque = false; surfaceTextureListener = player.bind(this) } },
        modifier = modifier,
    )
}

/** TextureView 위 MediaPlayer — 화면 크기에 맞춰 [LaunchIntroFrame] 규칙대로 채우거나 넣는다 */
private class IntroPlayer(
    private val app: Context,
    private val sound: Boolean,
    private val onEnd: () -> Unit,
) : TextureView.SurfaceTextureListener {
    private var view: TextureView? = null
    private var player: MediaPlayer? = null
    private var surface: Surface? = null
    private var prepared = false
    private var resumed = true
    private var ended = false
    private var sourceWidth = LaunchIntroFrame.WIDTH
    private var sourceHeight = LaunchIntroFrame.HEIGHT

    fun bind(view: TextureView): IntroPlayer {
        this.view = view
        return this
    }

    override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
        if (ended) return
        fit(width, height)
        try {
            val target = Surface(texture).also { surface = it }
            player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build(),
                )
                app.resources.openRawResourceFd(R.raw.launch_intro).use { setDataSource(it.fileDescriptor, it.startOffset, it.length) }
                setSurface(target)
                if (!sound) setVolume(0f, 0f)
                setOnVideoSizeChangedListener { _, w, h ->
                    if (w > 0 && h > 0) {
                        sourceWidth = w.toFloat()
                        sourceHeight = h.toFloat()
                        view?.let { fit(it.width, it.height) }
                    }
                }
                setOnPreparedListener {
                    prepared = true
                    if (resumed && !ended) it.start()
                }
                setOnCompletionListener { finish() }
                setOnErrorListener { _, _, _ -> finish(); true }
                prepareAsync()
            }
        } catch (_: Exception) {
            finish()
        }
    }

    override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) = fit(width, height)

    override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {
        // 화면이 사라지면 영상도 끝 — 마지막 장면 그림이 대신한다
        finish()
        release()
        return true
    }

    override fun onSurfaceTextureUpdated(texture: SurfaceTexture) = Unit

    /** TextureView 는 영상을 틀에 늘려 그린다 — 가운데 기준으로 비율을 되돌려 채우기/넣기를 맞춘다 */
    private fun fit(width: Int, height: Int) {
        val target = view ?: return
        if (width <= 0 || height <= 0) return
        val w = width.toFloat()
        val h = height.toFloat()
        val fills = LaunchIntroFrame.fills(w, h)
        val scale = if (fills) maxOf(w / sourceWidth, h / sourceHeight) else minOf(w / sourceWidth, h / sourceHeight)
        target.setTransform(Matrix().apply { setScale(sourceWidth * scale / w, sourceHeight * scale / h, w / 2f, h / 2f) })
    }

    fun pause() {
        resumed = false
        if (prepared && !ended) runCatching { player?.pause() }
    }

    fun resume() {
        resumed = true
        if (prepared && !ended) runCatching { player?.start() }
    }

    private fun finish() {
        if (ended) return
        ended = true
        onEnd()
    }

    fun release() {
        ended = true
        prepared = false
        player?.let { runCatching { it.release() } }
        player = null
        surface?.release()
        surface = null
    }
}
