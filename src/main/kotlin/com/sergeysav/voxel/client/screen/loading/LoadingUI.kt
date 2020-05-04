package com.sergeysav.voxel.client.screen.loading

import com.sergeysav.voxel.client.nuklear.Gui
import com.sergeysav.voxel.client.nuklear.GuiWindow
import com.sergeysav.voxel.client.nuklear.HAlign
import com.sergeysav.voxel.common.Voxel
import org.lwjgl.nuklear.Nuklear

/**
 * @author sergeys
 *
 * @constructor Creates a new LoadingUI
 */
class LoadingUI : GuiWindow("Loading") {

    fun layout(gui: Gui, width: Float, height: Float) {
        nkEditing(gui) {
            window.background.hidden {
                window(0f, 0f, width, height,
                    Nuklear.NK_WINDOW_NO_INPUT or
                            Nuklear.NK_WINDOW_ROM or
                            Nuklear.NK_WINDOW_NO_SCROLLBAR) {
                    dynamicRow(1, (height - LOADING_TEXT_HEIGHT/* - LOADING_BAR_HEIGHT - LOADING_REASON_HEIGHT*/) / 2f) {}
                    dynamicRow(1, LOADING_TEXT_HEIGHT) {
                        label("Loading...", HAlign.CENTER)
                    }
//                    dynamicRow(1, LOADING_BAR_HEIGHT) {
//                        progressBar(Voxel.getCurrentLoadAmount(), Voxel.getMaxLoadAmount(), false)
//                    }
//                    dynamicRow(1, LOADING_REASON_HEIGHT) {
//                        label(Voxel.getCurrentLoadStatus(), HAlign.CENTER)
//                    }
                }
            }
        }
    }

    companion object {
        private const val LOADING_TEXT_HEIGHT = 20f
//        private const val LOADING_BAR_HEIGHT = 20f
//        private const val LOADING_REASON_HEIGHT = 20f
    }
}