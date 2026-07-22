package io.lionpa.physinautics

import com.hypixel.hytale.codec.builder.BuilderCodec
import com.hypixel.hytale.component.Component
import com.hypixel.hytale.component.ComponentType
import com.hypixel.hytale.server.core.entity.Frozen
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import io.lionpa.physinautics.rust.RigidBody
import java.util.function.Supplier

class PhysicalObjectComponent: Component<EntityStore> {

    var physicsBody: RigidBody? = null
    var mass: Float = 1.0f

    companion object {
        fun getComponentType(): ComponentType<EntityStore, PhysicalObjectComponent> {
            return Physinautics.physicalObjectComponentType
        }

        val CODEC = BuilderCodec.builder(PhysicalObjectComponent::class.java, ::PhysicalObjectComponent).build()
    }

    override fun clone(): PhysicalObjectComponent {
        val cloned = PhysicalObjectComponent()
        cloned.mass = this.mass
        return cloned
    }
}