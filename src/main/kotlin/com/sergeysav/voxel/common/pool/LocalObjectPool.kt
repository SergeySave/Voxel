package com.sergeysav.voxel.common.pool

/**
 * @author sergeys
 *
 * @constructor Creates a new LocalObjectPool
 */
class LocalObjectPool<T>(builder: ()->T, initialCapacity: Int) : ObjectPool<T> {

    private val threadLocal = ThreadLocal.withInitial { SimpleObjectPool(builder, initialCapacity) }

    override fun get(): T {
        return threadLocal.get().get()
    }

    override fun put(item: T) {
        threadLocal.get().put(item)
    }

    override fun cleanup() {
        threadLocal.get().cleanup()
    }
}