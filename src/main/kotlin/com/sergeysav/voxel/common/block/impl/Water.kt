package com.sergeysav.voxel.common.block.impl

import com.sergeysav.voxel.common.block.Block
import com.sergeysav.voxel.common.block.MutableBlockPosition
import com.sergeysav.voxel.common.block.state.BlockState
import com.sergeysav.voxel.common.data.Direction
import com.sergeysav.voxel.common.world.World

/**
 * @author sergeys
 */
object Water : BaseBlock<Water.State>("water") {

    override fun getStateFromSimpleValue(value: Byte): State = State.values()[value.toInt() and 0xFF]

    override fun getSimpleValueForState(state: State): Byte = state.ordinal.toByte()

    override fun onBlockPlaced(state: State, world: World<*>, blockPosition: MutableBlockPosition): State {
        blockPosition += Direction.Up
        // If the block above is is water, we should be full
        return if (world.getBlock(blockPosition) == Water) {
            State.FULL_BLOCK
        } else {
            super.onBlockPlaced(state, world, blockPosition)
        }
    }

    override fun <S : BlockState> onAdjacentBlockChanged(
        state: State,
        world: World<*>,
        blockPosition: MutableBlockPosition,
        d: Direction,
        adjacentBlock: Block<S>,
        adjacentState: S
    ): State {
        return if (d == Direction.Up) {
            if (adjacentBlock == Water) {
                State.FULL_BLOCK
            } else {
                State.NORMAL
            }
        } else {
            super.onAdjacentBlockChanged(state, world, blockPosition, d, adjacentBlock, adjacentState)
        }
    }

    enum class State : BlockState {
        FULL_BLOCK,
        NORMAL
    }
}
