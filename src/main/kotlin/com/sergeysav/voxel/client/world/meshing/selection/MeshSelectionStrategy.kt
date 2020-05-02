package com.sergeysav.voxel.client.world.meshing.selection

import com.sergeysav.voxel.client.chunk.ClientChunk

/**
 * @author sergeys
 */
interface MeshSelectionStrategy {

    fun add(chunk: ClientChunk)
    fun remove(chunk: ClientChunk)

    fun tryGetNext(): ClientChunk?

    fun clear()
}