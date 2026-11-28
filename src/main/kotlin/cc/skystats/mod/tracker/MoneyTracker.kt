package cc.skystats.mod.tracker

import cc.skystats.mod.ModConfig
import cc.skystats.mod.SkyStats
import cc.skystats.mod.api.ApiException
import cc.skystats.mod.api.SkyStatsApi
import cc.skystats.mod.auth.Auth
import cc.skystats.mod.util.Hypixel
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.world.scores.DisplaySlot
import net.minecraft.world.scores.PlayerTeam

/**
 * Sends your own coins to SkyStats every few minutes while you play SkyBlock.
 * This is the only source of money history: Hypixel's API policy forbids building it by polling.
 * Reads only the sidebar purse line and the "Profile ID:" chat line. Nothing else from chat is read
 * or sent. Sends nothing unless you have linked SkyStats and tracking is on.
 */
object MoneyTracker {
    private const val TICKS_PER_SECOND = 20

    @Volatile var trackingEnabled = true
    @Volatile var intervalSeconds = 300

    private var profileId: String? = null
    private var ticks = 0

    @Volatile private var inFlight = false

    fun register() {
        ClientPlayConnectionEvents.JOIN.register { _, _, _ ->
            profileId = null
            ticks = 0
        }
        ClientReceiveMessageEvents.GAME.register { message, overlay -> if (!overlay) onChat(message) }
        ClientTickEvents.END_CLIENT_TICK.register { client -> tick(client) }
    }

    private fun onChat(message: Component) {
        SkyBlockParse.profileId(message.string)?.let {
            if (it != profileId) ticks = intervalSeconds * TICKS_PER_SECOND - 10 * TICKS_PER_SECOND // send soon after a switch
            profileId = it
        }
    }

    private fun tick(mc: Minecraft) {
        if (ModConfig.token == null || !trackingEnabled || !Hypixel.isConnected(mc)) return
        if (++ticks < intervalSeconds * TICKS_PER_SECOND || inFlight) return
        ticks = 0
        val profile = profileId ?: return
        val purse = SkyBlockParse.purse(sidebarLines(mc)) ?: return
        inFlight = true
        SkyStats.io.execute {
            try {
                Auth.withToken { SkyStatsApi.snapshot(it, profile, purse) }
            } catch (e: ApiException) {
                if (e.code == "tracking_disabled") trackingEnabled = false
                SkyStats.logger.debug("Snapshot not sent: {}", e.message)
            } catch (e: Exception) {
                SkyStats.logger.debug("Snapshot failed: {}", e.message)
            } finally {
                inFlight = false
            }
        }
    }

    /** Sidebar lines, top to bottom, as the player sees them */
    private fun sidebarLines(mc: Minecraft): List<String> {
        val scoreboard = mc.level?.scoreboard ?: return emptyList()
        val objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR) ?: return emptyList()
        return scoreboard.listPlayerScores(objective)
            .sortedByDescending { it.value() }
            .map { entry ->
                val team = scoreboard.getPlayersTeam(entry.owner())
                PlayerTeam.formatNameForTeam(team, Component.literal(entry.owner())).string
            }
    }
}
