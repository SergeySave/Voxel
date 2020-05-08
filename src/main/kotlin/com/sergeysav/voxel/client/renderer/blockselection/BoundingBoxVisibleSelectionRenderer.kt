package com.sergeysav.voxel.client.renderer.blockselection

import com.sergeysav.voxel.client.FrontendProxy
import com.sergeysav.voxel.client.camera.Camera
import com.sergeysav.voxel.client.gl.setUniform
import com.sergeysav.voxel.common.block.Block
import com.sergeysav.voxel.common.block.BlockPosition
import com.sergeysav.voxel.common.block.state.BlockState
import com.sergeysav.voxel.common.bound
import com.sergeysav.voxel.common.math.square
import org.joml.Matrix4f
import org.lwjgl.opengl.GL20
import kotlin.math.sqrt

/**
 * @author sergeys
 *
 * @constructor Creates a new BoundingBoxVisibleSelectionRenderer
 */
class BoundingBoxVisibleSelectionRenderer(
    private val r: Float,
    private val g: Float,
    private val b: Float,
    private val a: Float,
    width: Float
) : BlockSelectionRenderer {

    private val model = Matrix4f()
    private val width = width / 2f

    override fun initialize() {
    }

    override fun <B : Block<S>, S : BlockState> render(camera: Camera, blockPosition: BlockPosition, block: B, state: S) {

        val distance = sqrt((blockPosition.x + 0.5 - camera.position.x()).square() +
                (blockPosition.y + 0.5 - camera.position.y()).square() +
                (blockPosition.z + 0.5 - camera.position.z()).square()).toFloat()

        FrontendProxy.cubeShader.bound {
            GL20.glUniform4f(FrontendProxy.cubeShader.getUniform("color"), r, g, b, a)
            camera.combined.setUniform(FrontendProxy.cubeShader.getUniform("uCamera"))

            for (i in edges.indices) {
                val edge = edges[i]
                model.identity()
                model.translate(blockPosition.x + edge[0], blockPosition.y + edge[1], blockPosition.z + edge[2])
                model.scale(width * distance + edge[3], width * distance + edge[4], width * distance + edge[5])
                model.setUniform(FrontendProxy.cubeShader.getUniform("uModel"))
                FrontendProxy.cubeMesh.draw()
            }
        }
    }

    override fun cleanup() {
    }

    companion object {
        private val edges = arrayOf(
            floatArrayOf(0.5f, 0f, 0f, 0.5f, 0f, 0f),
            floatArrayOf(0.5f, 0f, 1f, 0.5f, 0f, 0f),
            floatArrayOf(0.5f, 1f, 0f, 0.5f, 0f, 0f),
            floatArrayOf(0.5f, 1f, 1f, 0.5f, 0f, 0f),

            floatArrayOf(0f, 0.5f, 0f, 0f, 0.5f, 0f),
            floatArrayOf(0f, 0.5f, 1f, 0f, 0.5f, 0f),
            floatArrayOf(1f, 0.5f, 0f, 0f, 0.5f, 0f),
            floatArrayOf(1f, 0.5f, 1f, 0f, 0.5f, 0f),

            floatArrayOf(0f, 0f, 0.5f, 0f, 0f, 0.5f),
            floatArrayOf(0f, 1f, 0.5f, 0f, 0f, 0.5f),
            floatArrayOf(1f, 0f, 0.5f, 0f, 0f, 0.5f),
            floatArrayOf(1f, 1f, 0.5f, 0f, 0f, 0.5f)
        )
    }
}