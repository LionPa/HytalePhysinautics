package io.lionpa.physinautics

import com.hypixel.hytale.component.ArchetypeChunk
import com.hypixel.hytale.component.CommandBuffer
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.component.query.Query
import com.hypixel.hytale.component.system.tick.EntityTickingSystem
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import `fun`.hygames.kotlinutils.BOUNDING_BOX
import `fun`.hygames.kotlinutils.TRANSFORM
import `fun`.hygames.kotlinutils.get
import io.lionpa.physinautics.Physinautics.Companion.PHYSICAL_OBJECT
import org.joml.Math
import org.joml.Quaternionf
import org.joml.Vector3f

object PhysicsSystem: EntityTickingSystem<EntityStore>() {
    override fun tick(
        dt: Float,
        id: Int,
        chunk: ArchetypeChunk<EntityStore>,
        store: Store<EntityStore?>,
        buffer: CommandBuffer<EntityStore?>
    ) {
        val transform = chunk[id, TRANSFORM]!!
        val physObj = chunk[id, PHYSICAL_OBJECT]!!

        val boundingBox = chunk[id, BOUNDING_BOX]
        val sizeX = boundingBox?.boundingBox?.width() ?: 1.0

        if (physObj.physicsBody == null) {
            physObj.physicsBody = Physinautics.getWorld(store.externalData.world).createRigidBody(
                transform.position.x,
                transform.position.y,
                transform.position.z,
                sizeX
            )
        }

        val body = physObj.physicsBody!!

        transform.position.set(body.getPositionX(), body.getPositionY(), body.getPositionZ())

        val x = body.getRotationX()
        val y = body.getRotationY()
        val z =body.getRotationZ()
        val w = body.getRotationW()

        val xRot = Math.safeAsin(-2.0f * (y * z - w * x))
        val yRot = Math.atan2(x * z + y * w, 0.5f - y * y - x * x)
        val zRot = Math.atan2(y * x + w * z, 0.5f - x * x - z * z)

        transform.rotation.set(xRot, yRot, zRot)
    }

    override fun getQuery(): Query<EntityStore> {
        return Query.and(PHYSICAL_OBJECT, TRANSFORM)
    }
}