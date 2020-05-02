package com.sergeysav.voxel.client.resource.texture

import com.sergeysav.voxel.client.gl.Image
import com.sergeysav.voxel.client.gl.Texture2D
import com.sergeysav.voxel.client.gl.TextureInterpolationMode
import com.sergeysav.voxel.client.gl.TextureWrapMode
import com.sergeysav.voxel.client.gl.bound
import com.sergeysav.voxel.client.gl.createTexture
import com.sergeysav.voxel.common.IOUtil
import mu.KotlinLogging
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL30
import org.lwjgl.stb.STBImageWrite
import org.lwjgl.stb.STBRPContext
import org.lwjgl.stb.STBRPNode
import org.lwjgl.stb.STBRPRect
import org.lwjgl.stb.STBRectPack
import org.lwjgl.system.MemoryStack
import java.nio.ByteBuffer
import java.nio.IntBuffer

/**
 * @author sergeys
 *
 * @constructor Creates a new AtlasDataTexture
 */
class AtlasDataTexture(private val width: Int, private val height: Int) {

    private val log = KotlinLogging.logger {  }
    val textureData = BufferUtils.createIntBuffer(width*height*2)

    init {
        log.trace { "Creating New Atlas Data Texture" }
    }

    fun createTexture(): Texture2D {
        log.trace { "Uploading Atlas Data Texture" }
        textureData.rewind()
        val texture = Texture2D(GL11.glGenTextures())
        texture.bound {
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_RG32UI, width, height, 0, GL30.GL_RG_INTEGER, GL11.GL_UNSIGNED_INT, textureData)
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, TextureWrapMode.CLAMP_TO_EDGE.id)
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, TextureWrapMode.CLAMP_TO_EDGE.id)
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, TextureInterpolationMode.NEAREST.id)
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, TextureInterpolationMode.NEAREST.id)
//            if (generateMipmaps) {
//                GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D)
//            }
//            if (clampColor != null) {
//                GL11.glTexParameterfv(GL11.GL_TEXTURE_2D, GL20.GL_TEXTURE_BORDER_COLOR, clampColor)
//            }
        }
        return texture
    }
}