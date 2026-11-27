package cc.skystats.mod

import com.google.gson.GsonBuilder
import net.fabricmc.loader.api.FabricLoader
import java.nio.file.Files
import java.nio.file.Path

/**
 * config/skystats.json      settings you can edit
 * config/skystats-auth.json your SkyStats token (don't share this file)
 *
 * Dev override: -Dskystats.api=http://localhost:3000
 */
object ModConfig {
    const val DEFAULT_API = "https://api.skystats.cc"

    private class Settings(var apiUrl: String = DEFAULT_API)
    private class AuthFile(var token: String? = null)

    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val dir: Path get() = FabricLoader.getInstance().configDir
    private val settingsPath: Path get() = dir.resolve("skystats.json")
    private val authPath: Path get() = dir.resolve("skystats-auth.json")

    private var settings = Settings()

    @Volatile
    var token: String? = null
        private set

    val apiUrl: String
        get() = (System.getProperty("skystats.api") ?: settings.apiUrl).trimEnd('/')

    fun load() {
        settings = read(settingsPath, Settings::class.java) ?: Settings().also { write(settingsPath, it) }
        token = read(authPath, AuthFile::class.java)?.token
    }

    fun saveToken(value: String?) {
        token = value
        write(authPath, AuthFile(value))
    }

    private fun <T> read(path: Path, type: Class<T>): T? = try {
        if (Files.exists(path)) Files.newBufferedReader(path).use { gson.fromJson(it, type) } else null
    } catch (e: Exception) {
        SkyStats.logger.warn("Couldn't read {}: {}", path.fileName, e.message)
        null
    }

    private fun write(path: Path, value: Any) {
        try {
            Files.createDirectories(path.parent)
            Files.newBufferedWriter(path).use { gson.toJson(value, it) }
        } catch (e: Exception) {
            SkyStats.logger.warn("Couldn't write {}: {}", path.fileName, e.message)
        }
    }
}
