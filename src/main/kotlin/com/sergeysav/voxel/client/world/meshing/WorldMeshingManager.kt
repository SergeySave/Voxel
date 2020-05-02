package com.sergeysav.voxel.client.world.meshing

import com.sergeysav.voxel.client.chunk.ClientChunk
import com.sergeysav.voxel.common.world.World

/**
 * @author sergeys
 */
interface WorldMeshingManager<in W : World<ClientChunk>> {

    fun getQueueSize(): Int

    fun notifyMeshDirty(chunk: ClientChunk)

    fun notifyMeshUnneeded(chunk: ClientChunk)

    fun updateWorldMeshing(world: W)

    fun cleanupWorldMeshing()
}