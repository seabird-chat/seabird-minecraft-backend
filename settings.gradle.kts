pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/") { name = "FabricMC" }
        maven("https://maven.neoforged.net/releases/") { name = "NeoForged" }
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
    }
}

plugins {
    // Stonecutter
    id("dev.kikugie.stonecutter") version "0.7.11"

    // Mod loaders
    id("fabric-loom") version "1.13-SNAPSHOT" apply false
    id("net.neoforged.moddev") version "2.0.119" apply false

    // Additional plugins
    id("com.google.protobuf") version "0.9.5" apply false
    id("com.gradleup.shadow") version "9.2.2" apply false
}

stonecutter {
    create(rootProject) {
        mapBuilds { branch, data ->
            val loader = data.project.substringAfterLast('-')
            "build.$loader.gradle.kts"
        }

        vers("1.21.1-fabric", "1.21.1")
        vers("1.21.1-neoforge", "1.21.1")

        vcsVersion = "1.21.1-neoforge"
    }
}

rootProject.name = "Seabird Minecraft Backend"
