package io.lionpa.physinautics

import com.hypixel.hytale.component.AddReason
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.math.shape.Box
import com.hypixel.hytale.server.core.asset.type.model.config.Model
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset
import com.hypixel.hytale.server.core.command.commands.debug.component.hitboxcollision.HitboxCollisionAddCommand
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand
import com.hypixel.hytale.server.core.entity.entities.BlockEntity
import com.hypixel.hytale.server.core.modules.entity.component.BoundingBox
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent
import com.hypixel.hytale.server.core.modules.entity.component.PropComponent
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent
import com.hypixel.hytale.server.core.modules.entity.hitboxcollision.HitboxCollision
import com.hypixel.hytale.server.core.modules.entity.hitboxcollision.HitboxCollisionConfig
import com.hypixel.hytale.server.core.modules.entity.system.ModelSystems
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import `fun`.hygames.kotlinutils.*
import `fun`.hygames.kotlinutils.codeInitialization.*
import io.lionpa.physinautics.Physinautics.Companion.PHYSICAL_OBJECT

class TestHardCollisionsCommand: AbstractPlayerCommand {

    constructor() : super("coltest", "Command for developers") { }

    override fun execute(
        p0: CommandContext,
        p1: Store<EntityStore?>,
        p2: Ref<EntityStore?>,
        p3: PlayerRef,
        p4: World
    ) {
        TestHardCollisions.spawn(p3)
    }

}

object TestHardCollisions {

    @Run(on = RunOn.TEST, name = "Hi")
    fun spawn(player: PlayerRef){
        val pos = player[TRANSFORM]?.position ?: return
        val world = player.world ?: return

        world {
            val store = world.entityStore.store

            val holder = EntityStore.REGISTRY.newHolder()

            holder[TRANSFORM] = TransformComponent(pos, vec3(0,0,0).toRotation())
            holder[BLOCK_ENTITY] = BlockEntity("Soil_Grass_Full")
            holder[BOUNDING_BOX] = BoundingBox(Box.UNIT)

            holder[PHYSICAL_OBJECT] = PhysicalObjectComponent()

            holder[NetworkId.getComponentType()] = NetworkId(store.getExternalData().takeNextNetworkId())
            holder[PropComponent.getComponentType()] = PropComponent()

            holder.ensureComponent(UUID_COMPONENT)

            val config = HitboxCollisionConfig.getAssetMap().getAsset("RotatedCollision")!!
            holder[HITBOX_COLLISION] = HitboxCollision(config)

            store.addEntity(holder, AddReason.SPAWN)
        }
    }
}