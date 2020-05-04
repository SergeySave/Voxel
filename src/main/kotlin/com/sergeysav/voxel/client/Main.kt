@file:JvmMultifileClass
@file:JvmName("Main")
package com.sergeysav.voxel.client

import com.sergeysav.voxel.common.SidedProxy
import com.sergeysav.voxel.common.chunk.Chunk
import com.sergeysav.voxel.common.chunk.ImmutableChunkPosition
import com.sergeysav.voxel.common.chunk.datamapper.NaiveChunkDataMapper
import com.sergeysav.voxel.common.chunk.datamapper.ZStdChunkDataMapper
import com.sergeysav.voxel.common.world.generator.DevTestGenerator1
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * @author sergeys
 */
fun main() {
    //    ClasspathScanner.scanForAnnotation(EventHandler::class.java).forEach(System.out::println)
//    LoggingUtils.setLogLevel(Level.TRACE)
    SidedProxy.sideProxy = FrontendProxy

//    val compressTest = Chunk(ImmutableChunkPosition(0, -10, 0))
//    val generator = DevTestGenerator1()
//    generator.generateChunk(compressTest)
//    val dataMapper = ZStdChunkDataMapper(NaiveChunkDataMapper())
//    val buffer = ByteBuffer.allocateDirect(8 + 16 * 16 * 16 * (10 + 4 + 1))
//    buffer.order(ByteOrder.BIG_ENDIAN)
//    buffer.putInt(0x12345678)
//    buffer.putInt(dataMapper.mapperId)
//    val len = dataMapper.writeToChunk(buffer.slice(), compressTest)
//    buffer.rewind()
//    buffer.putInt(len)
//    buffer.rewind()
//    buffer.limit(len + 8)
//
//    var result = ""
//    while (buffer.hasRemaining()) {
//        result += " " + (buffer.get().toInt() and 0xFF).toString(16)
//    }
//    println(result)
//
//    return

    Frontend().run("Voxel")
}
