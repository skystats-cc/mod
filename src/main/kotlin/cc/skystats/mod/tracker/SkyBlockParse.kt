package cc.skystats.mod.tracker

/**
 * Pure text parsing for what Hypixel shows the player. No Minecraft classes, so it is easy to test.
 * We only read two things: the sidebar "Purse:" line and the "Profile ID:" chat line.
 */
object SkyBlockParse {
    // "Purse: 1,234,567" / "Piggy: 1,234.5 (+20)" after colour codes are stripped
    private val PURSE = Regex("""^(?:Purse|Piggy):\s*([\d,]+(?:\.\d+)?)""")
    // "Profile ID: 1b2c3d4e-...". Hypixel sends this when you join SkyBlock or switch profile.
    private val PROFILE_ID = Regex("""^Profile ID:\s*([0-9a-fA-F]{8}-?[0-9a-fA-F]{4}-?[0-9a-fA-F]{4}-?[0-9a-fA-F]{4}-?[0-9a-fA-F]{12})\s*$""")
    private val FORMATTING = Regex("§.")

    fun stripFormatting(s: String): String = FORMATTING.replace(s, "").replace(Regex("[^\\x20-\\x7E]"), "").trim()

    /** Purse from sidebar lines (top to bottom), or null if there is none (not in SkyBlock) */
    fun purse(lines: List<String>): Double? =
        lines.firstNotNullOfOrNull { PURSE.find(stripFormatting(it))?.groupValues?.get(1) }
            ?.replace(",", "")
            ?.toDoubleOrNull()
            ?.takeIf { it >= 0 && it < 1e13 }

    /** Profile id from one chat message, without dashes, or null */
    fun profileId(chat: String): String? =
        PROFILE_ID.find(stripFormatting(chat))?.groupValues?.get(1)?.replace("-", "")?.lowercase()
}
