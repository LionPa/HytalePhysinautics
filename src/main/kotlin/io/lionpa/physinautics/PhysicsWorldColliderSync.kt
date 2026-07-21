package io.lionpa.physinautics

import io.lionpa.physinautics.api.IStaticCollider
object PhysicsWorldColliderSync {
    data class BlockPos(val x: Int, val y: Int, val z: Int)

    val activeBlocks = mutableMapOf<BlockPos, IStaticCollider>()
    val currentTickBlocks = mutableSetOf<BlockPos>()

    fun beginSync() {
        currentTickBlocks.clear()
    }

    fun endSync() {
        val toRemove = activeBlocks.keys.filter { it !in currentTickBlocks }
        for (bp in toRemove) {
            activeBlocks[bp]?.let { PhysicsWorld.engine.removeStaticCollider(it) }
            activeBlocks.remove(bp)
        }
    }
}
