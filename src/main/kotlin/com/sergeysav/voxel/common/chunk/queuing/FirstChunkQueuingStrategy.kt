package com.sergeysav.voxel.common.chunk.queuing

import com.sergeysav.voxel.common.chunk.Chunk
import mu.KotlinLogging

/**
 * @author sergeys
 *
 * @constructor Creates a new RandomLoadSelectionStrategy
 */
class FirstChunkQueuingStrategy<C : Chunk> : ChunkQueuingStrategy<C> {

    private val log = KotlinLogging.logger {  }
    private val chunks = LinkedHashSet<C>(100, 0.75f)

    init {
        log.trace { "Initializing ChunkQueueingStrategy" }
    }

    override fun currentSize(): Int = chunks.size

    override fun add(chunk: C) {
        chunks.add(chunk)
    }

    override fun remove(chunk: C) {
        chunks.remove(chunk)
    }

    override fun tryGetNext(): C? {
        return chunks.firstOrNull()
    }

    override fun clear() {
        chunks.clear()
    }
}