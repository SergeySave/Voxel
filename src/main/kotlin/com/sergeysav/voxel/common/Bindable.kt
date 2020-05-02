package com.sergeysav.voxel.common

/**
 * @author sergeys
 */
interface Bindable {
    fun bind()
    fun unbind()
}

inline fun Bindable.bound(inner: () -> Unit) {
    try {
        bind()
        inner()
    } finally {
        unbind()
    }
}

inline fun bound(vararg binables: Bindable, inner: () -> Unit) {
    var i = 0
    try {
        while (i < binables.size) {
            binables[i++].bind()
        }
        inner()
    } finally {
        while (i > 0) {
            binables[--i].unbind()
        }
    }
}