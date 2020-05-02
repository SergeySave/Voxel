package com.sergeysav.voxel.client.world.meshing.selection

import com.sergeysav.voxel.client.chunk.ClientChunk
import com.sergeysav.voxel.common.block.BlockPosition
import com.sergeysav.voxel.common.chunk.Chunk
import com.sergeysav.voxel.common.math.square
import mu.KotlinLogging
import kotlin.random.Random

/**
 * @author sergeys
 *
 * @constructor Creates a new PriorityMeshSelectionStrategy
 */
class PriorityMeshSelectionStrategy(private val blockPos: BlockPosition) : MeshSelectionStrategy {

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
                300 * chunk.adjacentLoadedChunks.get() +
                70 * (((chunk.position.x + 0.5) - blockPos.x / Chunk.SIZE.toDouble()).square() +
                ((chunk.position.y + 0.5) - blockPos.y / Chunk.SIZE.toDouble()).square() +
                ((chunk.position.z + 0.5) - blockPos.z / Chunk.SIZE.toDouble()).square())
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