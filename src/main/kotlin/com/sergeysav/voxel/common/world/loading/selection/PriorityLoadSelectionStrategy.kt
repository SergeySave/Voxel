package com.sergeysav.voxel.common.world.loading.selection

import com.sergeysav.voxel.client.chunk.ClientChunk
import com.sergeysav.voxel.common.block.BlockPosition
import com.sergeysav.voxel.common.chunk.Chunk
import com.sergeysav.voxel.common.chunk.ChunkPosition
import com.sergeysav.voxel.common.math.square
import mu.KotlinLogging
import kotlin.random.Random

/**
 * @author sergeys
 *
 * @constructor Creates a new RandomLoadSelectionStrategy
 */
class PriorityLoadSelectionStrategy<C : Chunk>(private val blockPos: BlockPosition) : LoadSelectionStrategy<C> {

    private val log = KotlinLogging.logger {  }
    private val chunks = mutableSetOf<C>()

    init {
        log.trace { "Initializing World Load Selection Strategy" }
    }

    override fun currentSize(): Int = chunks.size

    override fun add(chunk: C) {
        chunks.add(chunk)
    }

    override fun remove(chunk: C) {
        chunks.remove(chunk)
    }


    private fun mapFunc(chunk: C): Double {
        return ((chunk.position.x + 0.5) - blockPos.x / Chunk.SIZE.toDouble()).square() +
                ((chunk.position.y + 0.5) - blockPos.y / Chunk.SIZE.toDouble()).square() +
                ((chunk.position.z + 0.5) - blockPos.z / Chunk.SIZE.toDouble()).square()
    }


    override fun tryGetNext(): C? {
        return if (chunks.isNotEmpty()) chunks.minBy(::mapFunc) else null
    }

    override fun clear() {
        chunks.clear()
    }
}