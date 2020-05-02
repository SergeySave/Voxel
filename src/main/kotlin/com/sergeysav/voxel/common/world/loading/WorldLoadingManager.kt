package com.sergeysav.voxel.common.world.loading

import com.sergeysav.voxel.common.chunk.Chunk
import com.sergeysav.voxel.common.chunk.ChunkPosition
import com.sergeysav.voxel.common.world.World

/**
 * @author sergeys
 */
interface WorldLoadingManager<C : Chunk, W : World<C>> {

    fun updateWorldLoading(world: W, chunks: Iterable<C>, loadCallback: (ChunkPosition)->Unit, unloadCallback: (ChunkPosition)->Unit)
    fun cleanupWorldLoading()
}