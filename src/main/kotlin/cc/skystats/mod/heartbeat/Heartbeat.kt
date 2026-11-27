package cc.skystats.mod.heartbeat

import cc.skystats.mod.ModConfig
import cc.skystats.mod.SkyStats
import cc.skystats.mod.api.SkyStatsApi
import cc.skystats.mod.auth.Auth
import cc.skystats.mod.util.Hypixel
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.minecraft.client.Minecraft

/**
 * While you're on Hypixel (and have linked SkyStats), tell SkyStats every ~5 minutes that the mod is running.
 * SkyStats uses this to show "SkyStats mod" on your co-op and to keep your profiles fresh.
 * Players who never ran /skystats link send nothing.
 */
object Heartbeat {
    private const val TICKS_PER_SECOND = 20
    private var intervalTicks = 300 * TICKS_PER_SECOND
    private var ticks = 0

    @Volatile
    private var inFlight = false

    fun register() {
        // First beat ~15s after joining a server
        ClientPlayConnectionEvents.JOIN.register { _, _, _ -> ticks = intervalTicks - 15 * TICKS_PER_SECOND }
        ClientTickEvents.END_CLIENT_TICK.register { client -> tick(client) }
    }

    private fun tick(mc: Minecraft) {
        if (ModConfig.token == null || !Hypixel.isConnected(mc)) return
        if (++ticks < intervalTicks || inFlight) return
        ticks = 0
        inFlight = true
        SkyStats.io.execute {
            try {
                val result = Auth.withToken { SkyStatsApi.heartbeat(it) }
                intervalTicks = result.nextHeartbeatSeconds.coerceIn(60, 3600) * TICKS_PER_SECOND
            } catch (e: Exception) {
                SkyStats.logger.debug("Heartbeat failed: {}", e.message)
            } finally {
                inFlight = false
            }
        }
    }
}
