package me.santio.npe.command

import com.google.auto.service.AutoService
import me.santio.npe.NPE
import me.santio.npe.helper.not
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.CommandDescription
import org.incendo.cloud.annotations.Permission
import org.incendo.cloud.paper.util.sender.PlayerSource

@AutoService(BaseCommand::class)
class ReloadCommand: BaseCommand {

    @Command("npe reload")
    @Permission("npe.reload")
    @CommandDescription("Reload all of NPEs configuration live")
    fun reload(sender: PlayerSource) {
        try {
            NPE.instance.reloadConfig()
            sender.source().sendMessage(!"<body>You have <primary>successfully</primary> reloaded NPEs configuration")
        } catch (_: Exception) {
            sender.source().sendMessage(!"<body>Failed to reload NPEs configuration, see the console for information")
        }
    }

}
