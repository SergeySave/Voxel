package com.sergeysav.voxel.common.world.loading

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
class DistanceWorldLoadingStrategy(val baseCoords: ChunkPosition, var distance: Int) : WorldLoadingStrategy {

    private val log = KotlinLogging.logger {  }
    private val chunkPosition = MutableChunkPosition()

    init {
        log.trace { "Initializing World Loading Strategy" }
    }

    override fun update() {
    }

    override fun shouldStayLoaded(chunkPosition: ChunkPosition): Boolean {
        return (chunkPosition.x - baseCoords.x).square() +
                (chunkPosition.y - baseCoords.y).square() +
                (chunkPosition.z - baseCoords.z).square() <= distance.square()
    }

    override fun requestLoads(world: World<Chunk>, chunks: Iterable<Chunk>, loadCallback: (ChunkPosition) -> Unit) {
        for (i in -distance..distance) {
            for (j in -distance..distance) {
                for (k in -distance..distance) {
                    if (i.square() + j.square() + k.square() <= distance.square()) {
                        chunkPosition.x = i + baseCoords.x
                        chunkPosition.y = j + baseCoords.y
                        chunkPosition.z = k + baseCoords.z
                        loadCallback(chunkPosition)
                    }
                }
            }
        }
    }

    override fun cleanup() {
    }
}