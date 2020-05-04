package com.sergeysav.voxel.client.screen.mainmenu

import com.sergeysav.voxel.client.Frontend
import com.sergeysav.voxel.client.nuklear.Gui
import com.sergeysav.voxel.client.nuklear.GuiWindow
import com.sergeysav.voxel.client.nuklear.HAlign
import com.sergeysav.voxel.client.screen.game.GameScreen
import org.lwjgl.nuklear.Nuklear

/**
 * @author sergeys
 *
 * @constructor Creates a new MainMenuUI
 */
class MainMenuUI : GuiWindow("Main Menu") {

    fun layout(application: Frontend) {
        nkEditing(application.gui) {
            window.background.hidden {
                window(0f, 0f, application.width.toFloat(), application.height.toFloat(),
                    Nuklear.NK_WINDOW_NO_INPUT or
//                            Nuklear.NK_WINDOW_ROM or
                            Nuklear.NK_WINDOW_NO_SCROLLBAR) {
                    dynamicRow(1, (application.height - LOADING_TEXT_HEIGHT - PLAY_BUTTON_HEIGHT) / 3f) {}

                    dynamicRow(1, LOADING_TEXT_HEIGHT) {
                        font(application.gui.bigFont) {
                            label("Voxel Test Game", HAlign.CENTER)
                        }
                    }

                    dynamicRow(1, PLAY_BUTTON_HEIGHT) {
                        button("Enter World") {
                            application.openScreen(GameScreen())
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val LOADING_TEXT_HEIGHT = 40f
        private const val PLAY_BUTTON_HEIGHT = 20f
    }
}