package com.sergeysav.voxel.client.gl

import org.lwjgl.opengl.GL11

/**
 * @author sergeys
 */
enum class GLDrawingMode(val mode: Int) {
    POINTS(GL11.GL_POINTS),
    TRIANGLES(GL11.GL_TRIANGLES),
    TRIANGLE_STRIP(GL11.GL_TRIANGLE_STRIP),
    TRIANGLE_FAN(GL11.GL_TRIANGLE_FAN)
}