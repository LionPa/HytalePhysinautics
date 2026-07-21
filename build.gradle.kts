import com.google.errorprone.annotations.Var

plugins {
    idea

    kotlin("jvm") version "2.3.0"
    id("dev.gobley.cargo") version "0.3.7"

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
    implementation("org.ode4j:core:0.5.4")
    implementation("com.github.LionPa:kotlin-ffm:main-SNAPSHOT")
}

cargo {
    packageDirectory = layout.projectDirectory.dir("rust")

    jvmVariant = gobley.gradle.Variant.Release
}

tasks.named<Jar>("jar") {
    archiveBaseName.set(project.property("mod_name").toString())
    archiveVersion.set(project.property("version").toString())
}

 idea {
     module {
         isDownloadSources = true
         isDownloadJavadoc = true
     }
 }

tasks.configureEach {
    if (name.contains("WindowsArm64")) {
        enabled = false
    }
}
