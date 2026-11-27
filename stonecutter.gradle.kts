plugins {
    id("dev.kikugie.stonecutter")
}

// The version your IDE and `//?` comments currently target. Switch with the Stonecutter IntelliJ plugin
// or the "Set active project to ..." Gradle tasks. Commit with the vcsVersion active.
stonecutter active "26.3"

tasks.register("buildAll") {
    group = "build"
    description = "Builds the mod for every Minecraft version (jars end up in build/libs)."
    dependsOn(stonecutter.tasks.named("build"))
}
