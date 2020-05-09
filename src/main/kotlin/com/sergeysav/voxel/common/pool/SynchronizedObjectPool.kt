package com.sergeysav.voxel.common.pool

/**
 * @author sergeys
 *
 * @constructor Creates a new SimpleObjectPool
 */
class SynchronizedObjectPool<T>(val builder: ()->T, initialCapacity: Int) : ObjectPool<T> {
    private val pool = ArrayList<T>(initialCapacity)

    init {
        repeat(initialCapacity) {
            pool.add(builder())
        }
    }

    override fun get(): T = synchronized(pool) {
        if (pool.isEmpty()) {
            builder()
        } else {
            pool.removeAt(pool.lastIndex)
        }
    }

    override fun put(item: T) {
        synchronized(pool) {
            pool.add(item)
        }
    }

    override fun cleanup() {
    }
}