package com.sergeysav.voxel.common.block

import com.sergeysav.voxel.common.block.state.BlockState
import com.sergeysav.voxel.common.data.Direction
import com.sergeysav.voxel.common.world.World

/**
 * @author sergeys
 */
interface Block<T : BlockState> {

    /**
     * This unlocalized name is used for storing this block in a file.
     *
     * This must be lowercase.
     */
    val unlocalizedName: String

    /**
     * Get the block state from its simple value
     *
     * @param value the simple value byte
     *
     * @return the state
     */
    fun getStateFromSimpleValue(value: Byte): T

    /**
     * Get the simple value for the block's state
     *
     * @param state the state of the block
     *
     * @return the simple value
     */
    fun getSimpleValueForState(state: T): Byte

    /**
     * Called right before a block is placed to perform any to this block's upcoming state.
     * Note: this is only called when the block is in a loaded chunk.
     * Note: the change will be ignored if the state returned is the same as the world's current state
     *
     * @param state the state of the block
     * @param world the world
     * @param blockPosition the position of the block that was placed (Note: this is mutable to support the calculations
     * of [onBlockPlaced], however its value may be changed after this method returns. Thus it should not be saved.)
     *
     * @return the state that should be set
     */
    fun onBlockPlaced(state: T, world: World<*>, blockPosition: MutableBlockPosition): T = state

    /**
     * Called right after an adjacent block was changed.
     * Note: this is only called when the block is in a loaded chunk.
     *
     * @param S the state type of the adjacent changed block
     * @param state the state of this block
     * @param world the world
     * @param blockPosition the position of this block (Note: this is mutable to support the calculations of
     * [onAdjacentBlockChanged], however its value may be changed after this method returns. Thus it should not be saved.)
     * @param d the direction pointing towards the changed block
     * @param adjacentBlock the adjacent block that was changed
     * @param adjacentState the state of the adjacent block that was changed
     *
     * @return the state that should be set
     */
    fun <S : BlockState> onAdjacentBlockChanged(
        state: T,
        world: World<*>,
        blockPosition: MutableBlockPosition,
        d: Direction,
        adjacentBlock: Block<S>,
        adjacentState: S
    ): T = state
}
