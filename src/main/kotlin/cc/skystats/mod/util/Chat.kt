package cc.skystats.mod.util

import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.MutableComponent

/** Chat output. Safe to call from any thread (hops to the client thread). */
object Chat {
    private fun prefix(): MutableComponent = Component.literal("[SkyStats] ").withStyle(ChatFormatting.GOLD)

    fun line(vararg parts: Component): Component {
        val out = prefix()
        parts.forEach { out.append(it) }
        return out
    }

    fun text(s: String, color: ChatFormatting = ChatFormatting.GRAY): MutableComponent =
        Component.literal(s).withStyle(color)

    /** [label] that runs a (client) command when clicked */
    fun button(label: String, command: String, hover: String, color: ChatFormatting = ChatFormatting.GREEN): MutableComponent =
        Component.literal("[$label]").withStyle { style ->
            style.withColor(color)
                .withBold(true)
                .withClickEvent(ClickEvent.RunCommand(command))
                .withHoverEvent(HoverEvent.ShowText(Component.literal(hover)))
        }

    fun show(message: Component) {
        val mc = Minecraft.getInstance()
        mc.execute { mc.gui.chat.addMessage(message) }
    }

    fun info(s: String) = show(line(text(s)))
    fun success(s: String) = show(line(text(s, ChatFormatting.GREEN)))
    fun error(s: String) = show(line(text(s, ChatFormatting.RED)))
}
