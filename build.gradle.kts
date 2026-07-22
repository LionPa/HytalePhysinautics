plugins {
    idea
    kotlin("jvm") version "2.3.0"

    id("com.azuredoom.hytale-tools") version "1.+"
}


tasks.withType<Javadoc>().configureEach {
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:-missing", "-quiet")
}

group = project.property("group").toString()

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(property("java_version").toString().toInt()))
}

hytaleTools {
    javaVersion = property("java_version").toString().toInt()
    hytaleVersion = property("hytale_version").toString()
    manifestServerVersion = property("manifestServerVersion").toString()
    manifestGroup = property("manifest_group").toString()
    modId = property("mod_id").toString()
    modDescription = property("mod_description").toString()
    modUrl = property("mod_url").toString()
    mainClass = property("main_class").toString()
    modCredits = property("mod_author").toString()
    manifestDependencies = property("manifest_dependencies").toString()
    manifestOptionalDependencies = property("manifest_opt_dependencies").toString()
    curseforgeId = property("curseforgeID").toString()
    disabledByDefault = property("disabled_by_default").toString().toBoolean()
    includesPack = property("includes_pack").toString().toBoolean()
    patchline = property("patchline").toString()
    injectServerJavadocsIntoSources = property("injectServerJavadocsIntoSources").toString().toBoolean()
    generateAssetsBinary = property("generateAssetsBinary").toString().toBoolean()
    // hytaleHomeOverride = property("hytaleHomeOverride").toString()
}

repositories {
    mavenCentral()
    mavenLocal()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("fun.hygames.kotlinutils:HytaleKotlinUtils:dev")
    implementation("com.github.LionPa:kotlin-ffm:main-SNAPSHOT")
}

tasks.named<Jar>("jar") {
    archiveBaseName.set(project.property("mod_name").toString())
    archiveVersion.set(project.property("version").toString())
    // Включаем kotlin-ffm прямо в итоговый jar. 
    // Завернуто в provider, чтобы не резолвить зависимости на этапе конфигурации Gradle (ускоряет билд).
    from(provider {
        configurations.runtimeClasspath.get().filter { it.name.contains("kotlin-ffm") }.map { zipTree(it) }
    })
}

 idea {
     module {
         isDownloadSources = true
         isDownloadJavadoc = true
     }
 }

val rustRootDir = file("rust")
val nativesDir = file("src/main/resources/natives")
val pinnedRustVersion = "stable"
val zigbuildImage = "cargo-zigbuild-mac"
val xWinImage = "cargo-xwin-win"

data class Target(val arch: String, val os: String, val triple: String, val ext: String)

val supportedTargets = listOf(
    Target("x86_64", "macos", "x86_64-apple-darwin", "dylib"),
    Target("aarch64", "macos", "aarch64-apple-darwin", "dylib"),
    Target("x86_64", "linux", "x86_64-unknown-linux-gnu", "so"),
    Target("aarch64", "linux", "aarch64-unknown-linux-gnu", "so"),
    Target("x86_64", "windows", "x86_64-pc-windows-msvc", "dll"),
    Target("aarch64", "windows", "aarch64-pc-windows-msvc", "dll")
)

// 100% надежная проверка графа задач (вынесена наверх, до использования)
val isDev = provider {
    gradle.taskGraph.hasTask(":buildDev") || gradle.taskGraph.hasTask(":runServerDev")
}

tasks.register<Exec>("buildZigbuildImage") {
    group = "rust"
    workingDir = rustRootDir
    commandLine("docker", "build", "-t", zigbuildImage, "container/zigbuild", "--build-arg", "RUST_VERSION=${pinnedRustVersion}")
}

tasks.register<Exec>("buildXWinImage") {
    group = "rust"
    workingDir = rustRootDir
    commandLine("docker", "build", "-t", xWinImage, "container/xwin", "--build-arg", "RUST_VERSION=${pinnedRustVersion}")
}

tasks.register("buildImages") {
    group = "rust"
    dependsOn("buildZigbuildImage", "buildXWinImage")
}

val compileTasks = supportedTargets.map { target ->
    val taskName = "compileRust-${target.os}-${target.arch}"
    
    tasks.register<Exec>(taskName) {
        group = "rust"
        description = "Cross-compiles natives for the ${target.triple} target"
        
        val isWindows = target.triple.contains("msvc")
        val image = if (isWindows) xWinImage else zigbuildImage
        val cargoCommand = if (isWindows) "xwin" else "zigbuild"
        
        // Ensure path uses forward slashes for Docker on Windows
        val dockerVol = rustRootDir.absolutePath.replace("\\", "/")
        
        val args = mutableListOf("docker", "run", "--rm", "-v", "$dockerVol:/app", "-w", "/app", image, "cargo", cargoCommand)
        if (isWindows) args.add("build")
        args.addAll(listOf("--target", target.triple, "--release"))
        
        commandLine(args)
        
        // Кэширование Gradle: если исходники раста не менялись, докер даже не будет запускаться
        inputs.dir(rustRootDir.resolve("src"))
        inputs.file(rustRootDir.resolve("Cargo.toml"))
        outputs.dir(rustRootDir.resolve("target/${target.triple}/release"))
    }
}

tasks.register("buildRustNatives") {
    group = "build"
    description = "Compiles all Rust natives and moves them to resources."
    onlyIf { !isDev.get() }
    dependsOn(compileTasks)
}

tasks.register<Copy>("copyRustNatives") {
    group = "rust"
    into(nativesDir)
    onlyIf { !isDev.get() }
    mustRunAfter("buildRustNatives")

    supportedTargets.forEach { target ->
        from(file("$rustRootDir/target/${target.triple}/release/libphysinautics_lib.${target.ext}")) {
            into("${target.os}/${target.arch}")
        }

        if (target.os == "windows") {
            from(file("$rustRootDir/target/${target.triple}/release/physinautics_lib.${target.ext}")) {
                into("${target.os}/${target.arch}")
                rename { "libphysinautics_lib.${target.ext}" } // Standardize naming just in case
            }
        }
    }
}

tasks.named("processResources") {
    dependsOn("buildRustDev", "copyRustNatives")
}

tasks.named("prepareRunServer") {
    dependsOn("buildRustDev")
}

tasks.register("buildDev") {
    group = "build"
    description = "Fast development build (compiles local native only)"
    dependsOn("build")
}

tasks.register("runServerDev") {
    group = "hytale"
    description = "Fast development server run (compiles local native only)"
    dependsOn("runServer")
}

// ==========================================
// FAST LOCAL DEV BUILD (NO DOCKER)
// ==========================================

tasks.register<Exec>("compileRustDev") {
    group = "rust"
    description = "Compiles release native for the local host platform natively"
    onlyIf { isDev.get() }
    workingDir = rustRootDir
    commandLine("cargo", "build", "--release")
    
    // Кэширование Gradle
    inputs.dir(rustRootDir.resolve("src"))
    inputs.file(rustRootDir.resolve("Cargo.toml"))
    outputs.dir(rustRootDir.resolve("target/release"))
}

tasks.register<Copy>("buildRustDev") {
    group = "build"
    description = "Compiles and copies the local release native to resources"
    onlyIf { isDev.get() }
    dependsOn("compileRustDev")
    into(nativesDir)

    val osName = System.getProperty("os.name").lowercase()
    val isWin = osName.contains("win")
    val isMac = osName.contains("mac")

    val arch = System.getProperty("os.arch").lowercase()
    val isArm = arch.contains("aarch64") || arch.contains("arm")

    val targetOs = if (isWin) "windows" else if (isMac) "macos" else "linux"
    val targetArch = if (isArm) "aarch64" else "x86_64"
    val ext = if (isWin) "dll" else if (isMac) "dylib" else "so"
    val prefix = if (isWin) "" else "lib"

    from(file("$rustRootDir/target/release/${prefix}physinautics_lib.$ext")) {
        into("$targetOs/$targetArch")
        rename { "libphysinautics_lib.$ext" }
    }
}