package com.sergeysav.voxel.client.renderer

import com.sergeysav.voxel.client.FrontendProxy
import com.sergeysav.voxel.client.camera.Camera
import com.sergeysav.voxel.client.camera.CameraAABBChecker
import com.sergeysav.voxel.client.chunk.ClientChunk
import com.sergeysav.voxel.client.gl.bound
import com.sergeysav.voxel.client.gl.setUniform
import com.sergeysav.voxel.common.bound
import com.sergeysav.voxel.common.chunk.Chunk
import org.joml.Matrix4f
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL20

/**
 * @author sergeys
 *
 * @constructor Creates a new DefaultClientWorldRenderer
 */
class DefaultClientWorldRenderer : ClientWorldRenderer {

    private val model = Matrix4f()
    private val cameraAABBChecker = CameraAABBChecker()
    private val visibleChunks = ArrayList<ClientChunk>(4096)

    override fun initialize() {
        GL11.glEnable(GL11.GL_DEPTH_TEST)
        GL11.glDepthFunc(GL11.GL_LESS)

        GL11.glEnable(GL11.GL_CULL_FACE)
        GL11.glCullFace(GL11.GL_BACK)

        GL11.glEnable(GL11.GL_BLEND)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
    }

    override fun render(camera: Camera, chunks: List<ClientChunk>, width: Int, height: Int) {
        cameraAABBChecker.update(camera)

        visibleChunks.clear()
        for (chunk in chunks) {
            if (chunk.loaded && (chunk.opaqueMesh != null || chunk.translucentMesh != null)) {
                val x = chunk.position.x * Chunk.SIZE.toFloat()
                val y = chunk.position.y * Chunk.SIZE.toFloat()
                val z = chunk.position.z * Chunk.SIZE.toFloat()
                if (cameraAABBChecker.isAABBinCamera(x, y, z, Chunk.SIZE.toFloat(), Chunk.SIZE.toFloat(), Chunk.SIZE.toFloat())) {
                    visibleChunks.add(chunk)
                }
            }
        }

        FrontendProxy.assetData.bound(0) {
            FrontendProxy.textureAtlas.bound(1) {
                FrontendProxy.voxelShader.bound {
                    camera.combined.setUniform(FrontendProxy.voxelShader.getUniform("uCamera"))
                    GL20.glUniform1i(FrontendProxy.voxelShader.getUniform("assetData"), 0)
                    GL20.glUniform1i(FrontendProxy.voxelShader.getUniform("atlasPage0"), 1)

                    // Sort chunks in based on the direction that the camera is facing (dot product)
                    visibleChunks.sortBy { (it.position.x + 0.5) * Chunk.SIZE * camera.direction.x() +
                            (it.position.y + 0.5) * Chunk.SIZE * camera.direction.y() +
                            (it.position.z + 0.5) * Chunk.SIZE * camera.direction.z() }

                    for (chunk in visibleChunks) {
                        val mesh = chunk.opaqueMesh
                        if (mesh != null && mesh.indexCount > 0) {
                            model.identity()
                            val x = chunk.position.x * Chunk.SIZE.toFloat()
                            val y = chunk.position.y * Chunk.SIZE.toFloat()
                            val z = chunk.position.z * Chunk.SIZE.toFloat()
                            model.translate(x, y, z)
                            model.setUniform(FrontendProxy.voxelShader.getUniform("uModel"))
                            mesh.draw()
                        }
                    }
                }

                FrontendProxy.translucentVoxelShader.bound {
                    camera.combined.setUniform(FrontendProxy.translucentVoxelShader.getUniform("uCamera"))
                    GL20.glUniform1i(FrontendProxy.translucentVoxelShader.getUniform("assetData"), 0)
                    GL20.glUniform1i(FrontendProxy.translucentVoxelShader.getUniform("atlasPage0"), 1)

                    // Reverse Sort (so that further things try to render first)
                    visibleChunks.reverse()

                    GL11.glDepthMask(false)
                    for (chunk in visibleChunks) {
                        val mesh = chunk.translucentMesh
                        if (mesh != null && mesh.indexCount > 0) {
                            model.identity()
                            val x = chunk.position.x * Chunk.SIZE.toFloat()
                            val y = chunk.position.y * Chunk.SIZE.toFloat()
                            val z = chunk.position.z * Chunk.SIZE.toFloat()
                            model.translate(x, y, z)
                            model.setUniform(FrontendProxy.translucentVoxelShader.getUniform("uModel"))
                            mesh.draw()
                        }
                    }
                    GL11.glDepthMask(true)
                }
            }
        }
    }

    override fun cleanup() {
        visibleChunks.clear()
    }
}