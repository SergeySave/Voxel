package com.sergeysav.voxel.client.renderer.blockselection

import com.sergeysav.voxel.client.camera.Camera
import com.sergeysav.voxel.common.block.Block
import com.sergeysav.voxel.common.block.BlockPosition
import com.sergeysav.voxel.common.block.state.BlockState
import org.lwjgl.opengl.GL11

/**
 * @author sergeys
 *
 * @constructor Creates a new HighlightFullSelectionRenderer
 */
class HighlightFullSelectionRenderer(
    r: Float,
    g: Float,
    b: Float,
    a: Float
) : BlockSelectionRenderer {

    private val highlightVisibleSelectionRenderer = HighlightVisibleSelectionRenderer(r, g, b, a)

    override fun initialize() {
        highlightVisibleSelectionRenderer.initialize()
    }

    override fun <B : Block<S>, S : BlockState> render(camera: Camera, blockPosition: BlockPosition, block: B, state: S) {
        GL11.glDisable(GL11.GL_DEPTH_TEST)
        highlightVisibleSelectionRenderer.render(camera, blockPosition, block, state)
        GL11.glEnable(GL11.GL_DEPTH_TEST)
    }

    override fun cleanup() {
        highlightVisibleSelectionRenderer.cleanup()
    }
}