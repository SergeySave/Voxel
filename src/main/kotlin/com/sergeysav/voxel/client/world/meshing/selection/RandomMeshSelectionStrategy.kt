package com.sergeysav.voxel.client.world.meshing.selection

import com.sergeysav.voxel.client.chunk.ClientChunk

/**
 * @author sergeys
 *
 * @constructor Creates a new FirstMeshSelectionStrategy
 */
class RandomMeshSelectionStrategy : MeshSelectionStrategy {

    private val chunks = mutableSetOf<ClientChunk>()

    override fun currentSize(): Int = chunks.size

    override fun add(chunk: ClientChunk) {
        chunks.add(chunk)
    }

    override fun remove(chunk: ClientChunk) {
        chunks.remove(chunk)
    }

    override fun tryGetNext(): ClientChunk? {
        return if (chunks.isNotEmpty()) chunks.random() else null
    }

    override fun clear() {
        chunks.clear()
    }
}