package me.santio.npe.command

import com.google.auto.service.AutoService
import me.santio.npe.data.NPEUser
import me.santio.npe.data.npe
import me.santio.npe.helper.not
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.CommandDescription
import org.incendo.cloud.annotations.Permission
import org.incendo.cloud.annotations.suggestion.Suggestions
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.paper.util.sender.PlayerSource

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

}
