package me.santio.npe.command

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.ProtocolPacketEvent
import com.github.retrooper.packetevents.wrapper.PacketWrapper
import com.google.auto.service.AutoService
import me.santio.npe.data.DataConverter
import me.santio.npe.data.user.NPEUser
import me.santio.npe.data.user.npe
import me.santio.npe.helper.not
import me.santio.npe.inspection.PacketInspection
import org.bukkit.entity.Player
import org.incendo.cloud.annotation.specifier.Greedy
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.CommandDescription
import org.incendo.cloud.annotations.Permission
import org.incendo.cloud.annotations.suggestion.Suggestions
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.paper.util.sender.PlayerSource
import java.lang.reflect.Modifier

@Suppress("unused")
@AutoService(BaseCommand::class)
class DebugCommand: BaseCommand {

    @Command("npe debug listen [action]")
    @Permission("npe.debug")
    @CommandDescription("Developer command for viewing internal information")
    fun debugListen(
        sender: PlayerSource,
        @Argument("action", suggestions = "debug-action") action: String?
    ) {
        val action = action?.takeIf {
            it.isNotBlank() && it != "stop"
        }

        sender.source().npe.debug(action)
        sender.source().npe.sendDebug("Debugger set to: <debug>${action ?: "disabled"}", chat = true)
    }

    @Command("npe debug inspect <data>")
    @Permission("npe.debug")
    @CommandDescription("Developer command for viewing internal information")
    fun debugInspect(
        sender: PlayerSource,
        @Argument("data", suggestions = "debug-inspect") data: String
    ) {
        when (data) {
            "users" -> {
                sender.source().npe.sendDebug(!"<gray>Users:", chat = true)
                for (user in NPEUser.users.values) {
                    sender.source().npe.sendDebug(user.toString(), chat = true)
                }
            }
            else -> sender.source().sendMessage(!"<gray>Unknown inspection type: <red>$data")
        }
    }

    @Command("npe debug simulate <player> <packet> [args]")
    @Permission("npe.debug")
    @CommandDescription("Developer command for sending fake packets on behalf of players")
    fun debugSimulate(
        sender: PlayerSource,
        @Argument("player") target: Player,
        @Argument("packet", suggestions = "debug-wrappers") packet: String,
        @Argument("args", suggestions = "packet-suggestions") @Greedy args: String?
    ) {
        val wrapper = PacketInspection.findWrapper(packet)
            ?: return sender.source().sendMessage(!"<gray>Failed to find packet wrapper <red>$packet")

        val constructor = wrapper.declaredConstructors
            ?.filterNot { it.parameterCount == 1 && ProtocolPacketEvent::class.java.isAssignableFrom(it.parameterTypes[0]) }
            ?.firstOrNull { it.parameterTypes.all { parameter -> DataConverter.isSupported(parameter) } }
            ?: return sender.source().sendMessage(!"<gray>Failed to find a constructor for <red>$packet")

        if (args == null) {
            var command = "/npe debug simulate ${target.name} $packet"

            val fields = wrapper.declaredFields // stupid, but works
                .filter {
                    !Modifier.isStatic(it.modifiers)
                        && Modifier.isPrivate(it.modifiers)
                        && !Modifier.isFinal(it.modifiers)
                }

            sender.source().sendMessage(!"<gray>Packet <primary>$packet<gray> requires:")
            constructor.parameters.forEachIndexed { index, parameter ->
                val name = fields[index].name
                command += " <$name>"
                sender.source().sendMessage(!" <gray>| <primary>$name <dark_gray>- <gray>${parameter.type.name}")
            }

            sender.source().sendMessage(!"<gray>Example: <primary>$command")
            return
        }

        val args = DataConverter.split(args)
            .mapIndexed { i, it -> DataConverter.cast(it, constructor.parameters[i].type) }
            .toTypedArray()

        val instance = constructor.newInstance(*args) as PacketWrapper<*>
        val isClient = wrapper.packageName.endsWith(".client")

        if (isClient) {
            PacketEvents.getAPI().playerManager.receivePacket(target, instance)
            sender.source().sendMessage(!"<gray>Simulating <#8cd17d>sending<gray> a $packet packet as <primary>${target.name}<gray>!")
        } else {
            PacketEvents.getAPI().playerManager.sendPacket(target, instance)
            sender.source().sendMessage(!"<gray>Simulating <#d1847d>receiving<gray> a $packet packet as <primary>${target.name}<gray>!")
        }
    }

    @Suggestions("debug-action")
    fun debugActionSuggestions(ctx: CommandContext<PlayerSource>): List<String> {
        val player = ctx.sender().source()
        return listOf(
            "stop",
            "pps",
            "packets",
            "self-packets",
            "item-spawns"
        ) + player.npe.buffer.keys.map { it.toString() }
    }

    @Suggestions("debug-inspect")
    fun debugInspectSuggestions(ctx: CommandContext<PlayerSource>): List<String> {
        return listOf(
            "users"
        )
    }

    @Suggestions("debug-wrappers")
    fun debugPacketWrappers(ctx: CommandContext<PlayerSource>): List<String> {
        return PacketInspection.allWrappers().map { it.simpleName }
    }

    @Suggestions("packet-suggestions")
    fun debugPacketSuggestions(ctx: CommandContext<PlayerSource>): List<String> {
        // todo
        return emptyList()
    }
}
