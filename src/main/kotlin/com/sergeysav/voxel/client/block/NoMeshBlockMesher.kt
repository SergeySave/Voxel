package com.sergeysav.voxel.client.block

import com.sergeysav.voxel.client.chunk.meshing.ChunkMesherCallback
import com.sergeysav.voxel.common.block.Block
import com.sergeysav.voxel.common.block.BlockPosition
import com.sergeysav.voxel.common.block.state.BlockState
import com.sergeysav.voxel.common.chunk.Chunk
import com.sergeysav.voxel.common.data.Direction
import com.sergeysav.voxel.common.world.World

/**
 * @author sergeys
 */
object NoMeshBlockMesher : ClientBlockMesher<Block<out BlockState>, BlockState> {

    override fun addToMesh(
        opaqueMesher: ChunkMesherCallback,
        translucentMesher: ChunkMesherCallback,
        pos: BlockPosition,
        block: Block<out BlockState>,
        state: BlockState,
        world: World<Chunk>
    ) { }

    override fun shouldOpaqueAdjacentHideFace(side: Direction): Boolean = false

    override fun shouldTransparentAdjacentHideFace(side: Direction): Boolean = false
}