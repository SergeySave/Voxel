package com.sergeysav.voxel.common.world.chunks

import com.sergeysav.voxel.common.chunk.Chunk
import com.sergeysav.voxel.common.world.generator.ChunkGenerator

/**
 * @author sergeys
 *
 * @constructor Creates a new SimpleChunkManager
 */
@Deprecated("Does not support chunk save/load")
class SimpleChunkManager<C : Chunk>(private val chunkGenerator: ChunkGenerator<in Chunk>) : ChunkManager<C> {

    private lateinit var releaseCallback: (C) -> Unit
    private lateinit var callback: (C) -> Unit
    private val chunks = mutableSetOf<C>()
    private val toRelease = mutableSetOf<C>()

    override fun initialize(chunkReleaseCallback: (C) -> Unit, callback: (C) -> Unit) {
        this.releaseCallback = chunkReleaseCallback
        this.callback = callback
    }

    override fun requestLoad(chunk: C) {
        chunkGenerator.generateChunk(chunk)
        chunks.add(chunk)
    }

    override fun requestUnload(chunk: C) {
        toRelease.add(chunk)
    }

    override fun notifyChunkDirty(chunk: C) {
    }

    override fun update() {
        chunks.forEach { callback(it) }
        chunks.clear()
        toRelease.forEach(releaseCallback)
        toRelease.clear()
    }

    override fun cleanup() {
    }
}