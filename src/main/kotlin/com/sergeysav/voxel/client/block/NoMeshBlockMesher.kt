package com.sergeysav.voxel.client.block

import com.sergeysav.voxel.client.chunk.meshing.ChunkMesherCallback
import com.sergeysav.voxel.common.block.Block
import com.sergeysav.voxel.common.block.BlockPosition
import com.sergeysav.voxel.common.block.state.BlockState

/**
 * @author sergeys
 */
object NoMeshBlockMesher : ClientBlockMesher<Block<out BlockState>, BlockState> {

    override val opaque: Boolean = false

    override fun addToMesh(chunkMesher: ChunkMesherCallback, pos: BlockPosition, block: Block<out BlockState>, state: BlockState) { }
}