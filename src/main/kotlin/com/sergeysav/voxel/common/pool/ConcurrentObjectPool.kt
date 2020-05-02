package com.sergeysav.voxel.common.pool

import java.util.concurrent.ArrayBlockingQueue

/**
 * @author sergeys
 *
 * @constructor Creates a new SimpleObjectPool
 */
class ConcurrentObjectPool<T>(val builder: ()->T, capacity: Int, pregenAmount: Int = capacity) : ObjectPool<T> {
    private val pool = ArrayBlockingQueue<T>(capacity)

    init {
        repeat(capacity) {
            pool.put(builder())
        }
    }

    override fun get(): T = pool.poll() ?: builder()

    override fun put(item: T) {
        pool.offer(item)
    }

    override fun cleanup() {
    }
}