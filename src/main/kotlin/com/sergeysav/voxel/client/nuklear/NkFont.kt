package com.sergeysav.voxel.client.nuklear

import com.sergeysav.voxel.client.gl.Font
import org.lwjgl.nuklear.NkUserFont
import org.lwjgl.nuklear.NkUserFontGlyph
import org.lwjgl.nuklear.Nuklear
import org.lwjgl.stb.STBTTAlignedQuad
import org.lwjgl.stb.STBTruetype
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer

/**
 * @author sergeys
 *
 * @constructor Creates a new NkFont
 */
class NkFont(private val font: Font) {
    val nkFont: NkUserFont = NkUserFont.create()
            .width { _, _, text, len ->
                var textWidth = 0f
                MemoryStack.stackPush().use { stack ->
                    val unicode = stack.mallocInt(1)
                    
                    var glyphLen = Nuklear.nnk_utf_decode(text, MemoryUtil.memAddress(unicode), len)
                    var textLen = glyphLen
                    
                    if (glyphLen == 0) {
                        return@width 0f
                    }
                    
                    val advance = stack.mallocInt(1)
                    while (textLen <= len && glyphLen != 0) {
                        if (unicode.get(0) == Nuklear.NK_UTF_INVALID) {
                            break
                        }
                        
                        /* query currently drawn glyph information */
                        STBTruetype.stbtt_GetCodepointHMetrics(font.fontInfo, unicode.get(0), advance, null)
                        textWidth += advance.get(0) * font.scale
                        
                        /* offset next glyph */
                        glyphLen = Nuklear.nnk_utf_decode(text + textLen, MemoryUtil.memAddress(unicode),
                                                          len - textLen)
                        textLen += glyphLen
                    }
                }
                textWidth
            }
            .height(font.fontSize)
            .query { _, _, glyph, codePoint, _ ->
                MemoryStack.stackPush().use { stack ->
                    val x = stack.floats(0.0f)
                    val y = stack.floats(0.0f)
                    
                    val q = STBTTAlignedQuad.mallocStack(stack)
                    val advance = stack.mallocInt(1)
                    
                    STBTruetype.stbtt_GetPackedQuad(font.cdata, font.bitmapW, font.bitmapH, codePoint - 32, x, y, q,
                                                    false)
                    STBTruetype.stbtt_GetCodepointHMetrics(font.fontInfo, codePoint, advance, null)
                    
                    val ufg = NkUserFontGlyph.create(glyph)
                    
                    ufg.width(q.x1() - q.x0())
                    ufg.height(q.y1() - q.y0())
                    ufg.offset().set(q.x0(), q.y0() + (font.fontSize + font.descent))
                    ufg.xadvance(advance.get(0) * font.scale)
                    ufg.uv(0).set(q.s0(), q.t0())
                    ufg.uv(1).set(q.s1(), q.t1())
                }
            }
            .texture { it.id(font.fontTex.id) }
    
    fun cleanup() {
        nkFont.query()?.free()
        nkFont.width()?.free()
        font.cleanup()
    }
    
    companion object
}

fun NkFont.Companion.fromTTF(size: Float, ttf: ByteBuffer) = NkFont(Font(size, ttf))
