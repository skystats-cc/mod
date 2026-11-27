pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/") { name = "Fabric" }
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie (Stonecutter)" }
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("net.fabricmc.fabric-loom") version providers.gradleProperty("loom_version").get()
        id("org.jetbrains.kotlin.jvm") version providers.gradleProperty("kotlin_version").get()
        id("dev.kikugie.stonecutter") version providers.gradleProperty("stonecutter_version").get()
    }
}

plugins {
    id("dev.kikugie.stonecutter")
}

// One source tree, one jar per Minecraft version. Add new versions here + a versions/<ver>/gradle.properties.
stonecutter {
    create(rootProject) {
        versions("26.2", "26.3")
        vcsVersion = "26.3"
    }
}

rootProject.name = "skystats-mod"
