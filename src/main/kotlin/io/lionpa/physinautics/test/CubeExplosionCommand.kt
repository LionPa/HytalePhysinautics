package io.lionpa.physinautics.test

import com.hypixel.hytale.component.AddReason
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.math.shape.Box
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand
import com.hypixel.hytale.server.core.entity.entities.BlockEntity
import com.hypixel.hytale.server.core.modules.entity.component.BoundingBox
import com.hypixel.hytale.server.core.modules.entity.component.PropComponent
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent
import com.hypixel.hytale.server.core.modules.entity.hitboxcollision.HitboxCollision
import com.hypixel.hytale.server.core.modules.entity.hitboxcollision.HitboxCollisionConfig
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import `fun`.hygames.kotlinutils.BLOCK_ENTITY
import `fun`.hygames.kotlinutils.BOUNDING_BOX
import `fun`.hygames.kotlinutils.HITBOX_COLLISION
import `fun`.hygames.kotlinutils.TRANSFORM
import `fun`.hygames.kotlinutils.UUID_COMPONENT
import `fun`.hygames.kotlinutils.get
import `fun`.hygames.kotlinutils.set
import `fun`.hygames.kotlinutils.toRotation
import `fun`.hygames.kotlinutils.vec3
import io.lionpa.physinautics.PhysicalObjectComponent
import io.lionpa.physinautics.Physinautics
import org.joml.Vector3d

class CubeExplosionCommand : AbstractPlayerCommand("cube_explode", "Spawns an exploding cube of physics objects") {
    override fun execute(
        context: CommandContext,
        store: Store<EntityStore>,
        ref: Ref<EntityStore>,
        player: PlayerRef,
        world: World
    ) {
        val pos = ref[TRANSFORM]?.position ?: return

        val startX = pos.x
        val startY = pos.y + 5.0
        val startZ = pos.z

        for (x in 0 until 10) {
            for (y in 0 until 10) {
                for (z in 0 until 10) {
                    val blockPos = Vector3d(
                        (startX + x - 2.0),
                        (startY + y),
                        (startZ + z - 2.0)
                    )

                    spawn(blockPos, store)
                }
            }
        }
    }

    fun spawn(blockPos: Vector3d, store: Store<EntityStore>) {
        val holder = EntityStore.REGISTRY.newHolder()

        holder[TRANSFORM] = TransformComponent(blockPos, vec3(0, 0, 0).toRotation())
        holder[BLOCK_ENTITY] = BlockEntity("Soil_Grass_Full")
        holder[BOUNDING_BOX] = BoundingBox(Box.UNIT)

        holder[Physinautics.PHYSICAL_OBJECT] = PhysicalObjectComponent()

        holder[NetworkId.getComponentType()] = NetworkId(store.externalData.takeNextNetworkId())
        holder[PropComponent.getComponentType()] = PropComponent()

        holder.ensureComponent(UUID_COMPONENT)

        val config = HitboxCollisionConfig.getAssetMap().getAsset("RotatedCollision")!!
        holder[HITBOX_COLLISION] = HitboxCollision(config)

        store.addEntity(holder, AddReason.SPAWN)
    }
}