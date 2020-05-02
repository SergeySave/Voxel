package com.sergeysav.voxel.common.world.chunks

import com.sergeysav.voxel.common.chunk.Chunk
import com.sergeysav.voxel.common.world.generator.ChunkGenerator

/**
 * @author sergeys
 *
 * @constructor Creates a new SimpleChunkManager
 */
class SimpleChunkManager<C : Chunk>(private val chunkGenerator: ChunkGenerator<in Chunk>) : ChunkManager<C> {

    private lateinit var callback: (C) -> Unit
    private val chunks = mutableSetOf<C>()

    override fun initialize(callback: (C) -> Unit) {
        this.callback = callback
    }

    override fun requestLoad(chunk: C) {
        chunkGenerator.generateChunk(chunk)
        chunks.add(chunk)
    }

    override fun requestUnload(chunk: C) {
    }

    override fun update() {
        chunks.forEach { callback(it) }
        chunks.clear()
    }

    override fun cleanup() {
    }
}