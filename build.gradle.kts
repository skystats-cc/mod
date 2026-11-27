import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Applied once per Minecraft version (versions/<ver>/). Version-specific values live in
// versions/<ver>/gradle.properties; shared ones in the root gradle.properties.
plugins {
    id("net.fabricmc.fabric-loom")
    id("org.jetbrains.kotlin.jvm")
}

// Read every Gradle property here, at project level. Inside a task block (e.g. processResources)
// a bare property("x") looks on the task instead of the project and fails.
fun prop(name: String): String = project.property(name).toString()

val minecraftVersion = stonecutter.current.version
val minecraftDependency = prop("minecraft_dependency")
val loaderVersion = prop("loader_version")

version = "${prop("mod_version")}+$minecraftVersion"
group = prop("maven_group")
base { archivesName.set(prop("archives_base_name")) }

dependencies {
    minecraft("com.mojang:minecraft:${prop("minecraft_version")}")
    implementation("net.fabricmc:fabric-loader:$loaderVersion")
    implementation("net.fabricmc.fabric-api:fabric-api:${prop("fabric_api_version")}")
    implementation("net.fabricmc:fabric-language-kotlin:${prop("fabric_kotlin_version")}")
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
        "minecraft_dependency" to minecraftDependency,
        "loader_version" to loaderVersion,
    )
    inputs.properties(props)
    filesMatching("fabric.mod.json") { expand(props) }
}

// Collect every version's jar in the root build/libs
tasks.named<Jar>("jar") {
    destinationDirectory.set(rootProject.layout.buildDirectory.dir("libs"))
}
