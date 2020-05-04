package com.sergeysav.voxel.client.chunk.meshing

/**
 * @author sergeys
 */
enum class BlockTextureReflection {
    NO_REFLECT,
    X_REFLECT,
    Y_REFLECT,
    X_Y_REFLECT;

    companion object {
        val all = values()
    }

    operator fun BlockTextureReflection.plus(other: BlockTextureReflection): BlockTextureReflection {
        return all[this.ordinal or other.ordinal]
    }
}