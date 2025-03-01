package me.santio.npe.command

import com.google.auto.service.AutoService
import me.santio.npe.data.npe
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

    @Command("npe debug [action]")
    @Permission("npe.debug")
    @CommandDescription("Developer command for viewing internal information")
    fun debug(
        sender: PlayerSource,
        @Argument("action", suggestions = "debug-action") action: String?
    ) {
        sender.source().npe.debug(action)
        sender.source().npe.sendDebug("Debugger set to: <debug>$action", chat = true)
    }

    @Suggestions("debug-action")
    fun debugActionSuggestions(ctx: CommandContext<PlayerSource>): List<String> {
        val player = ctx.sender().source()
        return player.npe.buffer.keys.map { it.toString() } + listOf("pps")
    }

}
