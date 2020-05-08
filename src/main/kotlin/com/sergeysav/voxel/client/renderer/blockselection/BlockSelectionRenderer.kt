package com.sergeysav.voxel.client.renderer.blockselection

import com.sergeysav.voxel.client.camera.Camera
import com.sergeysav.voxel.common.block.Block
import com.sergeysav.voxel.common.block.BlockPosition
import com.sergeysav.voxel.common.block.state.BlockState

/**
 * @author sergeys
 */
interface BlockSelectionRenderer {
    fun initialize()

    fun <B : Block<S>, S : BlockState> render(camera: Camera, blockPosition: BlockPosition, block: B, state: S)

    fun cleanup()
}