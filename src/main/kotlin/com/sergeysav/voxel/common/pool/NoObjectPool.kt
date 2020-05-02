package com.sergeysav.voxel.common.pool

/**
 * @author sergeys
 *
 * @constructor Creates a new SimpleObjectPool
 */
class NoObjectPool<T>(val builder: ()->T) : ObjectPool<T> {

    override fun get(): T = builder()

    override fun put(item: T) = Unit

    override fun cleanup() = Unit
}