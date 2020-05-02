package com.sergeysav.voxel.client.gl

import org.lwjgl.opengl.GL15

/**
 * @author sergeys
 */
enum class GLDataUsage(val draw: Int) {
    STATIC(GL15.GL_STATIC_DRAW),
    DYNAMIC(GL15.GL_DYNAMIC_DRAW),
    STREAM(GL15.GL_STREAM_DRAW);
}