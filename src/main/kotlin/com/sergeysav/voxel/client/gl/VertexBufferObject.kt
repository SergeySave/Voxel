package com.sergeysav.voxel.client.gl

import com.sergeysav.voxel.common.Bindable
import org.lwjgl.opengl.GL15
import java.nio.IntBuffer

inline class VertexBufferObject(val id: Int = GL15.glGenBuffers()):
    Bindable {
    fun cleanup() = GL15.glDeleteBuffers(id)
    override fun bind() = GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, id)
    override fun unbind() = GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0)
    
    fun setData(data: FloatArray, usage: GLDataUsage) =
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, data, usage.draw)

    fun setData(data: IntBuffer, usage: GLDataUsage) =
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, data, usage.draw)

    fun setData(data: IntArray, usage: GLDataUsage) =
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, data, usage.draw)

//    fun setData(data: FloatBuffer, usage: GLDataUsage) =
//            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, data, usage.draw)
}
