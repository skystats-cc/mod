import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Applied once per Minecraft version (versions/<ver>/). Version-specific values live in
// versions/<ver>/gradle.properties; shared ones in the root gradle.properties.
plugins {
    id("net.fabricmc.fabric-loom")
    id("org.jetbrains.kotlin.jvm")
}

val minecraftVersion = stonecutter.current.version
version = "${property("mod_version")}+$minecraftVersion"
group = property("maven_group") as String
base { archivesName.set(property("archives_base_name") as String) }

dependencies {
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    implementation("net.fabricmc:fabric-loader:${property("loader_version")}")
    implementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_api_version")}")
    implementation("net.fabricmc:fabric-language-kotlin:${property("fabric_kotlin_version")}")
}

loom {
    runConfigs.all {
        ideConfigGenerated(true)
        runDir = "../../run" // one shared run folder for every version
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_25) }
}

tasks.withType<JavaCompile>().configureEach { options.release.set(25) }

tasks.processResources {
    val props = mapOf(
        "version" to project.version.toString(),
        "minecraft_dependency" to property("minecraft_dependency").toString(),
        "loader_version" to property("loader_version").toString(),
    )
    inputs.properties(props)
    filesMatching("fabric.mod.json") { expand(props) }
}

// Collect every version's jar in the root build/libs
tasks.named<Jar>("jar") {
    destinationDirectory.set(rootProject.layout.buildDirectory.dir("libs"))
}
