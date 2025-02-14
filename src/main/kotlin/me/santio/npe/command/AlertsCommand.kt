package me.santio.npe.command

import com.google.auto.service.AutoService
import me.santio.npe.data.npe
import me.santio.npe.helper.not
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.CommandDescription
import org.incendo.cloud.annotations.Permission
import org.incendo.cloud.paper.util.sender.PlayerSource

@AutoService(BaseCommand::class)
class AlertsCommand: BaseCommand {

    @Command("npe alerts")
    @Permission("npe.alerts")
    @CommandDescription("Turn alerts on or off for all NPE messages")
    fun toggleAlerts(sender: PlayerSource) {
        val alerts = sender.source().npe.data.alerts
        sender.source().npe.data.alerts = !alerts
        sender.source().sendMessage(!"<body>Alerts are now <primary>${if (!alerts) "enabled" else "disabled"}</primary>")
    }

}
