package cc.skystats.mod.command

import cc.skystats.mod.ModConfig
import cc.skystats.mod.SkyStats
import cc.skystats.mod.api.ApiException
import cc.skystats.mod.api.SkyStatsApi
import cc.skystats.mod.auth.Auth
import cc.skystats.mod.util.Chat
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import net.fabricmc.fabric.api.client.command.v2.ClientCommands
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft

/**
 * /skystats                      help
 * /skystats link <code>          asks you to confirm, then signs a browser in (code from skystats.cc/link)
 * /skystats status               what SkyStats knows about you
 * /skystats tracking <on|off>    record your money history or not
 * /skystats logout               forget the saved SkyStats token
 */
object SkyStatsCommand {
    private val CODE = Regex("^[A-Za-z0-9]{4}-?[A-Za-z0-9]{4}$")

    fun register(dispatcher: CommandDispatcher<FabricClientCommandSource>) {
        dispatcher.register(
            ClientCommands.literal("skystats")
                .executes { help(); 1 }
                .then(
                    ClientCommands.literal("link")
                        .executes { Chat.info("Get a code at skystats.cc/link, then run /skystats link <code>"); 1 }
                        .then(
                            ClientCommands.argument("code", StringArgumentType.word())
                                .executes { askToConfirm(StringArgumentType.getString(it, "code")); 1 }
                                .then(
                                    ClientCommands.literal("confirm")
                                        .executes { link(StringArgumentType.getString(it, "code")); 1 },
                                ),
                        ),
                )
                .then(ClientCommands.literal("status").executes { status(); 1 })
                .then(
                    ClientCommands.literal("tracking")
                        .then(ClientCommands.literal("on").executes { setTracking(true); 1 })
                        .then(ClientCommands.literal("off").executes { setTracking(false); 1 }),
                )
                .then(ClientCommands.literal("logout").executes { logout(); 1 }),
        )
    }

    private fun help() {
        Chat.show(Chat.line(Chat.text("SkyStats ${SkyStats.version}", ChatFormatting.WHITE)))
        Chat.info("/skystats link <code>  link skystats.cc (get a code at skystats.cc/link)")
        Chat.info("/skystats status  your SkyStats account")
        Chat.info("/skystats tracking on|off  record your money history")
        Chat.info("/skystats logout  forget the saved token")
    }

    /** Step 1: never link on paste alone. Someone could trick you into running *their* code. */
    private fun askToConfirm(rawCode: String) {
        if (!CODE.matches(rawCode)) {
            Chat.error("That doesn't look like a SkyStats code (e.g. ABCD-EFGH). Get one at skystats.cc/link")
            return
        }
        val code = rawCode.uppercase()
        val name = Minecraft.getInstance().user.name
        Chat.show(
            Chat.line(
                Chat.text("Sign a browser in to SkyStats as ", ChatFormatting.GRAY),
                Chat.text(name, ChatFormatting.WHITE),
                Chat.text("? Only confirm if you opened skystats.cc/link yourself just now. ", ChatFormatting.GRAY),
                Chat.button("Confirm", "/skystats link $code confirm", "Link code $code to $name"),
            ),
        )
    }

    /** Step 2: prove who we are via Mojang, then claim the code */
    private fun link(rawCode: String) {
        Chat.info("Linking…")
        SkyStats.io.execute {
            try {
                val result = Auth.withToken { SkyStatsApi.claimPairing(it, rawCode) }
                Chat.success("Linked! Your browser is now signed in as ${result.account.username}.")
            } catch (e: ApiException) {
                Chat.error(e.message ?: "Linking failed")
            } catch (e: Exception) {
                SkyStats.logger.warn("Linking failed", e)
                Chat.error("Linking failed: ${e.message}")
            }
        }
    }

    private fun status() {
        val token = ModConfig.token
        if (token == null) {
            Chat.info("Not linked yet. Get a code at skystats.cc/link and run /skystats link <code>")
            return
        }
        SkyStats.io.execute {
            try {
                val me = SkyStatsApi.me(token)
                Chat.info("Signed in as ${me.username}. Tracking ${if (me.trackingEnabled) "on" else "off"}. API ${ModConfig.apiUrl}")
            } catch (e: ApiException) {
                if (e.status == 401) {
                    Chat.info("Your SkyStats session expired. Run /skystats link <code> again.")
                } else {
                    Chat.error(e.message ?: "Couldn't reach SkyStats")
                }
            }
        }
    }

    private fun setTracking(enabled: Boolean) {
        SkyStats.io.execute {
            try {
                val me = Auth.withToken { SkyStatsApi.setTracking(it, enabled) }
                if (me.trackingEnabled) {
                    Chat.success("Tracking on: SkyStats records your money history.")
                } else {
                    Chat.success("Tracking off: SkyStats stops recording your money history. Your co-op sees \"Tracking off\".")
                }
            } catch (e: ApiException) {
                Chat.error(e.message ?: "Couldn't change tracking")
            }
        }
    }

    private fun logout() {
        val token = ModConfig.token ?: return Chat.info("You're not signed in.")
        SkyStats.io.execute {
            runCatching { SkyStatsApi.logout(token) }
            ModConfig.saveToken(null)
            Chat.success("Signed out of SkyStats on this game. Browsers you linked stay signed in until you sign out there.")
        }
    }
}
