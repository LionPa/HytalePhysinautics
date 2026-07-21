package io.lionpa.physinautics.api

interface IPhysicsEngine {
    fun init()
    fun step(dt: Float)
    fun createRigidBody(x: Double, y: Double, z: Double, sizeX: Double, sizeY: Double, sizeZ: Double, mass: Float, kinematic: Boolean): IPhysicsBody
    fun removeRigidBody(body: IPhysicsBody)

    fun createStaticBox(x: Double, y: Double, z: Double, sizeX: Double, sizeY: Double, sizeZ: Double): IStaticCollider
    fun removeStaticCollider(collider: IStaticCollider)
}

interface IStaticCollider {
}

interface IPhysicsBody {
    fun getPositionX(): Double
    fun getPositionY(): Double
    fun getPositionZ(): Double

    fun getRotationX(): Float
    fun getRotationY(): Float
    fun getRotationZ(): Float
    fun getRotationW(): Float

    fun getRotationEulerX(): Float
    fun getRotationEulerY(): Float
    fun getRotationEulerZ(): Float

    fun setPosition(x: Double, y: Double, z: Double)
    fun setRotation(x: Float, y: Float, z: Float, w: Float)
    fun setRotationEuler(x: Float, y: Float, z: Float)
    
    fun setLinearVelocity(x: Double, y: Double, z: Double)
    fun applyForce(x: Double, y: Double, z: Double)
}
