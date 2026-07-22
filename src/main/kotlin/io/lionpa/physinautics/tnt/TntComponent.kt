package io.lionpa.physinautics.tnt

import com.hypixel.hytale.codec.builder.BuilderCodec
import com.hypixel.hytale.codec.codecs.simple.IntegerCodec
import com.hypixel.hytale.component.Component
import com.hypixel.hytale.component.ComponentType
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import `fun`.hygames.kotlinutils.*
import io.lionpa.physinautics.Physinautics

class TntComponent: Component<EntityStore> {

    var fuseLeft: Int = 80

    override fun clone(): Component<EntityStore> {
        val clone = TntComponent()
        clone.fuseLeft = fuseLeft
        return clone
    }

    companion object {

        fun getComponentType(): ComponentType<EntityStore, TntComponent> {
            return Physinautics.tntComponentType
        }

        val CODEC = BuilderCodec.builder(TntComponent::class.java, ::TntComponent)
            .append("Fuse", IntegerCodec.INTEGER, {component, fuse -> component.fuseLeft = fuse}, { component: TntComponent -> component.fuseLeft})
            .add()
            .build()
    }
}