package cc.skystats.mod.auth

import cc.skystats.mod.ModConfig
import cc.skystats.mod.api.ApiException
import cc.skystats.mod.api.SkyStatsApi
import net.minecraft.client.Minecraft

/**
 * Proves to SkyStats who you are, the same way joining an online-mode server does:
 * 1. SkyStats gives us a one-time serverId
 * 2. we tell Mojang "this account is joining serverId" (your access token goes to Mojang only)
 * 3. SkyStats asks Mojang "did <username> join serverId?" and, if yes, gives us a SkyStats token
 *
 * Blocking: call from SkyStats.io.
 */
object Auth {
    fun token(forceNew: Boolean = false): String {
        if (!forceNew) ModConfig.token?.let { return it }

        val mc = Minecraft.getInstance()
        val user = mc.user
        val challenge = SkyStatsApi.challenge()
        try {
            mc.services().sessionService().joinServer(user.profileId, user.accessToken, challenge.serverId)
        } catch (e: Exception) {
            throw ApiException(
                401, "mojang",
                "Mojang couldn't verify your account (${e.message ?: e.javaClass.simpleName}). " +
                    "Make sure you're signed in with your Microsoft account, not offline.",
            )
        }

        val session = SkyStatsApi.verify(user.name, challenge.serverId)
        ModConfig.saveToken(session.token)
        return session.token
    }

    /** Runs an authenticated call, signing in again once if SkyStats rejected the saved token. */
    fun <T> withToken(call: (String) -> T): T {
        val saved = token()
        return try {
            call(saved)
        } catch (e: ApiException) {
            if (e.status != 401 || e.code == "mojang") throw e
            call(token(forceNew = true))
        }
    }
}
