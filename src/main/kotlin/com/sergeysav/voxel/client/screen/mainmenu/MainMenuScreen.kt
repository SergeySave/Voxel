package com.sergeysav.voxel.client.screen.mainmenu

import com.sergeysav.voxel.client.Frontend
import com.sergeysav.voxel.client.screen.Screen
import mu.KotlinLogging

/**
 * @author sergeys
 *
 * @constructor Creates a new MainMenuScreen
 */
class MainMenuScreen : Screen {

    private val log = KotlinLogging.logger {  }
    private lateinit var application: Frontend
    private var mainMenuUI: MainMenuUI? = null

    override fun register(application: Frontend) {
        this.application = application
        mainMenuUI = MainMenuUI()
    }

    override fun render(delta: Double) {
        mainMenuUI?.layout(application)
    }

    override fun unregister(application: Frontend) {
        mainMenuUI = null //Clean up the main menu ui if not needed
    }

    override fun cleanup() {
    }
}