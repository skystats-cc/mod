package cc.skystats.mod.util

import net.minecraft.client.Minecraft

object Hypixel {
    /** On Hypixel, judged by the address you joined (mc.hypixel.net, alpha.hypixel.net, ...) */
    fun isConnected(mc: Minecraft = Minecraft.getInstance()): Boolean =
        mc.currentServer?.ip?.lowercase()?.contains("hypixel.net") == true
}
