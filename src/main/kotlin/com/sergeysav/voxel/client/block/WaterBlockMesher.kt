package com.sergeysav.voxel.client.block

import com.sergeysav.voxel.client.resource.texture.TextureResource
import com.sergeysav.voxel.common.block.Block
import com.sergeysav.voxel.common.block.state.BlockState

/**
 * @author sergeys
 *
 * @constructor Creates a new WaterBlockMesher
 */
open class WaterBlockMesher<B : Block<out S>, S : BlockState>(
    texture: TextureResource
) : SolidBlockMesher<B, S>(texture) {

    override val opaque: Boolean = false
    override val translucent: Boolean = true
    override val doubleRender: Boolean = false
}