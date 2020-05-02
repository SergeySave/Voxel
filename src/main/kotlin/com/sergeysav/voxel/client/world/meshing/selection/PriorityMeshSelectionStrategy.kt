package com.sergeysav.voxel.client.world.meshing.selection

import com.sergeysav.voxel.client.chunk.ClientChunk
import com.sergeysav.voxel.common.chunk.ChunkPosition
import com.sergeysav.voxel.common.chunk.MutableChunkPosition
import com.sergeysav.voxel.common.math.square
import mu.KotlinLogging
import kotlin.random.Random

/**
 * @author sergeys
 *
 * @constructor Creates a new PriorityMeshSelectionStrategy
 */
class PriorityMeshSelectionStrategy(private val chunkPosition: ChunkPosition) : MeshSelectionStrategy {

    private val log = KotlinLogging.logger {  }
    private val chunks = mutableSetOf<ClientChunk>()

    init {
        log.trace { "Initializing Mesh Selection Strategy" }
    }

    override fun add(chunk: ClientChunk) {
        chunks.add(chunk)
    }

    override fun remove(chunk: ClientChunk) {
        chunks.remove(chunk)
    }

    private fun mapFunc(chunk: ClientChunk): Double {
        return (if (chunk.mesh == null) 0 else 1000) +
                50 * Random.nextDouble() -
                100 * chunk.adjacentLoadedChunks.get() +
                50 * ((chunkPosition.x - chunk.position.x).square() +
                (chunkPosition.y - chunk.position.y).square() +
                (chunkPosition.z - chunk.position.z).square())
    }

    override fun tryGetNext(): ClientChunk? {
        return if (chunks.isNotEmpty()) {
            chunks.minBy(::mapFunc)
        } else null
    }

    override fun clear() {
        chunks.clear()
    }
}