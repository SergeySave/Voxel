package com.sergeysav.voxel.client.world.meshing.selection

import com.sergeysav.voxel.client.chunk.ClientChunk
import com.sergeysav.voxel.common.chunk.queuing.ChunkQueuingStrategy

/**
 * @author sergeys
 */
typealias MeshSelectionStrategy = ChunkQueuingStrategy<ClientChunk>
