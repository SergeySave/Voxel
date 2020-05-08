package com.sergeysav.voxel.client.renderer.blockselection

import com.sergeysav.voxel.client.camera.Camera
import com.sergeysav.voxel.common.block.Block
import com.sergeysav.voxel.common.block.BlockPosition
import com.sergeysav.voxel.common.block.state.BlockState
import org.lwjgl.opengl.GL11

/**
 * @author sergeys
 *
 * @constructor Creates a new BoundingBoxFullSelectionRenderer
 */
class BoundingBoxFullSelectionRenderer(
    r: Float,
    g: Float,
    b: Float,
    a: Float,
    width: Float
) : BlockSelectionRenderer {

    private val boundingBoxVisibleSelectionRenderer = BoundingBoxVisibleSelectionRenderer(r, g, b, a, width)

    override fun initialize() {
        boundingBoxVisibleSelectionRenderer.initialize()
    }

    override fun <B : Block<S>, S : BlockState> render(camera: Camera, blockPosition: BlockPosition, block: B, state: S) {
        GL11.glDisable(GL11.GL_DEPTH_TEST)
        boundingBoxVisibleSelectionRenderer.render(camera, blockPosition, block, state)
        GL11.glEnable(GL11.GL_DEPTH_TEST)
    }

    override fun cleanup() {
        boundingBoxVisibleSelectionRenderer.cleanup()
    }
}