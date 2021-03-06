package com.sergeysav.voxel.client.chunk.meshing

/**
 * @author sergeys
 */
enum class BlockTextureRotation {
    NO_ROTATE,
    COUNTER_CLOCKWISE,
    FULL,
    CLOCKWISE;

    companion object {
        val all = values()
    }

    operator fun BlockTextureRotation.plus(other: BlockTextureRotation): BlockTextureRotation {
        return values()[(this.ordinal + other.ordinal) % values().size]
    }
}