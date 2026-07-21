package io.lionpa.physinautics

import com.hypixel.hytale.component.Store
import com.hypixel.hytale.component.system.tick.TickingSystem
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore

object PhysicsStepSystem : TickingSystem<EntityStore>() {
    override fun tick(
        dt: Float,
        index: Int,
        store: Store<EntityStore?>
    ) {
        Physinautics.getWorld(store.externalData.world).step(dt)
    }
}
