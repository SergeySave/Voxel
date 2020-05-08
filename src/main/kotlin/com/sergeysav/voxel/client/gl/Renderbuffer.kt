package com.sergeysav.voxel.client.gl

import com.sergeysav.voxel.common.Bindable
import org.lwjgl.opengl.GL30
import org.lwjgl.opengl.GL33C

/**
 * @author sergeys
 *
 * @constructor Creates a new Renderbuffer
 */
inline class Renderbuffer(val id: Int): Bindable {
    override fun bind() {
        GL33C.glBindRenderbuffer(GL33C.GL_RENDERBUFFER, id)
    }
    
    override fun unbind() {
        GL33C.glBindRenderbuffer(GL33C.GL_RENDERBUFFER, 0)
    }

    fun cleanup() {
        GL30.glDeleteRenderbuffers(id)
    }
}
