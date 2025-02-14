package me.santio.npe.command

import com.google.auto.service.AutoService
import me.santio.npe.data.npe
import me.santio.npe.helper.not
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.CommandDescription
import org.incendo.cloud.annotations.Permission
import org.incendo.cloud.paper.util.sender.PlayerSource

@AutoService(BaseCommand::class)
class BypassCommand: BaseCommand {

    @Command("npe bypass")
    @Permission("npe.bypass")
    @CommandDescription("Toggle NPE bypass for yourself")
    fun toggleBypass(sender: PlayerSource) {
        val bypass = sender.source().npe.data.ignoreBypass
        sender.source().npe.data.ignoreBypass = !bypass
        sender.source().sendMessage(!"<body>You are now <primary>${if (!bypass) "not bypassing" else "bypassing"}</primary> NPE checks")
    }

}
