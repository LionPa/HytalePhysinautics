package io.lionpa.physinautics.tnt

import com.hypixel.hytale.assetstore.map.BlockTypeAssetMap
import com.hypixel.hytale.component.AddReason
import com.hypixel.hytale.component.ArchetypeChunk
import com.hypixel.hytale.component.CommandBuffer
import com.hypixel.hytale.component.RemoveReason
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.component.query.Query
import com.hypixel.hytale.component.system.tick.EntityTickingSystem
import com.hypixel.hytale.function.predicate.TriIntObjPredicate
import com.hypixel.hytale.math.block.BlockSphereUtil
import com.hypixel.hytale.math.shape.Box
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType
import com.hypixel.hytale.server.core.entity.entities.BlockEntity
import com.hypixel.hytale.server.core.modules.entity.component.BoundingBox
import com.hypixel.hytale.server.core.modules.entity.component.PropComponent
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent
import com.hypixel.hytale.server.core.modules.entity.hitboxcollision.HitboxCollision
import com.hypixel.hytale.server.core.modules.entity.hitboxcollision.HitboxCollisionConfig
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import `fun`.hygames.kotlinutils.*
import io.lionpa.physinautics.PhysicalObjectComponent
import io.lionpa.physinautics.Physinautics.Companion.PHYSICAL_OBJECT
import it.unimi.dsi.fastutil.ints.IntArrayList
import org.joml.Vector3d

object TntSystem: EntityTickingSystem<EntityStore>() {
    override fun tick(
        dt: Float,
        index: Int,
        chunk: ArchetypeChunk<EntityStore>,
        store: Store<EntityStore>,
        buffer: CommandBuffer<EntityStore>
    ) {
        val tnt = chunk[index, TntComponent.getComponentType()]!!

        val list = IntArrayList()

        if (tnt.fuseLeft < 0) {
            // TODO Explode
            val pos = chunk[index, TRANSFORM]!!.position

            val world = store.externalData.world

            BlockSphereUtil.forEachBlock(
                pos.x.toInt(),
                pos.y.toInt(),
                pos.z.toInt(),
                10,
                null
            ) { x, y, z, var4 ->
                list.add(x)
                list.add(y)
                list.add(z)
                list.add(0)
                true
            }

            world {
                for (i in 0..<list.size / 4) {
                    val x = list[i * 4]
                    val y = list[i * 4 + 1]
                    val z = list[i * 4 + 2]

                    val chunkRef = world.chunkStore.getChunkSectionReferenceAtBlock(x, y, z) ?: continue

                    val blocks = chunkRef[BlockSection.getComponentType()] ?: continue
                    val blockId = blocks.get(x, y, z)

                    blocks.set(x, y, z, 0, 0, 0)

                    list[i * 4 + 3] = blockId
                }

                for (i in 0..<list.size / 4) {
                    val x = list[i * 4]
                    val y = list[i * 4 + 1]
                    val z = list[i * 4 + 2]
                    val blockId = list[i * 4 + 3]

                    val blockType = BlockType.getAssetMap().getAsset(blockId)!!

                    spawn(store, Vector3d(x.toDouble(), y.toDouble(), z.toDouble()), blockType.id)
                }
            }

            chunk[index, PHYSICAL_OBJECT]?.physicsBody?.remove()

            buffer.removeEntity(chunk.getReferenceTo(index), RemoveReason.REMOVE)

            return
        }

        tnt.fuseLeft -= 1
    }

    fun spawn(store: Store<EntityStore>, pos: Vector3d, blockType: String){
        val holder = EntityStore.REGISTRY.newHolder()

        holder[TRANSFORM] = TransformComponent(pos, vec3(0,0,0).toRotation())
        holder[BLOCK_ENTITY] = BlockEntity(blockType)
        holder[BOUNDING_BOX] = BoundingBox(Box.UNIT)

        holder[PHYSICAL_OBJECT] = PhysicalObjectComponent()

        holder[NetworkId.getComponentType()] = NetworkId(store.getExternalData().takeNextNetworkId())
        holder[PropComponent.getComponentType()] = PropComponent()

        holder.ensureComponent(UUID_COMPONENT)

        val config = HitboxCollisionConfig.getAssetMap().getAsset("RotatedCollision")!!
        holder[HITBOX_COLLISION] = HitboxCollision(config)

        store.addEntity(holder, AddReason.SPAWN)
    }

    override fun getQuery(): Query<EntityStore> {
        return Query.and(TntComponent.getComponentType(), TRANSFORM)
    }
}