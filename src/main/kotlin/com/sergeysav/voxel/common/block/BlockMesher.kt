package com.sergeysav.voxel.common.block

import com.sergeysav.voxel.common.block.state.BlockState
import com.sergeysav.voxel.common.chunk.Chunk
import com.sergeysav.voxel.common.world.World

/**
 * @author sergeys
 */
interface BlockMesher<C, B : Block<out S>, S : BlockState> {

    fun addToMesh(
        opaqueMesher: C,
        translucentMesher: C,
        pos: BlockPosition,
        block: B,
        state: S,
        world: World<Chunk>
    )
}