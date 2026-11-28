# SkyStats mod

Fabric client mod for [skystats.cc](https://skystats.cc), written in Kotlin. One source tree builds a jar for every supported Minecraft version with [Stonecutter](https://stonecutter.kikugie.dev/).

| Minecraft | Fabric API | Jar |
|---|---|---|
| 26.2.x | 0.161.0+26.2 | `skystats-<ver>+26.2.jar` |
| 26.3.x | 0.165.0+26.3 | `skystats-<ver>+26.3.jar` |

Requires Fabric Loader 0.19.5+, Fabric API, [Fabric Language Kotlin](https://modrinth.com/mod/fabric-language-kotlin) and Java 25.

## What it does

- **`/skystats link <code>`** links your browser to your Minecraft account. Get the code (with a copy button) at skystats.cc/link, paste the command, click **[Confirm]** in chat.
- **Heartbeat**: while you're on Hypixel, tells SkyStats every ~5 minutes that the mod is running, so your co-op sees "SkyStats mod". Only after you've linked.
- **Money tracking**: every ~5 minutes in SkyBlock, sends your purse (read from your sidebar) and your profile id (from Hypixel's "Profile ID:" chat line) to SkyStats. This is where your money history comes from, because Hypixel's API rules don't allow building it by polling their API. Nothing else from chat or the screen is read or sent. Only after you've linked, and never while `/skystats tracking off`.
- `/skystats status`, `/skystats tracking on|off`, `/skystats logout`.

### How sign-in works (your login never leaves Minecraft)
1. The mod asks SkyStats for a one-time `serverId`.
2. It tells **Mojang** "this account is joining `serverId`", the same call the game makes when you join any online server. Your access token goes to Mojang only.
3. SkyStats asks Mojang "did `<username>` join `serverId`?" and, if yes, issues a SkyStats token (saved in `config/skystats-auth.json`).

All Hypixel API calls happen on the SkyStats server with its own key. The mod never asks for or uses an API key.

## Building

```bash
./gradlew buildAll            # every version -> build/libs/
./gradlew :26.3:build         # one version
./gradlew :26.3:runClient     # dev client (run/ folder is shared between versions)
```

Gradle needs **Java 25** (`JAVA_HOME`, or IntelliJ: Settings → Build Tools → Gradle → Gradle JVM).

To use a local server: add `-Dskystats.api=http://localhost:3000` to the run config's VM options (or set `apiUrl` in `run/config/skystats.json`).

To test linking in a dev client you need a real Microsoft login (dev clients are offline by default). See CONTRIBUTING.md.

## Layout

```
settings.gradle.kts         Stonecutter versions list
stonecutter.gradle.kts      active version + buildAll task
build.gradle.kts            per-version build (Loom, Kotlin, Fabric API)
gradle.properties           shared versions (Loom, Kotlin, Loader, mod version)
versions/<mc>/gradle.properties   Minecraft + Fabric API version for that node
src/main/kotlin/cc/skystats/mod/
  SkyStats.kt               entrypoint, IO thread
  ModConfig.kt              config/skystats.json + config/skystats-auth.json
  api/SkyStatsApi.kt        HTTP client for the SkyStats API (contract: api-spec repo)
  auth/Auth.kt              Mojang joinServer → SkyStats token
  command/SkyStatsCommand.kt  /skystats ...
  heartbeat/Heartbeat.kt    5-minute heartbeat while on Hypixel
  tracker/MoneyTracker.kt   sends your purse every ~5 minutes (sidebar + profile id)
  tracker/SkyBlockParse.kt  pure parsing of the purse and profile id lines
  util/                     chat helpers, Hypixel detection
```

## Releases

Push a tag `v0.1.0` and CI builds every version and attaches the jars to a GitHub release.
