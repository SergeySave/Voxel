package com.sergeysav.voxel.common.pool

import java.util.Collections

/**
 * @author sergeys
 *
 * @constructor Creates a new SimpleObjectPool
 */
class SynchronizedObjectPool<T>(val builder: ()->T, initialCapacity: Int) : ObjectPool<T> {
    private val pool = Collections.synchronizedList(ArrayList<T>(initialCapacity))

    init {
        repeat(initialCapacity) {
            pool.add(builder())
        }
    }

    override fun get(): T = if (pool.isEmpty()) {
        builder()
    } else {
        pool.removeAt(pool.lastIndex)
    }

    override fun put(item: T) {
        pool.add(item)
    }

    override fun cleanup() {
    }
}