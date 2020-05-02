package com.sergeysav.voxel.client.world.meshing.selection

import com.sergeysav.voxel.client.chunk.ClientChunk

/**
 * @author sergeys
 *
 * @constructor Creates a new FirstMeshSelectionStrategy
 */
class FirstMeshSelectionStrategy : MeshSelectionStrategy {

    private val chunks = mutableSetOf<ClientChunk>()

    override fun currentSize(): Int = chunks.size

    override fun add(chunk: ClientChunk) {
        chunks.add(chunk)
    }

    override fun remove(chunk: ClientChunk) {
        chunks.remove(chunk)
    }

    override fun tryGetNext(): ClientChunk? {
        return chunks.firstOrNull()
    }

    override fun clear() {
        chunks.clear()
    }
}