package com.sergeysav.voxel.client.gl

import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL20

enum class TextureWrapMode(val id: Int) {
    REPEAT(GL11.GL_REPEAT),
    MIRRORED_REPEAT(GL20.GL_MIRRORED_REPEAT),
    CLAMP_TO_EDGE(GL20.GL_CLAMP_TO_EDGE),
    CLAMP_TO_COLOR(GL20.GL_CLAMP_TO_BORDER),
}