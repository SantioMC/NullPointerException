package me.santio.npe.metrics

import org.bstats.charts.SingleLineChart

object TotalAlertsChart: SingleLineChart("total_alerts", {
    BStatsMetrics.alerts.values.sum()
})