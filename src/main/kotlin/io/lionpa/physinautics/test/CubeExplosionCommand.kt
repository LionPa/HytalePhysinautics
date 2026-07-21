package io.lionpa.physinautics.test

import com.hypixel.hytale.component.AddReason
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.math.shape.Box
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand
import com.hypixel.hytale.server.core.entity.UUIDComponent
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
import java.util.UUID
import java.util.concurrent.TimeUnit

class CubeExplosionCommand : AbstractPlayerCommand("cube_explode", "Spawns an exploding cube of physics objects") {
    override fun execute(
        p0: CommandContext,
        p1: Store<EntityStore?>,
        p2: Ref<EntityStore?>,
        p3: PlayerRef,
        p4: World
    ) {
        val pos = p3[TRANSFORM]?.position ?: return
        val world = p4
        val store = world.entityStore.store

        val physObjects = mutableListOf<PhysicalObjectComponent>()
        val uuids = mutableListOf<UUID>()

        val startX = pos.x
        val startY = pos.y + 5.0
        val startZ = pos.z

        for (x in 0 until 10) {
            for (y in 0 until 10) {
                for (z in 0 until 10) {
                    val holder = EntityStore.REGISTRY.newHolder()

                    val blockPos = Vector3d(
                        (startX + x - 2.0),
                        (startY + y),
                        (startZ + z - 2.0)
                    )

                    holder[TRANSFORM] = TransformComponent(blockPos, vec3(0, 0, 0).toRotation())
                    holder[BLOCK_ENTITY] = BlockEntity("Soil_Grass_Full")
                    holder[BOUNDING_BOX] = BoundingBox(Box.UNIT)

                    val physObj = PhysicalObjectComponent()
                    physObjects.add(physObj)
                    holder[Physinautics.PHYSICAL_OBJECT] = physObj

                    holder[NetworkId.getComponentType()] = NetworkId(store.externalData.takeNextNetworkId())
                    holder[PropComponent.getComponentType()] = PropComponent()

                    val uuid = UUID.randomUUID()
                    holder[UUID_COMPONENT] = UUIDComponent(uuid)
                    uuids.add(uuid)

                    val config = HitboxCollisionConfig.getAssetMap().getAsset("RotatedCollision")!!
                    holder[HITBOX_COLLISION] = HitboxCollision(config)

                    store.addEntity(holder, AddReason.SPAWN)
                }
            }
        }

        // Apply explosion force after 1 tick (when physics bodies are created)
        world.scheduleAfter(Runnable {
            for ((index, physObj) in physObjects.withIndex()) {
                val body = physObj.physicsBody ?: continue

                // Calculate explosion force direction from the center of the cube
                val centerOffsetX = (index / 100) - 2.0
                val centerOffsetY = ((index / 10) % 10) - 2.0
                val centerOffsetZ = (index % 10) - 2.0

                // Normalize and scale force
                val length = Math.sqrt(centerOffsetX * centerOffsetX + centerOffsetY * centerOffsetY + centerOffsetZ * centerOffsetZ)
                if (length > 0.0) {
                    val forceScale = 3.0 / length // Super weak explosion force magnitude
                   // body.applyForce(
                   //     centerOffsetX * forceScale,
                   //     centerOffsetY * forceScale + 2.0, // tiny upward boost
                   //     centerOffsetZ * forceScale
                   // )
                }
            }
        }, 100, TimeUnit.MILLISECONDS)

        // Delete all entities after 15 seconds
            //world.scheduleAfter(Runnable {
       //    for (uuid in uuids) {
       //        val ref = world.getEntityRef(uuid)
                    //        if (ref != null && ref.isValid) {
       //            store.removeEntity(ref, RemoveReason.REMOVE)
                            //        }
                    //    }
                //}, 15, TimeUnit.SECONDS)
    }
}