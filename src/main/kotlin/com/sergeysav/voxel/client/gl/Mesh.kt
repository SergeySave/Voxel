package com.sergeysav.voxel.client.gl

import com.sergeysav.voxel.common.bound
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL15
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL30
import java.nio.IntBuffer

/**
 * @author sergeys
 *
 * @constructor Creates a new Mesh
 */
class Mesh(private var glDrawingMode: GLDrawingMode, private val useIBOs: Boolean = false) {

    var fullyInitialized = false
        private set
    private val vao = VertexArrayObject(GL30.glGenVertexArrays())
    private val vbo = VertexBufferObject(GL15.glGenBuffers())
    private val ibo = ElementBufferObject(if (useIBOs) GL15.glGenBuffers() else 0) // 0 = null
    private var vertexCount = 0
    var indexCount = 0
        private set
    private lateinit var vertexGLDataUsage: GLDataUsage
    
    fun setVertices(data: FloatArray, dataUsage: GLDataUsage, vararg attributes: VertexAttribute) {
        val stride = attributes.map(VertexAttribute::totalLength).sum()
        vertexCount = data.size/attributes.map(VertexAttribute::components).sum()
        vertexGLDataUsage = dataUsage
        
        vao.bound {
            vbo.bind()
            vbo.setData(data, dataUsage)
            
            var offset = 0L
            for ((index, attribute) in attributes.withIndex()) {
                if (attribute is UIntTypeVertexAttribute) {
                    GL30.glVertexAttribIPointer(index, attribute.components, attribute.type, stride, offset)
                } else {
                    GL20.glVertexAttribPointer(
                        index,
                        attribute.components,
                        attribute.type,
                        attribute.normalized,
                        stride,
                        offset
                    )
                }
                GL20.glEnableVertexAttribArray(index)
                offset += attribute.totalLength
            }

            if (useIBOs) {
                ibo.bind()
            }
        }
        fullyInitialized = true
    }
    
    fun setVertexData(data: FloatArray) {
        vao.bound {
            vbo.setData(data, vertexGLDataUsage)
        }
    }

    fun setVertices(data: IntBuffer, dataUsage: GLDataUsage, vararg attributes: VertexAttribute) {
        val stride = attributes.map(VertexAttribute::totalLength).sum()
        vertexCount = (data.limit())/attributes.map(VertexAttribute::components).sum()
        vertexGLDataUsage = dataUsage

        vao.bound {
            vbo.bind()
            vbo.setData(data, dataUsage)

            var offset = 0L
            for ((index, attribute) in attributes.withIndex()) {
                if (attribute is UIntTypeVertexAttribute) {
                    GL30.glVertexAttribIPointer(index, attribute.components, attribute.type, stride, offset)
                } else {
                    GL20.glVertexAttribPointer(
                        index,
                        attribute.components,
                        attribute.type,
                        attribute.normalized,
                        stride,
                        offset
                    )
                }
                GL20.glEnableVertexAttribArray(index)
                offset += attribute.totalLength
            }

            if (useIBOs) {
                ibo.bind()
            }
        }
        fullyInitialized = true
    }

    fun setVertexData(data: IntBuffer) {
        vao.bound {
            vbo.setData(data, vertexGLDataUsage)
        }
    }
    
    fun setIndexData(data: IntArray, dataUsage: GLDataUsage, count: Int = data.size) {
        if (!useIBOs) {
            throw IllegalStateException("Mesh Index data disabled")
        }
        vao.bound {
            ibo.bind()
            ibo.setData(data, dataUsage)
        }
        this.indexCount = count
    }

    fun setIndexData(data: IntBuffer, dataUsage: GLDataUsage, count: Int = data.limit()) {
        if (!useIBOs) {
            throw IllegalStateException("Mesh Index data disabled")
        }
        vao.bound {
            ibo.bind()
            ibo.setData(data, dataUsage)
        }
        this.indexCount = count
    }
    
    fun draw(numIndices: Int = this.indexCount) {
        bound {
            if (useIBOs) {
                GL11.glDrawElements(glDrawingMode.mode, numIndices, GL11.GL_UNSIGNED_INT, 0)
            } else {
                GL11.glDrawArrays(glDrawingMode.mode, 0, vertexCount)
            }
        }
    }
    
    fun bound(inner: ()->Unit) {
        vao.bound {
            inner()
        }
    }
    
    fun cleanup() {
        if (useIBOs) {
            ibo.cleanup()
        }
        vbo.cleanup()
        vao.cleanup()
    }
}
