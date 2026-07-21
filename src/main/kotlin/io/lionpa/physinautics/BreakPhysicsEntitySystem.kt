package io.lionpa.physinautics

import com.hypixel.hytale.component.ArchetypeChunk
import com.hypixel.hytale.component.CommandBuffer
import com.hypixel.hytale.component.RemoveReason
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.component.query.Query
import com.hypixel.hytale.server.core.modules.entity.damage.Damage
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import `fun`.hygames.kotlinutils.get

object BreakPhysicsEntitySystem: DamageEventSystem() {
    override fun handle(
        id: Int,
        chunk: ArchetypeChunk<EntityStore>,
        store: Store<EntityStore>,
        buffer: CommandBuffer<EntityStore>,
        damage: Damage
    ) {
        val physicalObject = chunk[id, PhysicalObjectComponent.getComponentType()]!!

        physicalObject.physicsBody?.remove()

        buffer.removeEntity(chunk.getReferenceTo(id), RemoveReason.REMOVE)
    }

    override fun getQuery(): Query<EntityStore> {
        return PhysicalObjectComponent.getComponentType()
    }
}