package io.lionpa.physinautics

import com.hypixel.hytale.component.ComponentType
import com.hypixel.hytale.server.core.plugin.JavaPlugin
import com.hypixel.hytale.server.core.plugin.JavaPluginInit
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import `fun`.hygames.kotlinutils.codeInitialization.CodeInitializer
import io.lionpa.physinautics.rust.PhysicsWorld
import io.lionpa.physinautics.test.CubeExplosionCommand
import io.lionpa.physinautics.test.TestHardCollisionsCommand

class Physinautics(init: JavaPluginInit): JavaPlugin(init) {

    companion object {
        private lateinit var instance: Physinautics

        private val worlds = HashMap<World, PhysicsWorld>()

        fun getWorld(hytaleWorld: World): PhysicsWorld {
            if (worlds.containsKey(hytaleWorld)) return worlds[hytaleWorld]!!

            val world = PhysicsWorld()
            world.init()
            worlds[hytaleWorld] = world
            return world
        }

        fun get(): Physinautics {
            return instance
        }

        val PHYSICAL_OBJECT
            get() = instance.physicalObjectComponentType
    }

    lateinit var physicalObjectComponentType: ComponentType<EntityStore, PhysicalObjectComponent>

    override fun setup() {
        instance = this

        physicalObjectComponentType = entityStoreRegistry.registerComponent(PhysicalObjectComponent::class.java, "PhysicalObject", PhysicalObjectComponent.CODEC)
        commandRegistry.registerCommand(TestHardCollisionsCommand())
        commandRegistry.registerCommand(CubeExplosionCommand())
        entityStoreRegistry.registerSystem(PhysicsStepSystem)
        entityStoreRegistry.registerSystem(PhysicsSystem)
        entityStoreRegistry.registerSystem(BreakPhysicsEntitySystem)
        entityStoreRegistry.registerSystem(PhysicsBlockColliderSystem)
        CodeInitializer.addPlugin(this)
    }
}