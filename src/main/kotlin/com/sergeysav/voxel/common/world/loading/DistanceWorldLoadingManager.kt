package com.sergeysav.voxel.common.world.loading

import com.sergeysav.voxel.common.chunk.Chunk
import com.sergeysav.voxel.common.chunk.ChunkPosition
import com.sergeysav.voxel.common.chunk.MutableChunkPosition
import com.sergeysav.voxel.common.world.World

/**
 * @author sergeys
 *
 * @constructor Creates a new DistanceWorldLoadingManager
 */
class DistanceWorldLoadingManager : WorldLoadingManager<Chunk, World<Chunk>> {

    private val baseCoords = MutableChunkPosition()
    private val strategy = DistanceWorldLoadingStrategy(baseCoords, 0)

    fun setLoadingDistance(chunks: Int) {
        strategy.distance = chunks
    }

    fun setCenter(chunkPosition: ChunkPosition) {
        baseCoords.set(chunkPosition)
    }

    override fun updateWorldLoading(
        world: World<Chunk>,
        chunks: Iterable<Chunk>,
        loadCallback: (ChunkPosition) -> Unit,
        unloadCallback: (ChunkPosition) -> Unit
    ) {
        strategy.update()

        chunks.map(Chunk::position)
            .filterNot(strategy::shouldStayLoaded)
            .forEach(unloadCallback)

        strategy.requestLoads(world, chunks, loadCallback)
    }

    override fun cleanupWorldLoading() {
        strategy.cleanup()
    }
}