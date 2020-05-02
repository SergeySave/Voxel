package com.sergeysav.voxel.client.camera

import org.joml.Matrix4f
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector3fc
import org.joml.Vector4f


/**
 * @author sergeys
 *
 * @constructor Creates a new CameraController
 */
class CameraController(val camera: Camera) {
    private val MAT = Matrix4f()
    private val VEC1 = Vector3f()
    private val VEC2 = Vector4f()

    val forward: Vector3fc
        get() = camera.direction
    val right: Vector3fc
        get() = camera.right
    val up: Vector3fc
        get() = camera.up
    
    fun setAspect(width: Int, height: Int) {
        camera.aspect = width.toFloat() / height
    }
    
    fun setPos(x: Float, y: Float, z: Float) {
        camera.position.set(x, y, z)
    }
    
    fun lookAt(x: Float, y: Float, z: Float) {
        camera.lookAt(VEC1.set(x, y, z))
    }
    
    fun translate(direction: Vector3fc, amount: Float) {
        camera.position.add(VEC1.set(direction).mul(amount))
    }
    
    fun rotateAround(center: Vector3fc, axis: Vector3fc, radians: Float) {
        camera.position.set(VEC1.set(camera.position)
                                    .sub(center)
                                    .rotateAxis(radians, axis.x(), axis.y(), axis.z())
                                    .add(center))
        camera.lookAt(center)
    }
    
    fun rotate(axis: Vector3fc, radians: Float) {
        camera.rotate(radians, axis)
    }
    
    fun update() {
        camera.update()
    }
    
    //    fun projectToScreen(vec: Vector3f) {
    //        linAlgPool.mat4 { tempMat4 ->
    //            linAlgPool.vec4 { tempVec4 ->
    //                tempVec4.set(vec.x,  vec.y,  vec.z,  1f)
    //                tempMat4.set(camera.combined)
    //                tempVec4.mul(tempMat4)
    //                vec.x = tempVec4.x / tempVec4.w
    //                vec.y = tempVec4.y / tempVec4.w
    //                vec.z = tempVec4.z / tempVec4.w
    //            }
    //        }
    //    }
    
    /**
     * Returns a normalized Vector representing a ray coming out of the camera's position
     * This vector should not be saved as it will be overridden
     */
    fun projectToWorld(vec: Vector2f): Vector3f {
        MAT.set(camera.combined)
        VEC2.set(vec.x, -vec.y, 0f, 1f)
        MAT.invert()
        VEC2.mul(MAT)
        VEC1.set(VEC2.x / VEC2.w, VEC2.y / VEC2.w, VEC2.z / VEC2.w).sub(camera.position).normalize()
        return VEC1
    }
}