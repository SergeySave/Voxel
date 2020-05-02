package com.sergeysav.voxel.common

import com.sergeysav.voxel.common.block.Block
import com.sergeysav.voxel.common.block.impl.Air
import com.sergeysav.voxel.common.block.impl.Dirt
import com.sergeysav.voxel.common.block.impl.Grass
import com.sergeysav.voxel.common.block.impl.Stone
import com.sergeysav.voxel.common.block.impl.Test

/**
 * @author sergeys
 */
abstract class CommonProxy : SidedProxy {
    final override val blocks = mutableListOf<Block<*>>()

    init {
        blocks.add(Air)
        blocks.add(Dirt)
        blocks.add(Stone)
        blocks.add(Grass)
        blocks.add(Test)
    }
}