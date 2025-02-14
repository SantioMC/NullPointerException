package me.santio.npe.command

import com.google.auto.service.AutoService
import me.santio.npe.NPE
import me.santio.npe.helper.not
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.CommandDescription
import org.incendo.cloud.paper.util.sender.Source

@AutoService(BaseCommand::class)
class HelpCommand: BaseCommand {

    private fun sendHelp(sender: Source) {
        sender.source().sendMessage(!"<body>Commands for <primary>NullPointerException</primary>:")
        for (command in NPE.commandManager.commands()) {
            if (command.components().size <= 1) continue

            val hasPermission = command.commandPermission().permissions()
                .all { sender.source().hasPermission(it.permissionString()) }
            if (!hasPermission) continue

            val description = command.commandDescription().takeIf { !it.isEmpty }
                ?.description()
                ?.textDescription()
                ?: "<red>No description available"

            sender.source().sendMessage(!"<click:suggest_command:'/$command'><primary>/$command <dark_gray>-</dark_gray> <gray>$description")
        }
    }

    @Command("npe")
    fun base(sender: Source) {
        if (!sender.source().hasPermission("npe.command")) {
            sender.source().sendMessage(!"<body>This server is monitored by <primary>NullPointerException</primary>!")
            sender.source().sendMessage(!"<body>You are not permitted to access this command!")
            return
        }

        sendHelp(sender)
    }

    @Command("npe help")
    @CommandDescription("Shows a list of commands for NPE")
    fun help(sender: Source) {
        this.base(sender)
    }

}