package com.sergeysav.voxel.common.world.loading

import com.sergeysav.voxel.common.chunk.Chunk
import com.sergeysav.voxel.common.chunk.ChunkPosition
import com.sergeysav.voxel.common.world.World

/**
 * @author sergeys
 */
interface WorldLoadingStrategy {

    fun update()

    fun shouldStayLoaded(chunkPosition: ChunkPosition): Boolean

    fun requestLoads(world: World<Chunk>,
                     chunks: Iterable<Chunk>,
                     loadCallback: (ChunkPosition) -> Unit)

    fun cleanup()
}