package io.lionpa.physinautics.systems

import com.hypixel.hytale.component.AddReason
import com.hypixel.hytale.component.CommandBuffer
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.RemoveReason
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.component.query.Query
import com.hypixel.hytale.component.system.RefSystem
import com.hypixel.hytale.server.core.universe.world.chunk.section.ChunkSection
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore
import io.lionpa.physinautics.Physinautics

object PhysicsChunkUnloadSystem : RefSystem<ChunkStore>() {
    override fun getQuery(): Query<ChunkStore> = ChunkSection.getComponentType()

    override fun onEntityAdded(
        ref: Ref<ChunkStore>,
        reason: AddReason,
        store: Store<ChunkStore>,
        commandBuffer: CommandBuffer<ChunkStore>
    ) {
    }

    override fun onEntityRemove(
        ref: Ref<ChunkStore>,
        reason: RemoveReason,
        store: Store<ChunkStore>,
        commandBuffer: CommandBuffer<ChunkStore>
    ) {
        val section = commandBuffer.getComponent(ref, ChunkSection.getComponentType()) ?: return
        val physicsWorld = Physinautics.getWorld(store.externalData.world)
        physicsWorld.removeChunk(section.x, section.y, section.z)
    }
}
