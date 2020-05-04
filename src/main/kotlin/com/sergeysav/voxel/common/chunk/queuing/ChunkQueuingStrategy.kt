package com.sergeysav.voxel.common.chunk.queuing

import com.sergeysav.voxel.common.chunk.Chunk

/**
 * @author sergeys
 */
interface ChunkQueuingStrategy<C : Chunk> {

    fun currentSize(): Int

    fun add(chunk: C)
    fun remove(chunk: C)

    fun tryGetNext(): C?

    fun clear()
}