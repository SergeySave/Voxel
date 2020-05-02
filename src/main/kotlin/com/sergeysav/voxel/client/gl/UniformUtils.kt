package com.sergeysav.voxel.client.gl

import org.joml.Matrix3f
import org.joml.Matrix4f
import org.lwjgl.opengl.GL20
import org.lwjgl.system.MemoryStack

/**
 * @author sergeys
 */
fun Matrix3f.setUniform(uniformId: Int) {
    MemoryStack.stackPush().use { stack ->
        // Dump the matrix into a float buffer
        val fb = stack.mallocFloat(9)
        this.get(fb)
        GL20.glUniformMatrix3fv(uniformId, false, fb)
    }
}

fun Matrix4f.setUniform(uniformId: Int) {
    MemoryStack.stackPush().use { stack ->
        // Dump the matrix into a float buffer
        val fb = stack.mallocFloat(16)
        this.get(fb)
        GL20.glUniformMatrix4fv(uniformId, false, fb)
    }
}