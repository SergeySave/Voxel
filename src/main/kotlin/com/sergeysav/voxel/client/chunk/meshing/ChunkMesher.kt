package com.sergeysav.voxel.client.chunk.meshing

import com.sergeysav.voxel.client.chunk.ClientChunk
import com.sergeysav.voxel.common.world.World

/**
 * @author sergeys
 */
interface ChunkMesher {

    val free: Boolean
    val ready: Boolean
    val chunk: ClientChunk?

    fun generateMesh(world: World<*>, chunk: ClientChunk)
    fun applyMesh()
    fun cancel()
}