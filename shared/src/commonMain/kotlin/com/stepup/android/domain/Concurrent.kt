@file:OptIn(kotlin.concurrent.atomics.ExperimentalAtomicApi::class)

package com.stepup.android.domain

import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.AtomicReference

/**
 * 여러 일꾼이 함께 쓰는 표 · 수(java.util.concurrent 의 ConcurrentHashMap · AtomicInteger 자리, 2026-11-03 iOS C5).
 * 원자 참조의 바꿔 쓰기로 한 번에 하나 — compute 의 함수는 다시 불릴 수 있으니 부작용 없이 쓴다(ConcurrentHashMap 도 같은 약속).
 */
class ConcurrentMap<K, V> {
    private val ref = AtomicReference(emptyMap<K, V>())

    private inline fun update(change: (Map<K, V>) -> Map<K, V>): Pair<Map<K, V>, Map<K, V>> {
        while (true) {
            val old = ref.load()
            val new = change(old)
            if (ref.compareAndSet(old, new)) return old to new
        }
    }

    operator fun get(key: K): V? = ref.load()[key]
    operator fun set(key: K, value: V) { update { it + (key to value) } }
    fun put(key: K, value: V): V? = update { it + (key to value) }.first[key]
    fun putIfAbsent(key: K, value: V): V? = update { if (key in it) it else it + (key to value) }.first[key]
    fun containsKey(key: K): Boolean = key in ref.load()
    fun remove(key: K): V? = update { it - key }.first[key]

    /** 지금 값이 [value] 일 때만 지운다 — 지웠으면 true */
    fun remove(key: K, value: V): Boolean {
        var removed = false
        update { m -> removed = m[key] == value && key in m; if (removed) m - key else m }
        return removed
    }

    fun compute(key: K, f: (K, V?) -> V?): V? {
        var out: V? = null
        update { m -> out = f(key, m[key]); val v = out; if (v == null) m - key else m + (key to v) }
        return out
    }

    fun computeIfAbsent(key: K, f: (K) -> V): V {
        ref.load()[key]?.let { return it }
        var out: V? = null
        update { m -> val have = m[key]; if (have != null) { out = have; m } else { val v = f(key); out = v; m + (key to v) } }
        @Suppress("UNCHECKED_CAST")
        return out as V
    }

    val keys: Set<K> get() = ref.load().keys
    val values: Collection<V> get() = ref.load().values
    val entries: Set<Map.Entry<K, V>> get() = ref.load().entries
    val size: Int get() = ref.load().size
    fun isEmpty(): Boolean = ref.load().isEmpty()
    fun clear() { ref.store(emptyMap()) }
    fun snapshot(): Map<K, V> = ref.load()
    fun removeIf(predicate: (Map.Entry<K, V>) -> Boolean) { update { m -> m.filterNot(predicate) } }
}

/** AtomicInteger 자리 */
class AtomicCounter(initial: Int = 0) {
    private val v = AtomicInt(initial)
    fun get(): Int = v.load()
    fun set(value: Int) = v.store(value)
    fun incrementAndGet(): Int = v.addAndFetch(1)
    fun decrementAndGet(): Int = v.addAndFetch(-1)
    fun getAndIncrement(): Int = v.fetchAndAdd(1)
    fun addAndGet(delta: Int): Int = v.addAndFetch(delta)
    fun compareAndSet(expect: Int, update: Int): Boolean = v.compareAndSet(expect, update)
}

/** 여러 일꾼이 함께 쓰는 글자 모음(ConcurrentHashMap.newKeySet() 자리) — 넣었으면 true */
class ConcurrentSet<T> {
    private val ref = AtomicReference(emptySet<T>())
    fun add(value: T): Boolean {
        while (true) {
            val now = ref.load()
            if (value in now) return false
            if (ref.compareAndSet(now, now + value)) return true
        }
    }
    fun remove(value: T): Boolean {
        while (true) {
            val now = ref.load()
            if (value !in now) return false
            if (ref.compareAndSet(now, now - value)) return true
        }
    }
    operator fun contains(value: T): Boolean = value in ref.load()
    fun clear() { ref.store(emptySet()) }
    fun snapshot(): Set<T> = ref.load()
    val size: Int get() = ref.load().size
}

/** UUID.randomUUID().toString() 자리(같은 꼴 — 소문자 8-4-4-4-12) */
@OptIn(kotlin.uuid.ExperimentalUuidApi::class)
fun randomUuid(): String = kotlin.uuid.Uuid.random().toString()
