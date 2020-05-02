package com.sergeysav.voxel.client.gl

import com.sergeysav.voxel.common.IOUtil
import mu.KotlinLogging
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL30
import org.lwjgl.stb.STBImage
import java.nio.ByteBuffer

/**
 * @author sergeys
 *
 * @constructor Creates a new Image
 */
class Image private constructor(
    val data: ByteBuffer,
    val width: Int,
    val height: Int,
    val channels: Int,
    private val stbFree: Boolean
) {

    fun free() {
        if (stbFree) {
            STBImage.stbi_image_free(data)
        }
    }
    
    companion object {
        val logger = KotlinLogging.logger {  }

        fun load(bytes: ByteBuffer, flipY: Boolean = true): Image {
            val w = IntArray(1)
            val h = IntArray(1)
            val c = IntArray(1)
            STBImage.stbi_set_flip_vertically_on_load(flipY)
            val data = STBImage.stbi_load_from_memory(bytes, w, h, c, 0)!!
            val width = w[0]
            val height = h[0]
            val channels = c[0]
            return Image(data, width, height, channels, true)
        }

        fun createDirect(data: ByteBuffer, width: Int, height: Int, channels: Int) = Image(data, width, height, channels, false)
    }
}

fun Image.createTexture(imageMode: Int? = null, glMode: Int? = null,
                        xMode: TextureWrapMode = TextureWrapMode.CLAMP_TO_EDGE,
                        yMode: TextureWrapMode = xMode,
                        clampColor: FloatArray? = null,
                        minInterp: TextureInterpolationMode = TextureInterpolationMode.BI_MIPMAP_LINEAR,
                        maxInterp: TextureInterpolationMode = TextureInterpolationMode.BI_MIPMAP_LINEAR,
                        generateMipmaps: Boolean = false): Texture2D {
    val texture2D = Texture2D(GL11.glGenTextures())
    
    val imageFormat = imageMode ?: when (channels) {
        1 -> GL11.GL_RED
        3 -> GL11.GL_RGB
        4 -> GL11.GL_RGBA
        else -> error("Cannot determine Image Format for $channels channels")
    }
    val glFormat = glMode ?: when (channels) {
        1 -> GL11.GL_RED
        3 -> GL11.GL_RGB
        4 -> GL11.GL_RGBA
        else -> error("Cannot determine OpenGL Format for $channels channels")
    }
    
    texture2D.bound {
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, glFormat, width, height, 0, imageFormat, GL11.GL_UNSIGNED_BYTE, data)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, xMode.id)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, yMode.id)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, minInterp.id)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, maxInterp.id)
        if (generateMipmaps) {
            GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D)
        }
        if (clampColor != null) {
            GL11.glTexParameterfv(GL11.GL_TEXTURE_2D, GL20.GL_TEXTURE_BORDER_COLOR, clampColor)
        }
    }
    
    return texture2D
}

fun Image.Companion.createTexture(path: String,
                                  imageMode: Int? = null, glMode: Int? = null,
                                  flipY: Boolean = true,
                                  xMode: TextureWrapMode = TextureWrapMode.CLAMP_TO_EDGE,
                                  yMode: TextureWrapMode = xMode,
                                  clampColor: FloatArray? = null,
                                  minInterp: TextureInterpolationMode = TextureInterpolationMode.BI_MIPMAP_LINEAR,
                                  maxInterp: TextureInterpolationMode = TextureInterpolationMode.BI_MIPMAP_LINEAR,
                                  generateMipmaps: Boolean = false): Texture2D {
    logger.trace { "Creating Image $path" }
    val image = load(IOUtil.readResourceToBuffer(path, 1000), flipY)
    val texture = image.createTexture(imageMode, glMode, xMode, yMode, clampColor, minInterp, maxInterp, generateMipmaps)
    image.free()
    return texture
}
