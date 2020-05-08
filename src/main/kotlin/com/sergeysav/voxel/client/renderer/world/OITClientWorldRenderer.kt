package com.sergeysav.voxel.client.renderer.world

import com.sergeysav.voxel.client.FrontendProxy
import com.sergeysav.voxel.client.camera.Camera
import com.sergeysav.voxel.client.camera.CameraAABBChecker
import com.sergeysav.voxel.client.chunk.ClientChunk
import com.sergeysav.voxel.client.gl.Framebuffer
import com.sergeysav.voxel.client.gl.Renderbuffer
import com.sergeysav.voxel.client.gl.Texture2D
import com.sergeysav.voxel.client.gl.TextureInterpolationMode
import com.sergeysav.voxel.client.gl.bound
import com.sergeysav.voxel.client.gl.setUniform
import com.sergeysav.voxel.client.renderer.world.ClientWorldRenderer
import com.sergeysav.voxel.common.bound
import com.sergeysav.voxel.common.chunk.Chunk
import mu.KotlinLogging
import org.joml.Matrix4f
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL21
import org.lwjgl.opengl.GL30
import org.lwjgl.opengl.GL40
import org.lwjgl.system.MemoryUtil

/**
 * This renders using Order-Independent Transparency for the translucent mesh
 *
 * NOTE: For this to work translucentVoxelShader must use transparent.fragment.glsl
 *
 * @author sergeys
 *
 * @constructor Creates a new OITClientWorldRenderer
 */
class OITClientWorldRenderer : ClientWorldRenderer {

    private val log = KotlinLogging.logger {  }
    private val model = Matrix4f()
    private val cameraAABBChecker = CameraAABBChecker()
    private val visibleChunks = ArrayList<ClientChunk>(4096)
    private var opaqueFrameBuffer = Framebuffer(0)
    private var opaqueRenderBuffer = Renderbuffer(0)
    private var opaqueColorBuffer = Texture2D(0)
    private var transparentFrameBuffer = Framebuffer(0)
    private var transparentAccumBuffer = Texture2D(0)
    private var transparentRevealageBuffer = Texture2D(0)
    private var lastWidth = 800
    private var lastHeight = 600

    override fun initialize() {
        log.trace { "Initializing Client World Renderer" }

        GL11.glEnable(GL11.GL_DEPTH_TEST)
        GL11.glDepthFunc(GL11.GL_LESS)

        GL11.glEnable(GL11.GL_CULL_FACE)
        GL11.glCullFace(GL11.GL_BACK)

        GL11.glEnable(GL11.GL_BLEND)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)

        createBuffers()
    }

    private fun createBuffers() {
        opaqueFrameBuffer = Framebuffer(GL30.glGenFramebuffers())
        opaqueRenderBuffer = Renderbuffer(GL30.glGenRenderbuffers())
        opaqueColorBuffer = Texture2D(GL11.glGenTextures())
        transparentFrameBuffer = Framebuffer(GL30.glGenFramebuffers())
        transparentAccumBuffer = Texture2D(GL11.glGenTextures())
        transparentRevealageBuffer = Texture2D(GL11.glGenTextures())

        opaqueFrameBuffer.bound {
            opaqueColorBuffer.bind()
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, lastWidth, lastHeight, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, MemoryUtil.NULL)
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, TextureInterpolationMode.LINEAR.id)
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, TextureInterpolationMode.LINEAR.id)
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, opaqueColorBuffer.id, 0)

            GL30.glDrawBuffers(intArrayOf(GL30.GL_COLOR_ATTACHMENT0))

            opaqueRenderBuffer.bind()
            GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER, GL30.GL_DEPTH24_STENCIL8, lastWidth, lastHeight)
            GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_STENCIL_ATTACHMENT, GL30.GL_RENDERBUFFER, opaqueRenderBuffer.id)
        }

        transparentFrameBuffer.bound {
            transparentAccumBuffer.bind(0)
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_RGBA16F, lastWidth, lastHeight, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, MemoryUtil.NULL)

            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, TextureInterpolationMode.LINEAR.id)
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, TextureInterpolationMode.LINEAR.id)
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, transparentAccumBuffer.id, 0)

            transparentRevealageBuffer.bind(1)
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_R8, lastWidth, lastHeight, 0, GL11.GL_RED, GL11.GL_UNSIGNED_BYTE, MemoryUtil.NULL)
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, TextureInterpolationMode.LINEAR.id)
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, TextureInterpolationMode.LINEAR.id)
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT1, GL11.GL_TEXTURE_2D, transparentRevealageBuffer.id, 0)

            GL30.glDrawBuffers(intArrayOf(GL30.GL_COLOR_ATTACHMENT0, GL30.GL_COLOR_ATTACHMENT1))

            opaqueRenderBuffer.bind()
            GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_STENCIL_ATTACHMENT, GL30.GL_RENDERBUFFER, opaqueRenderBuffer.id)
        }
    }

    private fun cleanupBuffers() {
        transparentFrameBuffer.cleanup()
        transparentAccumBuffer.cleanup()
        transparentRevealageBuffer.cleanup()
        opaqueFrameBuffer.cleanup()
        opaqueRenderBuffer.cleanup()
        opaqueColorBuffer.cleanup()
    }

    override fun render(camera: Camera, chunks: List<ClientChunk>, width: Int, height: Int) {
        if (lastWidth != width || lastHeight != height) {
            lastWidth = width
            lastHeight = height
            cleanupBuffers()
            createBuffers()
        }
        cameraAABBChecker.update(camera)

        visibleChunks.clear()
        for (i in chunks.indices) {
            val chunk = chunks[i]
            if (chunk.loaded && chunk.meshed && (chunk.opaqueMesh != null || chunk.translucentMesh != null)) {
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
                opaqueFrameBuffer.bound {
                    GL30.glClearBufferfv(GL30.GL_COLOR, 0,
                        opaqueClearColor
                    )
                    GL30.glClearBufferfi(GL30.GL_DEPTH_STENCIL, 0, 1f, 0)

                    FrontendProxy.voxelShader.bound { // Opaque Components are rendered normally
                        camera.combined.setUniform(FrontendProxy.voxelShader.getUniform("uCamera"))
                        GL20.glUniform1i(FrontendProxy.voxelShader.getUniform("assetData"), 0)
                        GL20.glUniform1i(FrontendProxy.voxelShader.getUniform("atlasPage0"), 1)

                        // Sort chunks in based on the direction that the camera is facing (dot product)
                        visibleChunks.sortBy { (it.position.x + 0.5) * Chunk.SIZE * camera.direction.x() +
                                (it.position.y + 0.5) * Chunk.SIZE * camera.direction.y() +
                                (it.position.z + 0.5) * Chunk.SIZE * camera.direction.z() }

                        for (i in visibleChunks.indices) {
                            val chunk = visibleChunks[i]
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
                }

                // Render Transparent components to the 3d transparency accumulation framebuffer
                transparentFrameBuffer.bound {
                    GL30.glClearBufferfv(GL30.GL_COLOR, 0,
                        accumClearColor
                    )
                    GL30.glClearBufferfv(GL30.GL_COLOR, 1,
                        revealageClearColor
                    )
                    GL40.glBlendFunci(0, GL11.GL_ONE, GL11.GL_ONE)
                    GL40.glBlendFunci(1, GL11.GL_ZERO, GL11.GL_ONE_MINUS_SRC_COLOR)
                    GL40.glBlendEquationi(0, GL21.GL_FUNC_ADD)
                    GL40.glBlendEquationi(1, GL21.GL_FUNC_ADD)
                    FrontendProxy.translucentVoxelShader.bound {
                        camera.combined.setUniform(FrontendProxy.translucentVoxelShader.getUniform("uCamera"))
                        GL20.glUniform1i(FrontendProxy.translucentVoxelShader.getUniform("assetData"), 0)
                        GL20.glUniform1i(FrontendProxy.translucentVoxelShader.getUniform("atlasPage0"), 1)
                        GL11.glDepthMask(false)

                        for (i in visibleChunks.indices) {
                            val chunk = visibleChunks[i]
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

        transparentRevealageBuffer.bound(0) {
            FrontendProxy.passthroughShader.bound {
                GL20.glUniform1i(FrontendProxy.passthroughShader.getUniform("inputTexture"), 0)
                GL20.glUniform2f(FrontendProxy.passthroughShader.getUniform("viewportSize"), width.toFloat(), height.toFloat())
                GL11.glDisable(GL11.GL_DEPTH_TEST)
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)

                FrontendProxy.screenMesh.draw()

                GL11.glEnable(GL11.GL_DEPTH_TEST)
            }
        }

        transparentAccumBuffer.bound(0) {
            transparentRevealageBuffer.bound(1) {
                FrontendProxy.compositionShader.bound {
                    GL20.glUniform1i(FrontendProxy.compositionShader.getUniform("accumTexture"), 0)
                    GL20.glUniform1i(FrontendProxy.compositionShader.getUniform("revealageTexture"), 1)
                    GL20.glUniform2f(FrontendProxy.compositionShader.getUniform("viewportSize"), width.toFloat(), height.toFloat())
                    GL11.glDisable(GL11.GL_DEPTH_TEST)
                    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)

                    FrontendProxy.screenMesh.draw()

                    GL11.glEnable(GL11.GL_DEPTH_TEST)
                }
            }
        }
    }

    override fun cleanup() {
        visibleChunks.clear()
        cleanupBuffers()
    }

    companion object {
        val opaqueClearColor = floatArrayOf(0f, 0f, 0f, 0f)
        val accumClearColor = floatArrayOf(0f, 0f, 0f, 0f)
        val revealageClearColor = floatArrayOf(1f, 0f, 0f, 0f)
    }
}