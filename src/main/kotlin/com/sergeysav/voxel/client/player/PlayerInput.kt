package com.sergeysav.voxel.client.player

/**
 * @author sergeys
 *
 * @constructor Creates a new PlayerInput
 */
data class PlayerInput(
    var mouseButton1JustDown: Boolean,
    var mouseButton1Down: Boolean,
    var mouseButton1JustUp: Boolean,
    var mouseButton2JustDown: Boolean,
    var mouseButton2Down: Boolean,
    var mouseButton2JustUp: Boolean
)