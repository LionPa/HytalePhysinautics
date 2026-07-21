package io.lionpa.physinautics.engine.java

import io.lionpa.physinautics.api.IPhysicsBody
import io.lionpa.physinautics.api.IPhysicsEngine
import io.lionpa.physinautics.api.IStaticCollider
import org.ode4j.math.DVector3
import org.ode4j.math.DQuaternion
import org.ode4j.ode.DBody
import org.ode4j.ode.DContact
import org.ode4j.ode.DContactBuffer
import org.ode4j.ode.DGeom
import org.ode4j.ode.DJointGroup
import org.ode4j.ode.DSpace
import org.ode4j.ode.DWorld
import org.ode4j.ode.OdeHelper

class Ode4jPhysicsEngine : IPhysicsEngine {
    private lateinit var world: DWorld
    private lateinit var space: DSpace
    private lateinit var contactGroup: DJointGroup
    
    private val nearCallback = DGeom.DNearCallback { data, o1, o2 ->
        val b1 = o1.body
        val b2 = o2.body
        if (b1 != null && b2 != null && OdeHelper.areConnected(b1, b2)) {
            return@DNearCallback
        }
        val contacts = DContactBuffer(16)
        val numContacts = OdeHelper.collide(o1, o2, 16, contacts.geomBuffer)
        for (i in 0 until numContacts) {
            val contact = contacts[i]
            contact.surface.mode = 0x004 // dContactBounce
            contact.surface.mu = 0.5
            contact.surface.bounce = 0.2
            contact.surface.bounce_vel = 0.1
            val c = OdeHelper.createContactJoint(world, contactGroup, contact)
            c.attach(b1, b2)
        }
    }

    override fun init() {
        OdeHelper.initODE()
        world = OdeHelper.createWorld()
        world.setGravity(0.0, -9.81, 0.0)
        space = OdeHelper.createHashSpace()
        contactGroup = OdeHelper.createJointGroup()
    }

    override fun step(dt: Float) {
        space.collide(null, nearCallback)
        world.quickStep(dt.toDouble())
        contactGroup.empty()
    }

    override fun createRigidBody(x: Double, y: Double, z: Double, sizeX: Double, sizeY: Double, sizeZ: Double, massAmount: Float, kinematic: Boolean): IPhysicsBody {
        val body = OdeHelper.createBody(world)
        body.position = DVector3(x, y, z)
        
        // Removed setAutoDisableFlag(true) to ensure bodies wake up when blocks are broken
        body.maxAngularSpeed = 5.0 // Limit angular speed to prevent crazy spinning
        
        val mass = OdeHelper.createMass()
        mass.setBox(1.0, sizeX, sizeY, sizeZ)
        mass.mass = massAmount.toDouble()
        body.mass = mass
        
        if (kinematic) {
            body.setKinematic()
        }
        
        val geom = OdeHelper.createBox(space, sizeX, sizeY, sizeZ)
        geom.body = body
        
        return Ode4jPhysicsBody(body, geom)
    }

    override fun removeRigidBody(body: IPhysicsBody) {
        if (body is Ode4jPhysicsBody) {
            body.geom.destroy()
            body.body.destroy()
        }
    }

    override fun createStaticBox(x: Double, y: Double, z: Double, sizeX: Double, sizeY: Double, sizeZ: Double): IStaticCollider {
        val geom = OdeHelper.createBox(space, sizeX, sizeY, sizeZ)
        geom.position = DVector3(x, y, z)
        return Ode4jStaticCollider(geom)
    }

    override fun removeStaticCollider(collider: IStaticCollider) {
        if (collider is Ode4jStaticCollider) {
            collider.geom.destroy()
        }
    }
}

class Ode4jStaticCollider(val geom: DGeom) : IStaticCollider

class Ode4jPhysicsBody(val body: DBody, val geom: DGeom) : IPhysicsBody {
    
    override fun getPositionX(): Double = body.position.get0()
    override fun getPositionY(): Double = body.position.get1()
    override fun getPositionZ(): Double = body.position.get2()

    override fun getRotationX(): Float = body.quaternion.get1().toFloat()
    override fun getRotationY(): Float = body.quaternion.get2().toFloat()
    override fun getRotationZ(): Float = body.quaternion.get3().toFloat()
    override fun getRotationW(): Float = body.quaternion.get0().toFloat()

    override fun getRotationEulerX(): Float {
        val q0 = body.quaternion.get0()
        val q1 = body.quaternion.get1()
        val q2 = body.quaternion.get2()
        val q3 = body.quaternion.get3()
        val sinr_cosp = 2.0 * (q0 * q1 + q2 * q3)
        val cosr_cosp = 1.0 - 2.0 * (q1 * q1 + q2 * q2)
        return kotlin.math.atan2(sinr_cosp, cosr_cosp).toFloat()
    }

    override fun getRotationEulerY(): Float {
        val q0 = body.quaternion.get0()
        val q1 = body.quaternion.get1()
        val q2 = body.quaternion.get2()
        val q3 = body.quaternion.get3()
        val sinp = 2.0 * (q0 * q2 - q3 * q1)
        return if (kotlin.math.abs(sinp) >= 1.0) {
            java.lang.Math.copySign(kotlin.math.PI / 2.0, sinp).toFloat()
        } else {
            kotlin.math.asin(sinp).toFloat()
        }
    }

    override fun getRotationEulerZ(): Float {
        val q0 = body.quaternion.get0()
        val q1 = body.quaternion.get1()
        val q2 = body.quaternion.get2()
        val q3 = body.quaternion.get3()
        val siny_cosp = 2.0 * (q0 * q3 + q1 * q2)
        val cosy_cosp = 1.0 - 2.0 * (q2 * q2 + q3 * q3)
        return kotlin.math.atan2(siny_cosp, cosy_cosp).toFloat()
    }

    override fun setPosition(x: Double, y: Double, z: Double) {
        body.position = DVector3(x, y, z)
    }

    override fun setRotation(x: Float, y: Float, z: Float, w: Float) {
        body.quaternion = DQuaternion(w.toDouble(), x.toDouble(), y.toDouble(), z.toDouble())
    }

    override fun setRotationEuler(x: Float, y: Float, z: Float) {
        val cr = kotlin.math.cos(x * 0.5)
        val sr = kotlin.math.sin(x * 0.5)
        val cp = kotlin.math.cos(y * 0.5)
        val sp = kotlin.math.sin(y * 0.5)
        val cy = kotlin.math.cos(z * 0.5)
        val sy = kotlin.math.sin(z * 0.5)

        val qw = cr * cp * cy + sr * sp * sy
        val qx = sr * cp * cy - cr * sp * sy
        val qy = cr * sp * cy + sr * cp * sy
        val qz = cr * cp * sy - sr * sp * cy

        body.quaternion = DQuaternion(qw, qx, qy, qz)
    }

    override fun setLinearVelocity(x: Double, y: Double, z: Double) {
        body.linearVel = DVector3(x, y, z)
    }

    override fun applyForce(x: Double, y: Double, z: Double) {
        body.addForce(x, y, z)
    }
}
