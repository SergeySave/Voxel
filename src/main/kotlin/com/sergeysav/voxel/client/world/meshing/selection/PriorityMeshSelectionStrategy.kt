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
    private val chunks = ArrayList<ClientChunk>(1000)

    init {
        log.trace { "Initializing Mesh Selection Strategy" }
    }

    override fun currentSize(): Int = chunks.size

    override fun add(chunk: ClientChunk) {
        if (!chunks.contains(chunk)) {
            chunks.add(chunk)
        }
    }

    override fun remove(chunk: ClientChunk) {
        chunks.remove(chunk)
    }

    private fun mapFunc(chunk: ClientChunk): Double {
        return 1000 * chunk.adjacentLoadedChunks.get() +
                70 * (((chunk.position.x + 0.5) - blockPos.x / Chunk.SIZE.toDouble()).square() +
                ((chunk.position.y + 0.5) - blockPos.y / Chunk.SIZE.toDouble()).square() +
                ((chunk.position.z + 0.5) - blockPos.z / Chunk.SIZE.toDouble()).square())
    }

    override fun tryGetNext(): ClientChunk? {
        var minVal = Double.POSITIVE_INFINITY
        var minChunk: ClientChunk? = null
        for (i in 0 until chunks.size) {
            val chunkVal = mapFunc(chunks[i])
            if (chunkVal < minVal) {
                minChunk = chunks[i]
                minVal = chunkVal
            }
        }
        return minChunk
    }

    override fun clear() {
        chunks.clear()
    }
}