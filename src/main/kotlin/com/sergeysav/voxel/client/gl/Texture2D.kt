package com.sergeysav.voxel.client.gl

import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL20

/**
 * @author sergeys
 */
inline class Texture2D(val id: Int) {
    fun bind(textureNum: Int = 0) {
        GL20.glActiveTexture(GL20.GL_TEXTURE0 + textureNum)
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, id)
    }
    
    fun unbind() = GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0)
    fun cleanup() = GL11.glDeleteTextures(id)
}

fun Texture2D.bound(textureNum: Int = 0, inner: () -> Unit) {
    try {
        bind(textureNum)
        inner()
    } finally {
        unbind()
    }
}
