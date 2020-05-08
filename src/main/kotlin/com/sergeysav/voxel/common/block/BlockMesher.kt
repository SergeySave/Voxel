package com.sergeysav.voxel.common.block

import com.sergeysav.voxel.common.block.state.BlockState

/**
 * @author sergeys
 */
interface BlockMesher<C, B : Block<out S>, S : BlockState> {

    fun addToMesh(opaqueMesher: C, translucentMesher: C, pos: BlockPosition, block: B, state: S)
}