@file:JvmMultifileClass
@file:JvmName("Main")
package com.sergeysav.voxel.client

import com.sergeysav.voxel.common.SidedProxy

/**
 * @author sergeys
 */
fun main() {
    //    ClasspathScanner.scanForAnnotation(EventHandler::class.java).forEach(System.out::println)
//    LoggingUtils.setLogLevel(Level.TRACE)
    SidedProxy.sideProxy = FrontendProxy
    Frontend().run("Voxel")
}
