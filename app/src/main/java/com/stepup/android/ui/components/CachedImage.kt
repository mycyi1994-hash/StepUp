package com.stepup.android.ui.components

import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.BitmapFactory
import android.util.LruCache
import android.util.TypedValue
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

/**
 * painterResource 대신 쓰는 그림 — 한 번 푼 비트맵을 앱 전체에서 다시 쓴다.
 *
 * painterResource 는 그림(webp · png)을 화면 스레드에서 원본 크기로 풀고, 그 화면이 사라지면 버린다. 신발 칸 · 등급 무대 겹 ·
 * 탭 바탕 사진처럼 큰 그림이 탭을 오가거나 목록을 넘길 때마다 다시 풀려 프레임이 끊겼다(2026-10-04 "렉 걸린다").
 * 여기서는 같은 그림을 한 번만 풀어 기억해 두고, 자주 여는 화면의 그림은 [prewarm] 으로 미리 뒤에서 풀어 둔다.
 * 벡터(xml) 그림은 Compose 가 이미 캐시하므로 painterResource 를 그대로 쓴다.
 */
@Composable
fun cachedPainterResource(@DrawableRes id: Int): Painter {
    val resources = LocalContext.current.resources
    // 밤/낮 · 밀도가 바뀌면 다른 그림을 고를 수 있다 — 설정을 열쇠에 넣는다
    val configuration = LocalConfiguration.current
    val bitmap = remember(resources, configuration, id) { RasterCache.get(resources, id) }
    return if (bitmap != null) remember(bitmap) { BitmapPainter(bitmap) } else painterResource(id)
}

internal object RasterCache {
    // 기기 메모리의 6분의 1, 많아도 96MB — 큰 신발 그림(640×640 ≈ 1.6MB) 수십 장 · 바탕 사진 몇 장이 들어간다
    private val maxKb = (Runtime.getRuntime().maxMemory() / 1024 / 6).coerceAtMost(96L * 1024).toInt().coerceAtLeast(16 * 1024)
    private val cache = object : LruCache<Long, ImageBitmap>(maxKb) {
        override fun sizeOf(key: Long, value: ImageBitmap): Int = (value.width.toLong() * value.height * 4 / 1024).toInt().coerceAtLeast(1)
    }
    private val vectors = ConcurrentHashMap.newKeySet<Int>()
    private val locks = ConcurrentHashMap<Long, Any>()
    private val warmer by lazy {
        // 보통 우선순위 — 낮추면 화면 스레드가 이 작업이 끝나기를 기다릴 때 오래 밀린다
        Executors.newSingleThreadExecutor { r -> Thread(r, "stepup-image-warm") }
    }

    private fun key(resources: Resources, id: Int): Long {
        val c = resources.configuration
        val variant = c.densityDpi * 4 + ((c.uiMode and Configuration.UI_MODE_NIGHT_MASK) shr 4)
        return (variant.toLong() shl 32) or (id.toLong() and 0xffffffffL)
    }

    /** 이미 풀어 둔 그림인가 — 화면 스레드에서 기다리지 않고 물어본다 */
    fun isReady(resources: Resources, @DrawableRes id: Int): Boolean = cache.get(key(resources, id)) != null

    /** 비트맵 그림이면 풀어서(또는 기억한 것을) 돌려준다. 벡터 · 풀 수 없는 그림은 null — 부르는 쪽이 painterResource 로 그린다. */
    fun get(resources: Resources, @DrawableRes id: Int): ImageBitmap? {
        if (id in vectors) return null
        val key = key(resources, id)
        cache.get(key)?.let { return it }
        // 같은 그림을 뒤(prewarm)와 화면이 함께 풀지 않게 — 뒤에서 푸는 중이면 기다렸다 그 결과를 쓴다
        synchronized(locks.getOrPut(key) { Any() }) {
            cache.get(key)?.let { return it }
            val path = runCatching { TypedValue().also { resources.getValue(id, it, true) }.string?.toString() }.getOrNull()
            if (path == null || path.endsWith(".xml")) {
                vectors += id
                return null
            }
            val bitmap = runCatching { BitmapFactory.decodeResource(resources, id) }.getOrNull() ?: run {
                vectors += id
                return null
            }
            // 화면에 처음 그리기 전에 GPU 로 올려 둔다(뒤에서 풀었을 때 첫 프레임이 덜 걸린다)
            bitmap.prepareToDraw()
            return bitmap.asImageBitmap().also { cache.put(key, it) }
        }
    }

    /** 곧 쓸 그림을 뒤에서 미리 푼다. 화면 스레드를 막지 않는다. */
    fun prewarm(resources: Resources, @DrawableRes vararg ids: Int) {
        val list = ids.filter { it != 0 }
        if (list.isEmpty()) return
        runCatching { warmer.execute { list.forEach { runCatching { get(resources, it) } } } }
    }
}
