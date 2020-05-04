package com.sergeysav.voxel.client.screen.loading

import com.sergeysav.voxel.client.Frontend
import com.sergeysav.voxel.client.screen.Screen
import com.sergeysav.voxel.common.MainThreadRunner
import com.sergeysav.voxel.common.Voxel
import mu.KotlinLogging
import java.util.concurrent.BlockingQueue
import java.util.concurrent.CompletableFuture
import java.util.concurrent.LinkedBlockingQueue

/**
 * @author sergeys
 *
 * @constructor Creates a new LoadingScreen
 */
class LoadingScreen : Screen, MainThreadRunner {

    private val log = KotlinLogging.logger {  }
    private lateinit var application: Frontend
    private var loadingUI: LoadingUI = LoadingUI()
    private val toRunOnMainThreadQueue: BlockingQueue<()->Unit> = LinkedBlockingQueue()
    private var error: Throwable? = null

    override fun register(application: Frontend) {
        this.application = application

        Thread {
            try {
                log.info { "Starting Loading Procedure" }
                Voxel.initialize(this)
                log.info { "Loading Completed" }
                application.popScreen()
            } catch (e: Throwable) {
                error = e
            }
        }.apply {
            name = "Loading Thread"
            isDaemon = true
            start()
        }
    }

    override fun <T> runOnMainThread(inner: () -> T): CompletableFuture<T> {
        val future = CompletableFuture<T>()
        toRunOnMainThreadQueue.put {
            future.complete(inner())
        }
        return future
    }

    override fun render(delta: Double) {
        loadingUI.layout(application.gui, application.width.toFloat(), application.height.toFloat())
        toRunOnMainThreadQueue.poll()?.invoke()
        val err = error
        if (err != null) {
            error = null
            throw err
        }
    }

    override fun unregister(application: Frontend) {
    }

    override fun cleanup() {
    }
}