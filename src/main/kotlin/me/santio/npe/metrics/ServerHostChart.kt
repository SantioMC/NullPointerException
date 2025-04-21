package me.santio.npe.metrics

import me.santio.npe.NPE
import org.bstats.charts.SimplePie

object ServerHostChart: SimplePie("server_host", {
    NPE.getServerHost()
})