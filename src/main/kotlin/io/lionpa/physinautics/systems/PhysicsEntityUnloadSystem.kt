package io.lionpa.physinautics.systems

import com.hypixel.hytale.component.AddReason
import com.hypixel.hytale.component.CommandBuffer
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.RemoveReason
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.component.query.Query
import com.hypixel.hytale.component.system.RefSystem
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import io.lionpa.physinautics.Physinautics.Companion.PHYSICAL_OBJECT

object PhysicsEntityUnloadSystem : RefSystem<EntityStore>() {
    override fun getQuery(): Query<EntityStore> = PHYSICAL_OBJECT

    override fun onEntityAdded(
        ref: Ref<EntityStore>,
        reason: AddReason,
        store: Store<EntityStore>,
        commandBuffer: CommandBuffer<EntityStore>
    ) {
        // Handled by PhysicsSystem
    }

    override fun onEntityRemove(
        ref: Ref<EntityStore>,
        reason: RemoveReason,
        store: Store<EntityStore>,
        commandBuffer: CommandBuffer<EntityStore>
    ) {
        val physObj = commandBuffer.getComponent(ref, PHYSICAL_OBJECT)
        physObj?.physicsBody?.remove()
        physObj?.physicsBody = null
    }
}
