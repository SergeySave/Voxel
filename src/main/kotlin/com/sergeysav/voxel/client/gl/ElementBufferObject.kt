package com.sergeysav.voxel.client.gl

import com.sergeysav.voxel.common.Bindable
import org.lwjgl.opengl.GL15
import java.nio.IntBuffer

inline class ElementBufferObject(val id: Int = GL15.glGenBuffers()):
    Bindable {
    fun cleanup() = GL15.glDeleteBuffers(id)
    override fun bind() = GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, id)
    override fun unbind() = GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0)
    fun setData(data: IntArray, usage: GLDataUsage) =
            GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, data, usage.draw)
    
        fun setData(data: IntBuffer, usage: GLDataUsage) =
                GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, data, usage.draw)
}
