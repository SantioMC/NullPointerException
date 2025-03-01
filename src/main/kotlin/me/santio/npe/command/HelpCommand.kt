package me.santio.npe.command

import com.google.auto.service.AutoService
import me.santio.npe.NPE
import me.santio.npe.helper.not
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.CommandDescription
import org.incendo.cloud.component.CommandComponent
import org.incendo.cloud.paper.util.sender.Source

@AutoService(BaseCommand::class)
class HelpCommand: BaseCommand {

    private fun sendHelp(sender: Source) {
        sender.source().sendMessage(!"<body>Commands for <primary>NullPointerException</primary>:")
        for (command in NPE.commandManager.commands()) {
            if (command.components().size <= 1) continue
            val commandName = command.components()
                .filter { it.type() == CommandComponent.ComponentType.LITERAL }
                .joinToString(" ") { it.name() }

            val hasPermission = command.commandPermission().permissions()
                .all { sender.source().hasPermission(it.permissionString()) }
            if (!hasPermission) continue

            val description = command.commandDescription().takeIf { !it.isEmpty }
                ?.description()
                ?.textDescription()
                ?: "<red>No description available"

            sender.source().sendMessage(!"<click:suggest_command:'/$commandName'><primary>/$commandName <dark_gray>-</dark_gray> <gray>$description")
        }
    }

    @Command("npe")
    fun base(sender: Source) {
        val hasPermission = NPE.commandManager.commands()
            .filter { it.components().size > 1 && it.components().getOrNull(1)?.name() != "help" }
            .any {
                it.commandPermission().permissions().any { sender.source().hasPermission(it.permissionString()) }
            }

        if (!hasPermission) {
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