package me.santio.npe.base

import com.github.retrooper.packetevents.PacketEvents
import me.santio.npe.NPE
import java.util.*

object ProcessorLoader {

    val loaded = mutableSetOf<Processor>()

    fun load() {
        var processors = 0
        var modules = 0

        val loader = ServiceLoader.load(
            Processor::class.java,
            this.javaClass.classLoader
        )

        loader.forEach {
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