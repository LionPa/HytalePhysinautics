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
import io.lionpa.physinautics.Physinautics.Companion.PHYSICAL_OBJECT

object BreakPhysicsEntitySystem: DamageEventSystem() {
    override fun handle(
        id: Int,
        chunk: ArchetypeChunk<EntityStore>,
        store: Store<EntityStore>,
        buffer: CommandBuffer<EntityStore>,
        damage: Damage
    ) {
        val physicalObject = chunk[id, PHYSICAL_OBJECT]!!

        physicalObject.physicsBody?.remove()

        buffer.removeEntity(chunk.getReferenceTo(id), RemoveReason.REMOVE)
    }

    override fun getQuery(): Query<EntityStore> {
        return PHYSICAL_OBJECT
    }
}