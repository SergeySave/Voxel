package com.sergeysav.voxel.common.world.chunks

import com.sergeysav.voxel.common.chunk.Chunk

/**
 * @author sergeys
 */
interface ChunkManager<C : Chunk> {

    fun initialize(chunkReleaseCallback: (C) -> Unit, callback: (C)->Unit)

    fun requestLoad(chunk: C)
    fun requestUnload(chunk: C)

    fun update()

    fun cleanup()
}