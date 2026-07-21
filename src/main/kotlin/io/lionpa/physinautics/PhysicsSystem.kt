package io.lionpa.physinautics

import com.hypixel.hytale.component.ArchetypeChunk
import com.hypixel.hytale.component.CommandBuffer
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.component.query.Query
import com.hypixel.hytale.component.system.tick.EntityTickingSystem
import com.hypixel.hytale.server.core.modules.entity.component.BoundingBox
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import `fun`.hygames.kotlinutils.BOUNDING_BOX
import `fun`.hygames.kotlinutils.TRANSFORM
import `fun`.hygames.kotlinutils.codeInitialization.Register
import `fun`.hygames.kotlinutils.get
import io.lionpa.physinautics.Physinautics.Companion.PHYSICAL_OBJECT

object PhysicsSystem: EntityTickingSystem<EntityStore>() {
    override fun tick(
        dt: Float,
        id: Int,
        chunk: ArchetypeChunk<EntityStore?>,
        store: Store<EntityStore?>,
        buffer: CommandBuffer<EntityStore?>
    ) {

        @Suppress("UNCHECKED_CAST")
        val storeChunk = chunk as ArchetypeChunk<EntityStore>
        val transform = storeChunk[id, TRANSFORM]!!
        val physObj = storeChunk[id, PHYSICAL_OBJECT]!!

        val boundingBox = storeChunk[id, BOUNDING_BOX]
        val sizeX = boundingBox?.boundingBox?.width() ?: 1.0
        val sizeY = boundingBox?.boundingBox?.height() ?: 1.0
        val sizeZ = boundingBox?.boundingBox?.depth() ?: 1.0

        if (physObj.physicsBody == null) {
            physObj.physicsBody = PhysicsWorld.engine.createRigidBody(
                transform.position.x,
                transform.position.y,
                transform.position.z,
                sizeX, sizeY, sizeZ,
                physObj.mass,
                physObj.kinematic
            )
        }

        val body = physObj.physicsBody!!

        if (physObj.kinematic) {
            // If kinematic, Hytale controls the position, update the physics engine
            body.setPosition(transform.position.x, transform.position.y, transform.position.z)
            body.setRotationEuler(transform.rotation.x, transform.rotation.y, transform.rotation.z)
        } else {
            // If dynamic, Physics engine controls the position, update Hytale
            transform.position.set(body.getPositionX(), body.getPositionY(), body.getPositionZ())
            transform.rotation.set(body.getRotationEulerX(), body.getRotationEulerY(), body.getRotationEulerZ())
        }
    }

    override fun getQuery(): Query<EntityStore> {
        return Query.and(PHYSICAL_OBJECT, TRANSFORM)
    }
}