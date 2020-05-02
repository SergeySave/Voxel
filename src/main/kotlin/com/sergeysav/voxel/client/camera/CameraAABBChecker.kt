package com.sergeysav.voxel.client.camera

import org.joml.Vector4f

/**
 * @author sergeys
 */
class CameraAABBChecker {

    private val temp = Vector4f()
    private val left = Vector4f()
    private val right = Vector4f()
    private val bottom = Vector4f()
    private val top = Vector4f()
    private val near = Vector4f()
    private val far = Vector4f()
    private val planes = arrayOf(left, right, bottom, top, near, far)

    fun update(camera: Camera) {
        camera.combined.getRow(3, left)
        camera.combined.getRow(0, temp)
        left.add(temp)
        camera.combined.getRow(3, right)
        right.sub(temp)
        camera.combined.getRow(3, bottom)
        camera.combined.getRow(1, temp)
        bottom.add(temp)
        camera.combined.getRow(3, top)
        top.sub(temp)
        camera.combined.getRow(3, near)
        camera.combined.getRow(2, temp)
        near.add(temp)
        camera.combined.getRow(3, far)
        far.sub(temp)
    }

    fun isAABBinCamera(x: Float, y: Float, z: Float, dx: Float, dy: Float, dz: Float): Boolean {
        // check box outside/inside of frustum
        for (plane in planes) {
            var out = 0
            out += if (plane.dot(x, y, z, 1f) < 0) 1 else 0
            out += if (plane.dot(x + dx, y, z, 1f) < 0) 1 else 0
            out += if (plane.dot(x, y + dy, z, 1f) < 0) 1 else 0
            out += if (plane.dot(x + dx, y + dy, z, 1f) < 0) 1 else 0
            out += if (plane.dot(x, y, z + dz, 1f) < 0) 1 else 0
            out += if (plane.dot(x + dx, y, z + dz, 1f) < 0) 1 else 0
            out += if (plane.dot(x, y + dy, z + dz, 1f) < 0) 1 else 0
            out += if (plane.dot(x + dx, y + dy, z + dz, 1f) < 0) 1 else 0
            if( out==8 ) return false;
        }

        // check frustum outside/inside box
        var out = 0
        for (plane in planes) {
            out += if (plane.x() > x + dx) 1 else 0
        }
        if (out == 8) {
            return false
        }
        out=0
        for (plane in planes) {
            out += if (plane.x() < x) 1 else 0
        }
        if (out == 8) {
            return false
        }
        out=0
        for (plane in planes) {
            out += if (plane.y() > y + dy) 1 else 0
        }
        if (out == 8) {
            return false
        }
        out=0
        for (plane in planes) {
            out += if (plane.y() < y) 1 else 0
        }
        if (out == 8) {
            return false
        }
        out=0
        for (plane in planes) {
            out += if (plane.z() > z + dz) 1 else 0
        }
        if (out == 8) {
            return false
        }
        out=0
        for (plane in planes) {
            out += if (plane.z() < z) 1 else 0
        }
        if (out == 8) {
            return false
        }

        return true
    }
}