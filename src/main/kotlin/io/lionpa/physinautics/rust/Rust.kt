package io.lionpa.physinautics.rust

import io.lionpa.kotlinffm.library.Arch
import io.lionpa.kotlinffm.library.Library
import io.lionpa.kotlinffm.library.OS
import io.lionpa.kotlinffm.library.PlatformLibraryFile
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.DigestInputStream
import java.security.MessageDigest

object Rust : Library(
    PlatformLibraryFile(OS.WINDOWS, Arch.AMD64) {
        getResourceAsFile("/natives/windows/x86_64/libphysinautics_lib.dll")
    },
    PlatformLibraryFile(OS.WINDOWS, Arch.ARM) {
        getResourceAsFile("/natives/windows/aarch64/libphysinautics_lib.dll")
    },
    PlatformLibraryFile(OS.LINUX, Arch.AMD64) {
        getResourceAsFile("/natives/linux/x86_64/libphysinautics_lib.so")
    },
    PlatformLibraryFile(OS.LINUX, Arch.ARM) {
        getResourceAsFile("/natives/linux/aarch64/libphysinautics_lib.so")
    },
    PlatformLibraryFile(OS.MACOS, Arch.AMD64) {
        getResourceAsFile("/natives/macos/x86_64/libphysinautics_lib.dylib")
    },
    PlatformLibraryFile(OS.MACOS, Arch.ARM) {
        getResourceAsFile("/natives/macos/aarch64/libphysinautics_lib.dylib")
    }
)

private fun getResourceAsFile(resourcePath: String): File {
    val inputStream = Rust::class.java.getResourceAsStream(resourcePath)
        ?: throw IllegalArgumentException("Resource not found: $resourcePath")

    val fileName = resourcePath.substringAfterLast("/")
    val nameWithoutExt = fileName.substringBeforeLast(".")
    val extension = fileName.substringAfterLast(".", "")

    val tempDir = System.getProperty("java.io.tmpdir")

    val tempFile = Files.createTempFile("physinautics_", ".$extension").toFile()
    val hash: String

    try {
        val digest = MessageDigest.getInstance("SHA-256")
        inputStream.use { input ->
            DigestInputStream(input, digest).use { dis ->
                Files.copy(dis, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        }
        
        hash = digest.digest().joinToString("") { "%02x".format(it) }.take(12)
    } catch (e: Exception) {
        tempFile.delete()
        throw e
    }

    val finalFile = File(tempDir, "${nameWithoutExt}_$hash.$extension")

    if (finalFile.exists() && finalFile.length() == tempFile.length()) {
        tempFile.delete()
        return finalFile
    }

    try {
        Files.move(tempFile.toPath(), finalFile.toPath(), StandardCopyOption.ATOMIC_MOVE)
    } catch (e: java.nio.file.FileAlreadyExistsException) {
        tempFile.delete()
    } catch (e: Exception) {
        Files.move(tempFile.toPath(), finalFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }

    finalFile.deleteOnExit()
    
    return finalFile
}