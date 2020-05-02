package com.sergeysav.voxel.client.camera

import org.joml.Matrix3f
import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector3fc

/**
 * @author sergeys
 *
 * @constructor Creates a new Camera
 */
class Camera(private val fovy: Float, var aspect: Float, private val zNear: Float, private val zFar: Float) {
    val position = Vector3f(0f, 0f, 0f)
    val direction = Vector3f(1f, 0f, 0f)
    val up = Vector3f(0f, 1f, 0f)
    val right = Vector3f(0f, 0f, 1f)
    
    private val tempVec3 = Vector3f()
    private val tempMat3 = Matrix3f()
    
    val perspective: Matrix4f = Matrix4f()
    val view: Matrix4f = Matrix4f()
    val combined: Matrix4f = Matrix4f()
    
    fun update() {
        tempVec3.set(position).add(direction) // = target
        perspective.setPerspective(fovy, aspect, zNear, zFar)
        view.setLookAt(position, tempVec3, up)
        combined.set(perspective).mul(view)
    }
    
    fun rotate(angle: Float, axis: Vector3fc) {
        tempMat3.set(direction, up, right).transpose().rotate(angle, axis).transpose()
        tempMat3.getColumn(0, direction)
        tempMat3.getColumn(1, up)
        tempMat3.getColumn(2, right)
    }
    
    fun lookAt(target: Vector3fc) {
        direction.set(target).sub(position).normalize()
        right.set(direction).cross(up)
        up.set(tempVec3.set(right).cross(direction).normalize())
    }
}