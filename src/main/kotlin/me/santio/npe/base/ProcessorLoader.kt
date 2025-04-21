package me.santio.npe.base

import com.github.retrooper.packetevents.PacketEvents
import me.santio.npe.NPE
import org.slf4j.LoggerFactory
import java.util.*

object ProcessorLoader {

    private val logger = LoggerFactory.getLogger(ProcessorLoader::class.java)
    val loaded = mutableSetOf<Processor>()

    fun load() {
        var processors = 0
        var modules = 0

        val loader = ServiceLoader.load(
            Processor::class.java,
            this.javaClass.classLoader
        )

        loader.forEach {
            if (!it.config("enabled", false)) {
                logger.info("Skipping disabled processor ${it.id}, you can enable it with: modules.${it.config}.enabled")
                return@forEach
            }

            if (it is Module) modules++
            else processors++

            PacketEvents.getAPI().eventManager.registerListener(
                it,
                it.priority
            )

            loaded.add(it)
        }

        NPE.logger.info("Loaded {} modules and {} processors", modules, processors)
    }

    inline fun <reified P: Processor> get(): P {
        return loaded.first { it is P } as P
    }

}