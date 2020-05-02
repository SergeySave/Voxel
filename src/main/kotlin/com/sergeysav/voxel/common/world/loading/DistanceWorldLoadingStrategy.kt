package com.sergeysav.voxel.common.world.loading

import com.sergeysav.voxel.common.block.BlockPosition
import com.sergeysav.voxel.common.chunk.Chunk
import com.sergeysav.voxel.common.chunk.ChunkPosition
import com.sergeysav.voxel.common.chunk.MutableChunkPosition
import com.sergeysav.voxel.common.math.square
import com.sergeysav.voxel.common.world.World
import com.sergeysav.voxel.common.world.loading.WorldLoadingStrategy
import mu.KotlinLogging

/**
 * @author sergeys
 *
 * @constructor Creates a new DistanceWorldLoadingStrategy
 */
class DistanceWorldLoadingStrategy(val baseCoords: BlockPosition, var distance: Int) : WorldLoadingStrategy {

    private val log = KotlinLogging.logger {  }
    private val chunkPosition = MutableChunkPosition()

    init {
        log.trace { "Initializing World Loading Strategy" }
    }

    override fun update() {
    }

    override fun shouldStayLoaded(chunkPosition: ChunkPosition): Boolean {
        return ((chunkPosition.x + 0.5) - baseCoords.x / Chunk.SIZE.toDouble()).square() +
                ((chunkPosition.y + 0.5) - baseCoords.y / Chunk.SIZE.toDouble()).square() +
                ((chunkPosition.z + 0.5) - baseCoords.z / Chunk.SIZE.toDouble()).square() <= distance.square()
    }

    override fun requestLoads(world: World<Chunk>, chunks: Iterable<Chunk>, loadCallback: (ChunkPosition) -> Unit) {
        for (i in -distance..distance) {
            for (j in -distance..distance) {
                for (k in -distance..distance) {
                    chunkPosition.setToChunkOf(baseCoords)
                    chunkPosition.x += i
                    chunkPosition.y += j
                    chunkPosition.z += k
                    if (shouldStayLoaded(chunkPosition)) {
                        loadCallback(chunkPosition)
                    }
                }
            }
        }
    }

    override fun cleanup() {
    }
}