package com.sergeysav.voxel.common.pool

/**
 * @author sergeys
 */
interface ObjectPool<T> {
    fun get(): T
    fun put(item: T)

    fun cleanup()
}

inline fun <T, R>  ObjectPool<T>.with(inner: (T)->R): R {
    val item = get()
    try {
        return inner(item)
    } finally {
        put(item)
    }
}
