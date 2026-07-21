package io.lionpa.physinautics

import com.hypixel.hytale.component.ArchetypeChunk
import com.hypixel.hytale.component.CommandBuffer
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.component.query.Query
import com.hypixel.hytale.component.system.tick.EntityTickingSystem
import com.hypixel.hytale.math.util.ChunkUtil
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection
import com.hypixel.hytale.server.core.universe.world.chunk.section.ChunkSection
import `fun`.hygames.kotlinutils.get
import kotlin.math.max
import kotlin.math.min
import java.util.function.BiConsumer
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import io.lionpa.physinautics.Physinautics.Companion.PHYSICAL_OBJECT
import `fun`.hygames.kotlinutils.TRANSFORM

object PhysicsBlockColliderSystem : EntityTickingSystem<ChunkStore>() {
    
    override fun tick(
        dt: Float,
        index: Int,
        chunk: ArchetypeChunk<ChunkStore>,
        store: Store<ChunkStore?>,
        buffer: CommandBuffer<ChunkStore?>
    ) {
        val blocks = chunk[index, BlockSection.getComponentType()]!!
        val section = chunk[index, ChunkSection.getComponentType()]!!

        val worldChunk = buffer[section.chunkColumnReference, WorldChunk.getComponentType()]!!
        val world = worldChunk.world
        val entityStore = world.entityStore.store

        val chunkGlobalX = worldChunk.x * 32
        val chunkGlobalZ = worldChunk.z * 32
        val chunkGlobalY = section.y * 32

        entityStore.forEachChunk(
            PHYSICAL_OBJECT,
            BiConsumer { entityChunk: ArchetypeChunk<EntityStore>, _ ->
                for (i in 0 until entityChunk.size()) {
                    val transform = entityChunk.getComponent(i, TRANSFORM) ?: continue
                    val pos = transform.position

                val minX = (pos.x - 2).toInt()
                val maxX = (pos.x + 2).toInt()
                val minY = (pos.y - 2).toInt()
                val maxY = (pos.y + 2).toInt()
                val minZ = (pos.z - 2).toInt()
                val maxZ = (pos.z + 2).toInt()

                val intersectMinX = max(minX, chunkGlobalX)
                val intersectMaxX = min(maxX, chunkGlobalX + 31)
                val intersectMinY = max(minY, chunkGlobalY)
                val intersectMaxY = min(maxY, chunkGlobalY + 31)
                val intersectMinZ = max(minZ, chunkGlobalZ)
                val intersectMaxZ = min(maxZ, chunkGlobalZ + 31)

                if (!(intersectMinX <= intersectMaxX && intersectMinY <= intersectMaxY && intersectMinZ <= intersectMaxZ)) continue
                
                for (x in intersectMinX..intersectMaxX) {
                    for (y in intersectMinY..intersectMaxY) {
                        for (z in intersectMinZ..intersectMaxZ) {
                            val localX = x - chunkGlobalX
                            val localY = y - chunkGlobalY
                            val localZ = z - chunkGlobalZ

                            val blockId = blocks.get(localX, localY, localZ)

                            // ID 0 is Air
                            if (blockId == 0) continue

                            val bp = PhysicsWorldColliderSync.BlockPos(x, y, z)
                            PhysicsWorldColliderSync.currentTickBlocks.add(bp)

                            if (PhysicsWorldColliderSync.activeBlocks.containsKey(bp)) continue

                            // Block exists, assume solid
                            val collider = PhysicsWorld.engine.createStaticBox(
                                x + 0.5, y + 0.5, z + 0.5, 1.0, 1.0, 1.0
                            )
                            PhysicsWorldColliderSync.activeBlocks[bp] = collider
                        }
                    }
                }
            }
        })
    }

    override fun getQuery(): Query<ChunkStore> {
        return Query.and(BlockSection.getComponentType(), ChunkSection.getComponentType())
    }
}
