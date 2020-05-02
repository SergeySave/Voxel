package com.sergeysav.voxel.common.world.loading.selection

import com.sergeysav.voxel.common.chunk.Chunk

/**
 * @author sergeys
 */
interface LoadSelectionStrategy<C : Chunk> {

    fun currentSize(): Int

    fun add(chunk: C)
    fun remove(chunk: C)

    fun tryGetNext(): C?

    fun clear()
}