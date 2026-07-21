package io.lionpa.physinautics

import com.hypixel.hytale.component.ArchetypeChunk
import com.hypixel.hytale.component.CommandBuffer
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.component.query.Query
import com.hypixel.hytale.component.system.tick.EntityTickingSystem
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import `fun`.hygames.kotlinutils.*
import io.lionpa.kotlinffm.byte
import io.lionpa.physinautics.Physinautics.Companion.PHYSICAL_OBJECT
import io.lionpa.physinautics.rust.PhysicalChunk
import io.lionpa.physinautics.rust.PhysicsWorld
import java.lang.foreign.Arena

object PhysicsBlockColliderSystem : EntityTickingSystem<EntityStore>() {
    
    override fun tick(
        dt: Float,
        index: Int,
        chunk: ArchetypeChunk<EntityStore>,
        store: Store<EntityStore>,
        buffer: CommandBuffer<EntityStore>
    ) {
        val physicsWorld = Physinautics.getWorld(store.externalData.world)

        val chunkStore = store.externalData.world.chunkStore
        val transform = chunk[index, TRANSFORM] ?: return

        val chunkX = kotlin.math.floor(transform.position.x / 32.0).toInt()
        val chunkY = kotlin.math.floor(transform.position.y / 32.0).toInt()
        val chunkZ = kotlin.math.floor(transform.position.z / 32.0).toInt()

        for (x in -1..1) {
            for (y in -1..1) {
                for (z in -1..1) {
                    val localX = chunkX + x
                    val localY = chunkY + y
                    val localZ = chunkZ + z

                    val exist = physicsWorld.hasChunk(localX, localY, localZ)

                    if (!exist) {
                        val ref = chunkStore.getChunkSectionReference(chunkX + x, chunkY + y, chunkZ + z) ?: continue

                        val blocks = ref[BlockSection.getComponentType()]!!
                        setChunk(physicsWorld, blocks, localX, localY, localZ)
                    }
                }
            }
        }



       //val blocks = chunk[index, BlockSection.getComponentType()]!!
       //val section = chunk[index, ChunkSection.getComponentType()]!!

       //val worldChunk = buffer[section.chunkColumnReference, WorldChunk.getComponentType()]!!
       //val world = worldChunk.world
       //val entityStore = world.entityStore.store

       //val chunkGlobalX = worldChunk.x * 32
       //val chunkGlobalZ = worldChunk.z * 32
       //val chunkGlobalY = section.y * 32

       //entityStore.forEachChunk(
       //    PHYSICAL_OBJECT,
       //    BiConsumer { entityChunk: ArchetypeChunk<EntityStore>, _ ->
       //        for (i in 0 until entityChunk.size()) {
       //            val transform = entityChunk.getComponent(i, TRANSFORM) ?: continue
       //            val pos = transform.position

       //        val minX = (pos.x - 2).toInt()
       //        val maxX = (pos.x + 2).toInt()
       //        val minY = (pos.y - 2).toInt()
       //        val maxY = (pos.y + 2).toInt()
       //        val minZ = (pos.z - 2).toInt()
       //        val maxZ = (pos.z + 2).toInt()

       //        val intersectMinX = max(minX, chunkGlobalX)
       //        val intersectMaxX = min(maxX, chunkGlobalX + 31)
       //        val intersectMinY = max(minY, chunkGlobalY)
       //        val intersectMaxY = min(maxY, chunkGlobalY + 31)
       //        val intersectMinZ = max(minZ, chunkGlobalZ)
       //        val intersectMaxZ = min(maxZ, chunkGlobalZ + 31)

       //        if (!(intersectMinX <= intersectMaxX && intersectMinY <= intersectMaxY && intersectMinZ <= intersectMaxZ)) continue
       //
       //        for (x in intersectMinX..intersectMaxX) {
       //            for (y in intersectMinY..intersectMaxY) {
       //                for (z in intersectMinZ..intersectMaxZ) {
       //                    val localX = x - chunkGlobalX
       //                    val localY = y - chunkGlobalY
       //                    val localZ = z - chunkGlobalZ

       //                    val blockId = blocks.get(localX, localY, localZ)

       //                    // ID 0 is Air
       //                    if (blockId == 0) continue

       //                    val bp = PhysicsWorldColliderSync.BlockPos(x, y, z)
       //                    PhysicsWorldColliderSync.currentTickBlocks.add(bp)

       //                    if (PhysicsWorldColliderSync.activeBlocks.containsKey(bp)) continue

       //                    // Block exists, assume solid
       //                    val collider = PhysicsWorld.engine.createStaticBox(
       //                        x + 0.5, y + 0.5, z + 0.5, 1.0, 1.0, 1.0
       //                    )
       //                    PhysicsWorldColliderSync.activeBlocks[bp] = collider
       //                }
       //            }
       //        }
       //    }
       //})
    }

    private fun setChunk(physicsWorld: PhysicsWorld, blocks: BlockSection, chunkX: Int, chunkY: Int, chunkZ: Int){
        val arena = Arena.ofConfined()
        val segment = arena.allocate(4096L)
        
        for (index in 0 until 32768) {
            val x = index % 32
            val y = (index / 32) % 32
            val z = index / 1024
            
            val blockId = blocks.get(x, y, z)
            if (blockId != 0) {
                val byteIndex = (index / 8).toLong()
                val bitIndex = index % 8
                
                val currentByte = segment.get(byte, byteIndex).toInt()
                val newByte = (currentByte or (1 shl bitIndex)).toByte()
                segment.set(byte, byteIndex, newByte)
            }
        }
        
        val chunk = PhysicalChunk(chunkX, chunkY, chunkZ, arena, segment)
        physicsWorld.setChunk(chunk)
    }

    override fun getQuery(): Query<EntityStore> {
        return Query.and(PHYSICAL_OBJECT)
    }
}
