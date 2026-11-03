package com.stepup.android

import com.stepup.android.domain.AtomicCounter
import com.stepup.android.domain.ConcurrentMap
import com.stepup.android.domain.ConcurrentSet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** ConcurrentMap · AtomicCounter · ConcurrentSet 가 ConcurrentHashMap · AtomicInteger 와 같은 답을 내는가 */
class ConcurrentTest {
    @Test fun mapMatchesConcurrentHashMapContract() {
        val m = ConcurrentMap<Long, String>()
        assertNull(m.put(1, "a"))
        assertEquals("a", m.put(1, "b"))
        assertEquals("b", m.putIfAbsent(1, "c"))
        assertNull(m.putIfAbsent(2, "x"))
        assertFalse(m.remove(1, "a"))
        assertTrue(m.remove(1, "b"))
        assertNull(m[1])
        assertEquals("x1", m.compute(2) { _, v -> v + "1" })
        assertNull(m.compute(2) { _, _ -> null })
        assertFalse(m.containsKey(2))
        assertEquals("z", m.computeIfAbsent(3) { "z" })
        assertEquals("z", m.computeIfAbsent(3) { "q" })
        m[4] = "w"
        assertEquals(setOf(3L, 4L), m.keys)
        assertEquals("w", m.remove(4))
    }

    @Test fun counterAndSet() {
        val c = AtomicCounter()
        assertEquals(1, c.incrementAndGet())
        assertEquals(1, c.get())
        val s = ConcurrentSet<Long>()
        assertTrue(s.add(5))
        assertFalse(s.add(5))
        assertTrue(5L in s)
        assertTrue(s.remove(5))
        assertFalse(s.remove(5))
    }
}
