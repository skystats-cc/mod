package cc.skystats.mod.api

import cc.skystats.mod.ModConfig
import cc.skystats.mod.SkyStats
import com.google.gson.Gson
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/** Thrown for any failed API call. status 0 = couldn't reach SkyStats. */
class ApiException(val status: Int, val code: String, message: String) : RuntimeException(message)

// Response shapes (subset of the api-spec schemas). `var` + defaults so Gson can fill them.
class Account(
    var uuid: String = "",
    var username: String = "",
    var trackingEnabled: Boolean = true,
    var modLastSeenAt: String? = null,
    var modVersion: String? = null,
)
class Challenge(var serverId: String = "", var expiresAt: String = "")
class ModSession(var token: String = "", var expiresAt: String = "", var account: Account = Account())
class ClaimResult(var linked: Boolean = false, var account: Account = Account())
class HeartbeatResult(
    var ok: Boolean = false,
    var trackingEnabled: Boolean = true,
    var nextHeartbeatSeconds: Int = 300,
    var snapshotEverySeconds: Int = 300,
)
class SnapshotResult(var stored: Boolean = false, var reason: String? = null)
private class ErrorBody(var error: String? = null, var message: String? = null)

/**
 * Blocking HTTP client for the SkyStats API (see the api-spec repo for the contract).
 * Call only from SkyStats.io, never the render thread.
 */
object SkyStatsApi {
    private val gson = Gson()
    private val http: HttpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build()

    fun challenge(): Challenge = call("POST", "/v1/auth/mod/challenge", Challenge::class.java)

    fun verify(username: String, serverId: String): ModSession =
        call("POST", "/v1/auth/mod/verify", ModSession::class.java, body = mapOf("username" to username, "serverId" to serverId))

    fun claimPairing(token: String, code: String): ClaimResult =
        call("POST", "/v1/pairing/claim", ClaimResult::class.java, token, mapOf("code" to code))

    fun heartbeat(token: String): HeartbeatResult =
        call(
            "POST", "/v1/mod/heartbeat", HeartbeatResult::class.java, token,
            mapOf("modVersion" to SkyStats.version, "minecraftVersion" to SkyStats.minecraftVersion),
        )

    /** Your own coins, read from your sidebar. The only source of SkyStats money history. */
    fun snapshot(token: String, profileId: String, purse: Double): SnapshotResult =
        call("POST", "/v1/mod/snapshot", SnapshotResult::class.java, token, mapOf("profileId" to profileId, "purse" to purse))

    fun me(token: String): Account = call("GET", "/v1/me", Account::class.java, token)

    fun setTracking(token: String, enabled: Boolean): Account =
        call("PATCH", "/v1/me/settings", Account::class.java, token, mapOf("trackingEnabled" to enabled))

    fun logout(token: String) {
        call("POST", "/v1/auth/logout", Unit::class.java, token)
    }

    private fun <T> call(httpMethod: String, path: String, type: Class<T>, token: String? = null, body: Any? = null): T {
        val request = HttpRequest.newBuilder(URI.create(ModConfig.apiUrl + path))
            .timeout(Duration.ofSeconds(15))
            .header("User-Agent", "SkyStats-Mod/${SkyStats.version} (Minecraft ${SkyStats.minecraftVersion})")
            .header("Accept", "application/json")
            .apply { if (token != null) header("Authorization", "Bearer $token") }
            .apply {
                if (body == null) {
                    method(httpMethod, HttpRequest.BodyPublishers.noBody())
                } else {
                    header("Content-Type", "application/json")
                    method(httpMethod, HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                }
            }
            .build()

        val response = try {
            http.send(request, HttpResponse.BodyHandlers.ofString())
        } catch (e: Exception) {
            throw ApiException(0, "unreachable", "Couldn't reach SkyStats (${e.javaClass.simpleName}). Try again in a moment.")
        }

        if (response.statusCode() !in 200..299) {
            val err = runCatching { gson.fromJson(response.body(), ErrorBody::class.java) }.getOrNull()
            throw ApiException(
                response.statusCode(),
                err?.error ?: "http_${response.statusCode()}",
                err?.message ?: "SkyStats returned ${response.statusCode()}",
            )
        }

        @Suppress("UNCHECKED_CAST")
        if (type == Unit::class.java) return Unit as T
        return gson.fromJson(response.body(), type)
    }
}
