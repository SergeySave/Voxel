package com.sergeysav.voxel.client.block

import com.sergeysav.voxel.client.resource.texture.TextureResource
import com.sergeysav.voxel.common.block.Block
import com.sergeysav.voxel.common.block.state.BlockState

/**
 * @author sergeys
 *
 * @constructor Creates a new SolidBlockMesher
 */
open class LeavesBlockMesher<B : Block<out S>, S : BlockState>(
    texture: TextureResource
) : RandomizedSolidBlockMesher<B, S>(texture) {

    override val opaque: Boolean = false
}