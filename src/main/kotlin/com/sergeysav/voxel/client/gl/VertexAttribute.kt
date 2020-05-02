package com.sergeysav.voxel.client.gl

import org.lwjgl.opengl.GL11

/**
 * @author sergeys
 *
 * @constructor Creates a new VertexAttribute
 */
sealed class VertexAttribute(val name: String, val type: Int, val components: Int, bytes: Int,
                             val normalized: Boolean) {
    val totalLength = components * bytes
}

sealed class FloatTypeVertexAttribute(name: String, components: Int, normalized: Boolean) : VertexAttribute(name, GL11.GL_FLOAT, components, 4, normalized)
//class FloatVertexAttribute(name: String, normalized: Boolean = false) : FloatTypeVertexAttribute(name, 1, normalized)
class Vec2VertexAttribute(name: String, normalized: Boolean = false) : FloatTypeVertexAttribute(name, 2, normalized)
class Vec3VertexAttribute(name: String, normalized: Boolean = false) : FloatTypeVertexAttribute(name, 3, normalized)
//class Vec4VertexAttribute(name: String, normalized: Boolean = false): FloatTypeVertexAttribute(name, 4, normalized)

sealed class UIntTypeVertexAttribute(name: String, components: Int, normalized: Boolean) : VertexAttribute(name, GL11.GL_UNSIGNED_INT, components, 4, normalized)

class UVec3VertexAttribute(name: String, normalized: Boolean = false) : UIntTypeVertexAttribute(name, 3, normalized)
