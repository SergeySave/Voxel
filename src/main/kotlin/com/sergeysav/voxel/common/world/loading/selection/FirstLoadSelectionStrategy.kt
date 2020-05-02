package com.sergeysav.voxel.common.world.loading.selection

import com.sergeysav.voxel.common.chunk.Chunk
import mu.KotlinLogging

/**
 * @author sergeys
 *
 * @constructor Creates a new RandomLoadSelectionStrategy
 */
class FirstLoadSelectionStrategy<C : Chunk> : LoadSelectionStrategy<C> {

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

    override fun tryGetNext(): C? {
        return chunks.firstOrNull()
    }

    override fun clear() {
        chunks.clear()
    }
}