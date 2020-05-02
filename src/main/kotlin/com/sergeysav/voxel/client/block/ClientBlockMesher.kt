package com.sergeysav.voxel.client.block

import com.sergeysav.voxel.client.chunk.meshing.ChunkMesherCallback
import com.sergeysav.voxel.common.block.Block
import com.sergeysav.voxel.common.block.BlockMesher
import com.sergeysav.voxel.common.block.state.BlockState

/**
 * @author sergeys
 */
interface ClientBlockMesher<B : Block<out S>, S : BlockState> : BlockMesher<ChunkMesherCallback, B, S> {
}