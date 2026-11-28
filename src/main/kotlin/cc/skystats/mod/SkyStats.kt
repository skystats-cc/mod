package cc.skystats.mod

import cc.skystats.mod.command.SkyStatsCommand
import cc.skystats.mod.heartbeat.Heartbeat
import cc.skystats.mod.tracker.MoneyTracker
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.loader.api.FabricLoader
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

object SkyStats : ClientModInitializer {
    const val MOD_ID = "skystats"

    val logger: Logger = LoggerFactory.getLogger("SkyStats")

    val version: String by lazy { modVersion(MOD_ID) ?: "dev" }
    val minecraftVersion: String by lazy { modVersion("minecraft") ?: "unknown" }

    /** Every network and Mojang call runs here, never on the render thread. */
    val io: ExecutorService = Executors.newSingleThreadExecutor { r ->
        Thread(r, "SkyStats-IO").apply { isDaemon = true }
    }

    override fun onInitializeClient() {
        ModConfig.load()
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ -> SkyStatsCommand.register(dispatcher) }
        Heartbeat.register()
        MoneyTracker.register()
        logger.info("SkyStats {} loaded (API {})", version, ModConfig.apiUrl)
    }

    private fun modVersion(id: String): String? =
        FabricLoader.getInstance().getModContainer(id).map { it.metadata.version.friendlyString }.orElse(null)
}
