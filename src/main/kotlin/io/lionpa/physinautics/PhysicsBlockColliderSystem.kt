package io.lionpa.physinautics

import com.hypixel.hytale.component.ArchetypeChunk
import com.hypixel.hytale.component.CommandBuffer
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.component.query.Query
import com.hypixel.hytale.component.system.tick.EntityTickingSystem
import com.hypixel.hytale.math.block.BlockUtil
import com.hypixel.hytale.math.util.ChunkUtil
import com.hypixel.hytale.server.core.asset.type.blockhitbox.BlockBoundingBoxes
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import `fun`.hygames.kotlinutils.*
import io.lionpa.kotlinffm.byte
import io.lionpa.kotlinffm.int
import io.lionpa.physinautics.Physinautics.Companion.PHYSICAL_OBJECT
import io.lionpa.physinautics.rust.PhysicalChunk
import io.lionpa.physinautics.rust.PhysicsWorld
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment

object PhysicsBlockColliderSystem : EntityTickingSystem<EntityStore>() {
    
    private val requiredShapes = HashSet<Int>()
    private val specials = HashMap<Long, IntArrayList>()

    private class IntArrayList(capacity: Int = 16) {
        var data = IntArray(capacity)
        var size = 0
        
        fun add(element: Int) {
            if (size == data.size) {
                data = data.copyOf(data.size * 2)
            }
            data[size++] = element
        }

        fun clear() {
            size = 0
        }
    }
    
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

                    if (physicsWorld.hasChunk(localX, localY, localZ)) continue

                    val ref = chunkStore.getChunkSectionReference(localX, localY, localZ) ?: continue
                    val blocks = ref[BlockSection.getComponentType()] ?: continue
                    
                    setChunk(physicsWorld, blocks, localX, localY, localZ)
                }
            }
        }
    }

    private fun setChunk(physicsWorld: PhysicsWorld, blocks: BlockSection, chunkX: Int, chunkY: Int, chunkZ: Int) {
        physicsWorld.removeChunk(chunkX, chunkY, chunkZ)

        requiredShapes.clear()
        specials.values.forEach { it.clear() }
        
        val bitmaskArray = ByteArray(4096)
        
        for (i in 0 until 32768) {
            val x = i % 32
            val y = (i / 32) % 32
            val z = i / 1024

            val index = ChunkUtil.indexBlock(x, y, z)
            val blockId = blocks.get(index)

            if (blockId == 0) continue
            val blockType = BlockType.getAssetMap().getAsset(blockId)

            if (blockType != null) {
                if (blockType.hitboxType == "Full") {
                    val byteIndex = i / 8
                    val bitIndex = i % 8
                    bitmaskArray[byteIndex] = (bitmaskArray[byteIndex].toInt() or (1 shl bitIndex)).toByte()
                } else {
                    val shapeId = blockType.hitboxTypeIndex
                    val materialType = 0

                    requiredShapes.add(shapeId)

                    val layerKey = (shapeId.toLong() shl 32) or materialType.toLong()
                    val list = specials.getOrPut(layerKey) { IntArrayList() }

                    val rotation = blocks.getRotationIndex(index)
                    val packed =
                        (x and 0xFF) or ((y and 0xFF) shl 8) or ((z and 0xFF) shl 16) or ((rotation and 0xFF) shl 24)
                    list.add(packed)
                }
            }

        }
        
        if (requiredShapes.isNotEmpty()) {
            val requiredArray = requiredShapes.toIntArray()
            val bitmask = physicsWorld.checkShapes(requiredArray)

            for (i in requiredArray.indices) {
                val byteIdx = i / 8
                val bitIdx = i % 8
                val hasShape = (bitmask[byteIdx].toInt() and (1 shl bitIdx)) != 0
                
                if (!hasShape) {
                    val shapeId = requiredArray[i]
                    val hitboxAsset = BlockBoundingBoxes.getAssetMap().getAsset(shapeId)
                    
                    if (hitboxAsset != null) {
                        val variantBoxes = hitboxAsset.get(0)
                        val boxes = variantBoxes.detailBoxes
                        if (boxes.isNotEmpty()) {
                            val floatArray = FloatArray(boxes.size * 6)
                                for (b in boxes.indices) {
                                    val box = boxes[b]
                                    floatArray[b * 6] = box.min.x.toFloat()
                                    floatArray[b * 6 + 1] = box.min.y.toFloat()
                                    floatArray[b * 6 + 2] = box.min.z.toFloat()
                                    floatArray[b * 6 + 3] = box.max.x.toFloat()
                                    floatArray[b * 6 + 4] = box.max.y.toFloat()
                                    floatArray[b * 6 + 5] = box.max.z.toFloat()
                                }
                            physicsWorld.addShape(shapeId, floatArray)
                        }
                    }
                }
            }
        }
        
        val arena = Arena.ofConfined()
        val segment = arena.allocate(4096L)
        MemorySegment.copy(bitmaskArray, 0, segment, byte, 0, 4096)
        
        val chunk = PhysicalChunk(chunkX, chunkY, chunkZ, arena, segment)
        physicsWorld.setChunk(chunk)

        for ((layerKey, list) in specials) {
            if (list.size == 0) continue
            
            val shapeId = (layerKey ushr 32).toInt()
            val materialType = layerKey.toInt()
            
            val instancesSegment = arena.allocate(list.size * 4L)
            for (i in 0 until list.size) {
                instancesSegment.setAtIndex(int, i.toLong(), list.data[i])
            }
            
            physicsWorld.setChunkSpecial(
                chunkX, chunkY, chunkZ,
                shapeId, materialType, list.size, instancesSegment.address()
            )
        }
    }

    override fun getQuery(): Query<EntityStore> {
        return PHYSICAL_OBJECT
    }
}
