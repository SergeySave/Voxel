package com.sergeysav.voxel.client.gl

import com.sergeysav.voxel.common.Bindable
import org.lwjgl.opengl.GL30

inline class VertexArrayObject(val id: Int = GL30.glGenVertexArrays()): Bindable {
    fun cleanup() = GL30.glDeleteVertexArrays(id)
    override fun bind() = GL30.glBindVertexArray(id)
    override fun unbind() = GL30.glBindVertexArray(0)
}
