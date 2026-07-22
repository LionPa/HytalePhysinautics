package io.lionpa.physinautics.rust

import io.lionpa.kotlinffm.*
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment

class PhysicsWorld {

    companion object {
        private val init = Rust.method(
            "rapier_init",
            long
        )

        private val rapier_set_sync_buffer = Rust.void(
            "rapier_set_sync_buffer",
            long,
            long,
            int
        )

        private val rapier_add_box = Rust.method(
            "rapier_add_box",
            int,
            long,
            float,
            float,
            float,
            float,
        )

        private val rapier_apply_impulse = Rust.void(
            "rapier_apply_impulse",
            long,
            int,
            float,
            float,
            float
        )

        private val rapier_step = Rust.void(
            "rapier_step",
            long,
            float
        )

        private val rapier_remove_body = Rust.void(
            "rapier_remove_body",
            long,
            int
        )

        private val has_chunk = Rust.method(
            "has_chunk",
            bool,
            long,
            int,
            int,
            int
        )

        private val set_chunk = Rust.void(
            "set_chunk",
            long,
            int,
            int,
            int,
            long
        )

        private val remove_chunk = Rust.void(
            "remove_chunk",
            long,
            int,
            int,
            int
        )

        private val set_chunk_special = Rust.void(
            "set_chunk_special",
            long, // world_ptr
            int,  // chunk_x
            int,  // chunk_y
            int,  // chunk_z
            int,  // shape_id (u32 -> int)
            int,  // material_type (u32 -> int)
            int,  // instances_count (u32 -> int)
            long  // instances_ptr
        )

        private val check_shapes_batch = Rust.void(
            "check_shapes_batch",
            long, // world_ptr
            long, // shape_ids_ptr
            int,  // count
            long  // out_bitmask_ptr
        )

        private val add_shape = Rust.void(
            "add_shape",
            long, // world_ptr
            int,  // shape_id
            int,  // boxes_count
            long  // boxes_ptr
        )
    }

    val arena = Arena.ofConfined()
    var worldPtr: Long = 0
    var syncBuffer: MemorySegment? = null
    val maxObjects = 10000

    val chunks = HashMap<Triple<Int, Int, Int>, PhysicalChunk>()

    fun init() {
        syncBuffer = arena.allocate(maxObjects * 7L * 4L)
        worldPtr = init.invokeExact() as Long
        setSyncBuffer(syncBuffer!!.address(), maxObjects)
    }

    fun close() {
        // TODO Close Rapier
        arena.close()
    }

    fun createRigidBody(x: Double, y: Double, z: Double, size: Double): RigidBody {
        val rustId = addBox(x.toFloat(), y.toFloat(), z.toFloat(), size.toFloat())
        return RigidBody(this, rustId)
    }

    fun setSyncBuffer(bufferPtr: Long, maxObjects: Int) {
        rapier_set_sync_buffer.invokeExact(worldPtr, bufferPtr, maxObjects)
    }

    fun addBox(x: Float, y: Float, z: Float, size: Float): Int {
        return rapier_add_box.invokeExact(worldPtr, x, y, z, size) as Int
    }

    fun applyImpulse(id: Int, fx: Float, fy: Float, fz: Float) {
        rapier_apply_impulse.invokeExact(worldPtr, id, fx, fy, fz)
    }

    fun step(dt: Float) {
        rapier_step.invokeExact(worldPtr, dt)
    }

    fun removeBody(id: Int) {
        rapier_remove_body.invokeExact(worldPtr, id)
    }

    fun hasChunk(chunkX: Int, chunkY: Int, chunkZ: Int): Boolean {
        return has_chunk.invokeExact(worldPtr, chunkX, chunkY, chunkZ) as Boolean
    }

    fun setChunk(chunk: PhysicalChunk) {
        val triple = Triple(chunk.x, chunk.y, chunk.z)

        if (chunks[triple] != chunk) {
            chunks.remove(triple)?.remove()
        }
        
        chunks[triple] = chunk
        set_chunk.invokeExact(worldPtr, chunk.x, chunk.y, chunk.z, chunk.blocks.address())
    }

    fun removeChunk(x: Int, y: Int, z: Int) {
        chunks.remove(Triple(x, y, z))?.remove()
        remove_chunk.invokeExact(worldPtr, x, y, z)
    }

    fun setChunkSpecial(
        chunkX: Int, chunkY: Int, chunkZ: Int,
        shapeId: Int, materialType: Int, instancesCount: Int, instancesPtr: Long
    ) {
        set_chunk_special.invokeExact(
            worldPtr, chunkX, chunkY, chunkZ, shapeId, materialType, instancesCount, instancesPtr
        )
    }

    fun checkShapes(shapeIds: IntArray): ByteArray {
        if (shapeIds.isEmpty()) return ByteArray(0)
        
        val byteCount = (shapeIds.size + 7) / 8
        val bitmaskArray = ByteArray(byteCount)
        
        arena {
            val idsSegment = allocate(shapeIds.size * 4L)
            for (i in shapeIds.indices) {
                idsSegment.setAtIndex(int, i.toLong(), shapeIds[i])
            }
            
            val outSegment = allocate(byteCount.toLong())
            
            check_shapes_batch.invokeExact(worldPtr, idsSegment.address(), shapeIds.size, outSegment.address())
            
            MemorySegment.copy(outSegment, byte, 0, bitmaskArray, 0, byteCount)
        }
        
        return bitmaskArray
    }

    fun addShape(shapeId: Int, boxes: FloatArray) {
        if (boxes.isEmpty() || boxes.size % 6 != 0) return
        val boxesCount = boxes.size / 6

        arena {
            val boxesSegment = allocate(boxes.size * 4L)
            for (i in boxes.indices) {
                boxesSegment.setAtIndex(float, i.toLong(), boxes[i])
            }
            
            add_shape.invokeExact(worldPtr, shapeId, boxesCount, boxesSegment.address())

            Unit
        }
    }
}