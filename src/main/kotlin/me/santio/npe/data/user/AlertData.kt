package me.santio.npe.data.user

import com.github.retrooper.packetevents.PacketEvents
import me.santio.npe.NPE
import me.santio.npe.base.FlagData
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer
import org.bukkit.entity.Player

data class AlertData(
    val player: Player,
    val id: String,
    val disconnect: Boolean = true,
    val clickEvent: ClickEvent? = null,
    val extra: Component? = null,
    var count: Int = 1,
    var data: FlagData.() -> Unit = {}
) {

    fun hover(): Component {
        var hover = Component.newline()
        val flagData = FlagData().apply(data)

        for (data in flagData.map()) {
            hover = hover.append(
                Component.text(data.key, NPE.primaryColor)
                    .append(Component.text(": ", NamedTextColor.GRAY))
                    .append(Component.text(data.value.toString(), NamedTextColor.WHITE))
                    .appendNewline()
            )
        }

        extra?.takeIf {
            jsonSerializer.serialize(it).length <= 2048
        }?.let { hover = hover.append(Component.newline().append(it)) }

        return hover
    }

    fun component(): Component? {
        val user = PacketEvents.getAPI().playerManager.getUser(player)
        if (user == null) return null

        // Permission check
        try {
            if (!player.npe.bypassing() && disconnect) {
                user.closeConnection()
            }
        } catch (e: Exception) {
            NPE.logger.error("Failed to perform permission check on '{}'", player.name, e)
        }

        // Send the alert to administrators
        val hover = this.hover()
        var alert = Component.empty()
            .append(Component.text(player.name, NamedTextColor.RED))
            .append(Component.text(" was detected for ", NamedTextColor.GRAY))
            .append(Component.text(id, NamedTextColor.YELLOW))
            .hoverEvent(hover)

        if (count > 1) {
            alert = alert.append(Component.text(" (x$count)", NamedTextColor.GRAY).decorate(TextDecoration.ITALIC))
        }

        // Trim the click event if it's too long
        if (clickEvent != null && clickEvent.action() == ClickEvent.Action.COPY_TO_CLIPBOARD) {
            alert = if (clickEvent.value().length <= 10_000) {
                alert.clickEvent(clickEvent)
            } else {
                alert.clickEvent(ClickEvent.copyToClipboard(clickEvent.value().take(10_000)))
            }
        }

        return alert
    }

    private companion object {
        val jsonSerializer = JSONComponentSerializer.json()
    }

}
