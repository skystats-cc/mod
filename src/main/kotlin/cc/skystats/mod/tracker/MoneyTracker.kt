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
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.scores.DisplaySlot
import net.minecraft.world.scores.PlayerTeam

/**
 * Sends your own coins to SkyStats every few minutes while you play SkyBlock.
 * This is the only source of money history: Hypixel's API policy forbids building it by polling.
 * Reads the sidebar purse line, the "Profile ID:" and rare-drop chat lines, and the SkyBlock item ids
 * and counts in your own inventory (the server keeps only their total value). Nothing else from chat
 * or the screen is read or sent. Sends nothing unless you have linked SkyStats and tracking is on.
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

    private val drops = java.util.concurrent.ConcurrentLinkedQueue<Map<String, Any>>()

    private fun onChat(message: Component) {
        if (trackingEnabled && ModConfig.token != null) {
            SkyBlockParse.rareDrop(message.string)?.let {
                if (drops.size < 100) drops.add(mapOf("type" to "rare_drop", "at" to System.currentTimeMillis(), "itemName" to it))
            }
        }
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
        val inventory = inventoryCounts(mc)
        val pendingDrops = generateSequence { drops.poll() }.toList()
        inFlight = true
        SkyStats.io.execute {
            try {
                Auth.withToken { SkyStatsApi.snapshot(it, profile, purse, inventory) }
                if (pendingDrops.isNotEmpty()) Auth.withToken { SkyStatsApi.events(it, profile, pendingDrops) }
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

    /** SkyBlock item ids and counts in your inventory (ids come from the item's custom data) */
    private fun inventoryCounts(mc: Minecraft): List<Map<String, Any>> {
        val inv = mc.player?.inventory ?: return emptyList()
        val counts = HashMap<String, Int>()
        for (slot in 0 until inv.containerSize) {
            val stack = inv.getItem(slot)
            if (stack.isEmpty) continue
            val id = stack.get(DataComponents.CUSTOM_DATA)?.copyTag()?.getStringOr("id", "") ?: ""
            if (id.isNotEmpty() && id.length <= 64) counts.merge(id, stack.count, Int::plus)
        }
        return counts.entries.take(300).map { mapOf("id" to it.key, "count" to it.value) }
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
