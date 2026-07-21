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
    }

    val arena = Arena.ofConfined()
    var worldPtr: Long = 0
    var syncBuffer: MemorySegment? = null
    val maxObjects = 10000

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
}