package io.lionpa.physinautics.rust

import io.lionpa.kotlinffm.byte
import io.lionpa.kotlinffm.int
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment

class PhysicalChunk(
    val x: Int,
    val y: Int,
    val z: Int,
    val arena: Arena = Arena.ofConfined(),

    val blocks: MemorySegment = arena.allocate(byte, 4096)
) {
    fun remove() {
        arena.close()
    }
}