package io.lionpa.physinautics

import io.lionpa.physinautics.api.IPhysicsEngine
import io.lionpa.physinautics.engine.java.Ode4jPhysicsEngine

object PhysicsWorld {
    val engine: IPhysicsEngine = Ode4jPhysicsEngine()

    fun init() {
        engine.init()
    }

    fun step(dt: Float) {
        engine.step(dt)
    }
}
