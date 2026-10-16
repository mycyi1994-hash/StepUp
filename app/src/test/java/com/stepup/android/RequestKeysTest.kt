package com.stepup.android

import com.stepup.android.data.repo.RequestKeys
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * 값을 치르는 요청의 번호 — 답을 잃은 요청을 다시 보내면 같은 번호(서버가 한 번만 치른다),
 * 답을 받았거나 한참 지난 뒤의 요청은 새 번호(옛 영수증으로 새 구매가 "됐어요"가 되지 않게).
 */
class RequestKeysTest {
    private var clock = 1_000_000L
    private val keys = RequestKeys { clock }

    @Test fun `답을 잃은 요청을 다시 보내면 같은 번호다`() {
        val first = keys.keyFor("REPAIR:7")
        clock += 60_000
        assertEquals(first, keys.keyFor("REPAIR:7"))
    }

    @Test fun `답을 받은 뒤의 요청은 새 번호다`() {
        val first = keys.keyFor("BOOST")
        keys.done("BOOST", first)
        assertNotEquals(first, keys.keyFor("BOOST"))
    }

    @Test fun `답을 잃은 번호도 오래 쓰지 않으면 버린다`() {
        val first = keys.keyFor("UPGRADE:3")
        clock += RequestKeys.TTL_MS + 1
        assertNotEquals(first, keys.keyFor("UPGRADE:3"))
    }

    @Test fun `다시 보낼 때마다 버리는 시각이 늦춰진다`() {
        val first = keys.keyFor("UPGRADE:3")
        repeat(3) {
            clock += RequestKeys.TTL_MS - 1
            assertEquals(first, keys.keyFor("UPGRADE:3"))
        }
    }

    @Test fun `늦게 온 옛 답은 새 번호를 지우지 않는다`() {
        val old = keys.keyFor("BOOST")
        clock += RequestKeys.TTL_MS + 1
        val fresh = keys.keyFor("BOOST")
        keys.done("BOOST", old)
        assertEquals(fresh, keys.keyFor("BOOST"))
    }

    @Test fun `요청마다 번호가 따로다`() {
        assertNotEquals(keys.keyFor("REPAIR:1"), keys.keyFor("REPAIR:2"))
    }
}
