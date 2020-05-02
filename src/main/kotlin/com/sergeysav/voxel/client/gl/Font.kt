package com.sergeysav.voxel.client.gl

import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL11C
import org.lwjgl.opengl.GL12C
import org.lwjgl.stb.STBTTFontinfo
import org.lwjgl.stb.STBTTPackContext
import org.lwjgl.stb.STBTTPackedchar
import org.lwjgl.stb.STBTruetype.stbtt_GetFontVMetrics
import org.lwjgl.stb.STBTruetype.stbtt_InitFont
import org.lwjgl.stb.STBTruetype.stbtt_PackBegin
import org.lwjgl.stb.STBTruetype.stbtt_PackEnd
import org.lwjgl.stb.STBTruetype.stbtt_PackFontRange
import org.lwjgl.stb.STBTruetype.stbtt_PackSetOversampling
import org.lwjgl.stb.STBTruetype.stbtt_ScaleForPixelHeight
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil.NULL
import org.lwjgl.system.MemoryUtil.memAlloc
import org.lwjgl.system.MemoryUtil.memFree
import java.nio.ByteBuffer

/**
 * @author sergeys
 */
class Font(val fontSize: Float, private val ttf: ByteBuffer) {
    val bitmapW = 1024
    val bitmapH = 1024
    
    val fontTex = Texture2D(GL11.glGenTextures())
    
    val fontInfo: STBTTFontinfo = STBTTFontinfo.create()
    val cdata: STBTTPackedchar.Buffer = STBTTPackedchar.create(95)
    
    var scale: Float = 0f
    var descent: Float = 0f
    
    init {
        stackPush().use { stack ->
            stbtt_InitFont(fontInfo, ttf)
            scale = stbtt_ScaleForPixelHeight(fontInfo, fontSize)
            
            val d = stack.mallocInt(1)
            stbtt_GetFontVMetrics(fontInfo, null, d, null)
            descent = d.get(0) * scale
            
            val bitmap = memAlloc(bitmapW * bitmapH)
            
            val pc = STBTTPackContext.mallocStack(stack)
            stbtt_PackBegin(pc, bitmap, bitmapW, bitmapH, 0, 1, NULL)
            stbtt_PackSetOversampling(pc, 4, 4)
            stbtt_PackFontRange(pc, ttf, 0, fontSize, 32, cdata)
            stbtt_PackEnd(pc)
            
            // Convert R8 to RGBA8
            val texture = memAlloc(bitmapW * bitmapH * 4)
            for (i in 0 until bitmap.capacity()) {
                texture.putInt((bitmap.get(i).toInt() shl 24) or 0x00FFFFFF)
            }
            texture.flip()
            
            GL11C.glBindTexture(GL11C.GL_TEXTURE_2D, fontTex.id)
            GL11C.glTexImage2D(GL11C.GL_TEXTURE_2D, 0, GL11C.GL_RGBA8, bitmapW, bitmapH, 0, GL11C.GL_RGBA,
                               GL12C.GL_UNSIGNED_INT_8_8_8_8_REV, texture)
            GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MAG_FILTER, GL11C.GL_LINEAR)
            GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MIN_FILTER, GL11C.GL_LINEAR)
            
            memFree(texture)
            memFree(bitmap)
        }
    }
    
    fun cleanup() {
        fontTex.cleanup()
        fontInfo.free()
        cdata.free()
    }
}