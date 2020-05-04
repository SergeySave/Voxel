package com.sergeysav.voxel.common.region

/**
 * @author sergeys
 */
abstract class RegionPosition {
    abstract val x: Int
    abstract val y: Int
    abstract val z: Int

    operator fun component1() = x
    operator fun component2() = y
    operator fun component3() = z

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RegionPosition) return false

        if (x != other.x) return false
        if (y != other.y) return false
        if (z != other.z) return false

        return true
    }

    override fun hashCode(): Int {
        var result = x
        result = 31 * result + y
        result = 31 * result + z
        return result
    }

    override fun toString(): String {
        return "${this::class.simpleName}(x=$x, y=$y, z=$z)"
    }
}
