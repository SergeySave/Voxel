package com.sergeysav.voxel.common.math

/**
 * @author sergeys
 */
fun Int.divisionQuotient(divisor: Int) = if (this % divisor < 0) {
    this / divisor - 1
} else {
    this / divisor
}

fun Int.divisionRemainder(divisor: Int) = if (this % divisor < 0) {
    this % divisor + divisor
} else {
    this % divisor
}

fun Int.square() = this * this

fun Double.square() = this * this
