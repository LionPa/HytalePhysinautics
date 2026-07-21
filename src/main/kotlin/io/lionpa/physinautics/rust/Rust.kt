package io.lionpa.physinautics.rust

import io.lionpa.kotlinffm.library.Arch
import io.lionpa.kotlinffm.library.Library
import io.lionpa.kotlinffm.library.OS
import io.lionpa.kotlinffm.library.PlatformLibraryFile
import java.io.File

object Rust: Library(
    PlatformLibraryFile(OS.WINDOWS, Arch.AMD64) {
        File("F:/Kotlin/HytalePhysinautics/rust/target/x86_64-pc-windows-msvc/release/physinautics_lib.dll")
    }
)