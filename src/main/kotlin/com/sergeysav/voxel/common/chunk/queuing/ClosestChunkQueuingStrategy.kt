package com.sergeysav.voxel.common.chunk.queuing

import com.sergeysav.voxel.common.block.BlockPosition
import com.sergeysav.voxel.common.chunk.Chunk
import com.sergeysav.voxel.common.math.square
import mu.KotlinLogging

/**
 * @author sergeys
 *
 * @constructor Creates a new ClosestChunkQueueingStrategy
 */
class ClosestChunkQueuingStrategy<C : Chunk>(
    private val center: BlockPosition
) : ChunkQueuingStrategy<C> {

    private val log = KotlinLogging.logger {  }
    private val chunks = ArrayList<C>(1000)
//    private val chunks = LinkedHashSet<C>(100, 0.75f)

    init {
        log.trace { "Initializing Chunk Queueing Strategy" }
    }

    override fun currentSize(): Int = chunks.size

    override fun add(chunk: C) {
        if (!chunks.contains(chunk)) {
            chunks.add(chunk)
        }
    }

    override fun remove(chunk: C) {
        chunks.remove(chunk)
    }


    private fun mapFunc(chunk: C): Double {
        return ((chunk.position.x + 0.5) - center.x / Chunk.SIZE.toDouble()).square() +
                ((chunk.position.y + 0.5) - center.y / Chunk.SIZE.toDouble()).square() +
                ((chunk.position.z + 0.5) - center.z / Chunk.SIZE.toDouble()).square()
    }


    override fun tryGetNext(): C? {
//        if (chunks.isEmpty()) return null
        var minVal = Double.POSITIVE_INFINITY
        var minChunk: C? = null
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