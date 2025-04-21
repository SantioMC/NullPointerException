package me.santio.npe.metrics

import me.santio.npe.config.config
import org.bstats.charts.SimplePie

object LoggingEnabledChat: SimplePie("logging_enabled", {
    if (config("npe.log-packets.enabled", true)) "Enabled" else "Disabled"
})