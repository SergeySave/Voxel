package com.sergeysav.voxel.common

import org.lwjgl.BufferUtils
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.file.Files
import java.nio.file.Paths
import java.util.Scanner

object IOUtil {
    
    private fun resizeBuffer(buffer: ByteBuffer, newCapacity: Int): ByteBuffer {
        val newBuffer = BufferUtils.createByteBuffer(newCapacity)
        buffer.flip()
        newBuffer.put(buffer)
        return newBuffer
    }
    
    /**
     * Reads the specified resource and returns the raw data as a ByteBuffer.
     *
     * @param resource   the resource to read
     * @param bufferSize the initial buffer size
     *
     * @return the resource data
     *
     * @throws IOException if an IO error occurs
     */
    @Throws(IOException::class)
    fun readResourceToBuffer(resource: String, bufferSize: Int): ByteBuffer {
        var buffer: ByteBuffer = BufferUtils.createByteBuffer(0)
        
        val path = Paths.get(resource)
        if (Files.isReadable(path)) {
            Files.newByteChannel(path).use { fc ->
                buffer = BufferUtils.createByteBuffer(fc.size().toInt() + 1)
                @Suppress("ControlFlowWithEmptyBody")
                while (fc.read(buffer) != -1);
            }
        } else {
            IOUtil::class.java.getResourceAsStream(resource)?.use { source ->
                Channels.newChannel(source).use { rbc ->
                    buffer = BufferUtils.createByteBuffer(bufferSize)
                    
                    while (true) {
                        val bytes = rbc.read(buffer)
                        if (bytes == -1) {
                            break
                        }
                        if (buffer.remaining() == 0) {
                            buffer = resizeBuffer(buffer, buffer.capacity() * 3 / 2) // 50%
                        }
                    }
                }
            } ?: error("Resource $resource not found")
        }
        
        buffer.flip()
        return buffer
    }
    
    fun getResourcePath(resource: String): String {
        val path = Paths.get(resource)
        return if (Files.isReadable(path)) {
            path.toAbsolutePath().toString()
        } else {
            IOUtil::class.java.getResource(resource).path
        }
    }
    
    fun doesResourceExist(resource: String): Boolean = try {
        getResourcePath(resource)
        true
    } catch (e: IllegalStateException) {
        false
    }

    fun loadResource(fileName: String): String {
        IOUtil::class.java.getResourceAsStream(fileName).use {
            Scanner(it, "UTF-8").use { scan ->
                scan.useDelimiter("\\A")
                return scan.next()
            }
        }
    }
}