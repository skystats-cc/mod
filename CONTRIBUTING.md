# Contributing to the mod

## Setup
1. Install **JDK 25** and IntelliJ IDEA 2025.3+ with the **Stonecutter** plugin.
2. Open the folder; let Gradle sync (first sync downloads Minecraft for each version and takes a while).
3. Run `26.3 > runClient` (IntelliJ run config, or `./gradlew :26.3:runClient`).

## Branches
Same rules as the other repos: branch (`feat/...`, `fix/...`), PR, CI must be green. Commit with the **vcsVersion (26.3) active**.

## Multi-version code (Stonecutter)
Write code for the active version. When a Minecraft API differs between versions, use Stonecutter comments:

```kotlin
//? if >=26.3 {
someNewApi()
//?} else {
/*oldApi()
*///?}
```

Switch the active version with the Stonecutter IntelliJ plugin (or the `Set active project to …` Gradle tasks) to edit and test the other branch, then switch back to 26.3 before committing.

### Adding a Minecraft version
1. Add it to `versions(...)` in `settings.gradle.kts`.
2. Create `versions/<ver>/gradle.properties` with `minecraft_version`, `minecraft_dependency`, `fabric_api_version` (latest from https://fabricmc.net/develop).
3. `./gradlew buildAll` and fix anything that broke with `//? if` blocks.

## Testing linking in a dev client
Dev clients use an offline account, so Mojang will refuse `joinServer`. Use [DevAuth](https://github.com/DJtheRedstoner/DevAuth) to sign in with your real Microsoft account in dev:

```kotlin
// build.gradle.kts
repositories { maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1") }
dependencies { runtimeOnly("me.djtheredstoner:DevAuth-fabric:1.2.2") }
```
and add `-Ddevauth.enabled=true` to the run config. Point the mod at your local server with `-Dskystats.api=http://localhost:3000`.

## Rules
- **Never block the render thread.** Network and Mojang calls go through `SkyStats.io`; chat output through `Chat` (it hops back to the client thread).
- **Never send the Minecraft access token anywhere but Mojang** (`Auth.kt` is the only place it's touched).
- **No Hypixel API calls from the mod.** The server does those with its own key.
- New API calls: add them to `api-spec` first, then mirror the shape in `api/SkyStatsApi.kt`.
