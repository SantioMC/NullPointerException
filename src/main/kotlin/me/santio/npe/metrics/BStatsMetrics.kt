package me.santio.npe.metrics

import org.bstats.bukkit.Metrics
import org.bukkit.plugin.java.JavaPlugin

object BStatsMetrics {

    private lateinit var metrics: Metrics
    val alerts = mutableMapOf<String, Int>()

    fun register(plugin: JavaPlugin) {
        metrics = Metrics(plugin, 25546)

        metrics.addCustomChart(FeaturesChart)
        metrics.addCustomChart(LoggingEnabledChat)
        metrics.addCustomChart(TotalAlertsChart)
        metrics.addCustomChart(ServerHostChart)
    }

    fun recordAlert(id: String) {
        alerts[id] = (alerts[id] ?: 0) + 1
    }

}