package io.lionpa.physinautics.rust

import io.lionpa.kotlinffm.float
import java.lang.foreign.Arena

class RigidBody(val world: PhysicsWorld, val id: Int) {

    fun applyForce(x: Double, y: Double, z: Double) {
        world.applyImpulse(id, x.toFloat(), y.toFloat(), z.toFloat())
    }

    fun remove() {
        world.removeBody(id)
    }

    private fun getFloat(offset: Long): Float {
        return world.syncBuffer?.get(float, id * 7L * 4L + offset) ?: 0f
    }

    fun getPositionX(): Double = getFloat(0).toDouble()
    fun getPositionY(): Double = getFloat(4).toDouble()
    fun getPositionZ(): Double = getFloat(8).toDouble()

    fun getRotationX(): Float = getFloat(12)
    fun getRotationY(): Float = getFloat(16)
    fun getRotationZ(): Float = getFloat(20)
    fun getRotationW(): Float = getFloat(24)

    fun getRotationEulerX(): Float {
        val q0 = getRotationW().toDouble()
        val q1 = getRotationX().toDouble()
        val q2 = getRotationY().toDouble()
        val q3 = getRotationZ().toDouble()
        val sinr_cosp = 2.0 * (q0 * q1 + q2 * q3)
        val cosr_cosp = 1.0 - 2.0 * (q1 * q1 + q2 * q2)
        return kotlin.math.atan2(sinr_cosp, cosr_cosp).toFloat()
    }

    fun getRotationEulerY(): Float {
        val q0 = getRotationW().toDouble()
        val q1 = getRotationX().toDouble()
        val q2 = getRotationY().toDouble()
        val q3 = getRotationZ().toDouble()
        val sinp = 2.0 * (q0 * q2 - q3 * q1)
        return if (kotlin.math.abs(sinp) >= 1.0) {
            java.lang.Math.copySign(kotlin.math.PI / 2.0, sinp).toFloat()
        } else {
            kotlin.math.asin(sinp).toFloat()
        }
    }

    fun getRotationEulerZ(): Float {
        val q0 = getRotationW().toDouble()
        val q1 = getRotationX().toDouble()
        val q2 = getRotationY().toDouble()
        val q3 = getRotationZ().toDouble()
        val siny_cosp = 2.0 * (q0 * q3 + q1 * q2)
        val cosy_cosp = 1.0 - 2.0 * (q2 * q2 + q3 * q3)
        return kotlin.math.atan2(siny_cosp, cosy_cosp).toFloat()
    }
}