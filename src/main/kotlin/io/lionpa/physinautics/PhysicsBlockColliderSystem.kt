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

                    if (exist) continue

                    val ref = chunkStore.getChunkSectionReference(chunkX + x, chunkY + y, chunkZ + z) ?: continue

                    val blocks = ref[BlockSection.getComponentType()]!!
                    setChunk(physicsWorld, blocks, localX, localY, localZ)

                }
            }
        }
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
        return PHYSICAL_OBJECT
    }
}
