package com.sergeysav.voxel.client.gl

import com.sergeysav.voxel.common.Bindable
import mu.KotlinLogging
import org.lwjgl.opengl.GL20

/**
 * @author sergeys
 *
 * @constructor Creates a new ShaderProgram
 */
class ShaderProgram @Throws(ShaderException::class) constructor():
    Bindable {
    private val log = KotlinLogging.logger {}
    
    private val programId: Int = GL20.glCreateProgram()
    private var vertexShaderId: Int = 0
    private var fragmentShaderId: Int = 0
    
    private val attributes = mutableMapOf<String, Int>()
    private val uniforms = mutableMapOf<String, Int>()
    
    private var bound: Boolean = false
    
    init {
        if (programId == 0) {
            throw ShaderException("Error creating shader")
        }
    }
    
    @Throws(ShaderException::class)
    fun createVertexShader(shaderCode: String) {
        vertexShaderId = createShader(shaderCode, GL20.GL_VERTEX_SHADER)
    }
    
    @Throws(ShaderException::class)
    fun createFragmentShader(shaderCode: String) {
        fragmentShaderId = createShader(shaderCode, GL20.GL_FRAGMENT_SHADER)
    }
    
    @Throws(ShaderException::class)
    private fun createShader(shaderCode: String, shaderType: Int): Int {
        val shaderId = GL20.glCreateShader(shaderType)
        if (shaderId == 0) {
            throw ShaderException("Error creating shader. Type: $shaderType")
        }
    
        GL20.glShaderSource(shaderId, shaderCode)
        GL20.glCompileShader(shaderId)
        
        if (GL20.glGetShaderi(shaderId, GL20.GL_COMPILE_STATUS) == 0) {
            throw ShaderException(
                    "Error compiling Shader code: " + GL20.glGetShaderInfoLog(shaderId, 1024))
        }
    
        GL20.glAttachShader(programId, shaderId)
        
        return shaderId
    }
    
    @Throws(ShaderException::class)
    fun link() {
        GL20.glLinkProgram(programId)
        if (GL20.glGetProgrami(programId, GL20.GL_LINK_STATUS) == 0) {
            throw ShaderException(
                    "Error linking Shader code: " + GL20.glGetProgramInfoLog(programId, 1024))
        }
        
        if (vertexShaderId != 0) {
            GL20.glDeleteShader(vertexShaderId)
        }
        if (fragmentShaderId != 0) {
            GL20.glDeleteShader(fragmentShaderId)
        }
    }
    
    fun validate() {
        GL20.glValidateProgram(programId)
        if (GL20.glGetProgrami(programId, GL20.GL_VALIDATE_STATUS) == 0) {
            log.warn { "Warning validating Shader code: " + GL20.glGetProgramInfoLog(programId, 1024) }
        }
    }
    
    fun getAttribute(attribute: String) =
            attributes.computeIfAbsent(attribute) { GL20.glGetAttribLocation(programId, attribute) }

    fun getUniform(uniform: String) = uniforms.computeIfAbsent(uniform) { GL20.glGetUniformLocation(programId, uniform) }
    
    override fun bind() {
        if (!bound) {
            GL20.glUseProgram(programId)
            bound = true
        }
    }
    
    override fun unbind() {
        if (bound) {
            bound = false
            GL20.glUseProgram(0)
        }
    }
    
    fun cleanup() {
        unbind()
        if (programId != 0) {
            GL20.glDeleteProgram(programId)
        }
    }
}
