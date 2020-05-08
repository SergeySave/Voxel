package com.sergeysav.voxel.client.renderer.blockselection

import com.sergeysav.voxel.client.FrontendProxy
import com.sergeysav.voxel.client.camera.Camera
import com.sergeysav.voxel.client.gl.setUniform
import com.sergeysav.voxel.common.block.Block
import com.sergeysav.voxel.common.block.BlockPosition
import com.sergeysav.voxel.common.block.state.BlockState
import com.sergeysav.voxel.common.bound
import org.joml.Matrix4f
import org.lwjgl.opengl.GL20

/**
 * @author sergeys
 *
 * @constructor Creates a new HighlightVisibleSelectionRenderer
 */
class HighlightVisibleSelectionRenderer(
    private val r: Float,
    private val g: Float,
    private val b: Float,
    private val a: Float
) : BlockSelectionRenderer {

    private val model = Matrix4f()

    override fun initialize() {
    }

    override fun <B : Block<S>, S : BlockState> render(camera: Camera, blockPosition: BlockPosition, block: B, state: S) {
        FrontendProxy.cubeShader.bound {
            GL20.glUniform4f(FrontendProxy.cubeShader.getUniform("color"), r, g, b, a)
            camera.combined.setUniform(FrontendProxy.cubeShader.getUniform("uCamera"))
            model.identity()
            model.translate(blockPosition.x + 0.5f, blockPosition.y + 0.5f, blockPosition.z + 0.5f)
            model.scale(0.501f)
            model.setUniform(FrontendProxy.cubeShader.getUniform("uModel"))
            FrontendProxy.cubeMesh.draw()
        }
    }

    override fun cleanup() {
    }
}