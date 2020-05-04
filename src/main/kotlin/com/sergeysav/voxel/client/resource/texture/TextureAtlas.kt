package com.sergeysav.voxel.client.resource.texture

import com.sergeysav.voxel.client.gl.Image
import com.sergeysav.voxel.client.gl.Texture2D
import com.sergeysav.voxel.client.gl.TextureInterpolationMode
import com.sergeysav.voxel.client.gl.createTexture
import com.sergeysav.voxel.common.IOUtil
import com.sergeysav.voxel.common.MainThreadRunner
import mu.KotlinLogging
import org.lwjgl.BufferUtils
import org.lwjgl.stb.STBImageWrite
import org.lwjgl.stb.STBRPContext
import org.lwjgl.stb.STBRPNode
import org.lwjgl.stb.STBRPRect
import org.lwjgl.stb.STBRectPack
import org.lwjgl.system.MemoryStack
import java.nio.ByteBuffer
import java.nio.IntBuffer
import java.util.concurrent.CompletableFuture

/**
 * @author sergeys
 *
 * @constructor Creates a new TextureAtlas
 */
class TextureAtlas {
    companion object {
        private val log = KotlinLogging.logger { }
        fun loadToAtlas(
            imagePaths: List<String>,
            width: Int, height: Int,
            atlasIndex: Int,
            assetDataTexture: IntBuffer,
            mainThreadRunner: MainThreadRunner
        ): CompletableFuture<Texture2D> {
            log.info { "Generating Texture Atlas" }
            val images = imagePaths.map {
                log.trace { "Loading $it for atlas" }
                Image.load(IOUtil.readResourceToBuffer(it, 1000), true)
            }

            val atlasData = BufferUtils.createByteBuffer(width * height * 4)

            MemoryStack.stackPush().run {
                val context = STBRPContext.mallocStack()
                val nodes = STBRPNode.mallocStack(width)
                STBRectPack.stbrp_init_target(context, width, height, nodes)
                STBRectPack.stbrp_setup_allow_out_of_mem(context, true)
                STBRectPack.stbrp_setup_heuristic(context, STBRectPack.STBRP_HEURISTIC_Skyline_BL_sortHeight)

                val buffer = STBRPRect.mallocStack(images.size)
                for (i in images.indices) {
                    buffer[i].id(i)
                    buffer[i].w(images[i].width.toShort())
                    buffer[i].h(images[i].height.toShort())
                    buffer[i].x(0.toShort())
                    buffer[i].y(0.toShort())
                    buffer[i].was_packed(false)
                }

                if (STBRectPack.stbrp_pack_rects(context, buffer) == 0) {
                    error("Failed to pack")
                } else {

                    for (j in images.indices) {
                        val rect = buffer[j]
                        val imageIndex = rect.id()
                        val x = rect.x()
                        val y = rect.y()
                        val w = rect.w()
                        val h = rect.h()
                        val wasPacked = rect.was_packed()
                        if (wasPacked) {
                            val imageData = images[imageIndex].data
                            val c = images[imageIndex].channels

                            for (i in 0 until h) {
                                atlasData.position((y + i) * width * 4 + x * 4)
                                val atlasSlice = atlasData.slice()
                                atlasSlice.limit(w * 4)
                                atlasData.rewind()

                                for (k in 0 until w) {
                                    imageData.position(w * c * i + k * c)
                                    val imgSlice = imageData.slice()
                                    imgSlice.limit(c)
                                    atlasSlice.put(imgSlice)
                                    for (z in c until 4) {
                                        if (z == 3) {
                                            atlasSlice.put(255.toByte())
                                        } else {
                                            atlasSlice.put(0)
                                        }
                                    }
                                    imageData.rewind()
                                }
                            }

                            assetDataTexture.put(((atlasIndex and 0xFF) shl 24) or ((x.toInt() and 0x3FFF) shl 10) or ((y.toInt() and 0x3FF0) ushr 4))
                            assetDataTexture.put(((y.toInt() and 0x000F) shl 28) or ((w.toInt() and 0x3FFF) shl 14) or (h.toInt() and 0x3FFF))
                        } else {
                            println("Failed to pack image $imageIndex")
                        }
                    }
                }
            }

            log.info { "Outputting Atlas Image" }
            STBImageWrite.stbi_flip_vertically_on_write(true)
            STBImageWrite.stbi_write_png("atlas.png", width, height,  4, atlasData, width * 4)

            val image = Image.createDirect(atlasData, width, height, 4)
            return mainThreadRunner.runOnMainThread {
                log.trace { "Creating Atlas Texture" }
                val texture = image.createTexture(
                    minInterp = TextureInterpolationMode.NEAREST,
                    maxInterp = TextureInterpolationMode.NEAREST,
                    generateMipmaps = false
                )
                image.free()
                texture
            }
        }
    }
}