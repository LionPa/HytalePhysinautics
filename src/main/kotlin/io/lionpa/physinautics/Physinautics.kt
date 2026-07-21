package io.lionpa.physinautics

import com.hypixel.hytale.component.ComponentType
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent
import com.hypixel.hytale.server.core.plugin.JavaPlugin
import com.hypixel.hytale.server.core.plugin.JavaPluginInit
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import `fun`.hygames.kotlinutils.codeInitialization.CodeInitializer

class Physinautics(init: JavaPluginInit): JavaPlugin(init) {

    companion object {
        private lateinit var instance: Physinautics

        fun get(): Physinautics {
            return instance
        }

        val PHYSICAL_OBJECT
            get() = instance.physicalObjectComponentType
    }

    lateinit var physicalObjectComponentType: ComponentType<EntityStore, PhysicalObjectComponent>

    override fun setup() {
        instance = this
        PhysicsWorld.init()
        physicalObjectComponentType = entityStoreRegistry.registerComponent(PhysicalObjectComponent::class.java, "PhysicalObject", PhysicalObjectComponent.CODEC)
        commandRegistry.registerCommand(TestHardCollisionsCommand())
        commandRegistry.registerCommand(CubeExplosionCommand())
        entityStoreRegistry.registerSystem(PhysicsStepSystem)
        entityStoreRegistry.registerSystem(PhysicsSystem)
        chunkStoreRegistry.registerSystem(PhysicsBlockColliderSystem)
        CodeInitializer.addPlugin(this)
    }
}