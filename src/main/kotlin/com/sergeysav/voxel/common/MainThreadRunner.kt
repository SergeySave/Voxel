package com.sergeysav.voxel.common

import java.util.concurrent.CompletableFuture

/**
 * @author sergeys
 */
interface MainThreadRunner {
    fun <T> runOnMainThread(inner: ()->T): CompletableFuture<T>
}