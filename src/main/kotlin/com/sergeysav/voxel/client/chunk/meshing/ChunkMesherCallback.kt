package com.sergeysav.voxel.client.chunk.meshing

import com.sergeysav.voxel.common.data.Direction
import com.sergeysav.voxel.client.resource.texture.TextureResource

/**
 * @author sergeys
 */
interface ChunkMesherCallback {

    fun addVertex(x: Double, y: Double, z: Double,
                  texture: TextureResource,
                  facing: Direction, rotation: BlockTextureRotation, reflection: BlockTextureReflection,
                  lightingR: Double, lightingG: Double, lightingB: Double): Int
    fun addTriangle(vertex1: Int, vertex2: Int, vertex3: Int)

    fun addAAQuad(inset: Double, l1: Double, u1: Double, r1: Double, g1: Double, b1: Double,
                  l2: Double, u2: Double, r2: Double, g2: Double, b2: Double,
                  l3: Double, u3: Double, r3: Double, g3: Double, b3: Double,
                  l4: Double, u4: Double, r4: Double, g4: Double, b4: Double,
                  texture: TextureResource, facing: Direction, rotation: BlockTextureRotation,
                  reflection: BlockTextureReflection, border: Boolean, doubleRender: Boolean)

}