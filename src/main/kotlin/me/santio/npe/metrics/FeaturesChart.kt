package me.santio.npe.metrics

import me.santio.npe.NPE
import me.santio.npe.config.config
import org.bstats.charts.DrilldownPie

object FeaturesChart: DrilldownPie("features", {
    val modules = NPE.instance.config
        .getConfigurationSection("modules")
        ?.getKeys(false) ?: listOf()

    modules.associateWith {
        val enabled = config("modules.$it.enabled", false)
        if (enabled) mapOf("Enabled" to 1) else mapOf("Disabled" to 1)
    }
})