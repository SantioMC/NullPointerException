package me.santio.npe

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.settings.PacketEventsSettings
import gg.ingot.iron.Iron
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder
import kotlinx.coroutines.runBlocking
import me.santio.npe.base.ProcessorLoader
import me.santio.npe.command.BaseCommand
import me.santio.npe.data.NPEUser
import me.santio.npe.data.PacketDumper
import me.santio.npe.data.npe
import me.santio.npe.database.Database
import me.santio.npe.database.column.UUIDAdapter
import me.santio.npe.ruleset.RuleSet
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin
import org.incendo.cloud.annotations.AnnotationParser
import org.incendo.cloud.execution.ExecutionCoordinator
import org.incendo.cloud.paper.PaperCommandManager
import org.incendo.cloud.paper.util.sender.PaperSimpleSenderMapper
import org.incendo.cloud.paper.util.sender.Source
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.io.path.createDirectories


class NPE: JavaPlugin() {

    @Suppress("UnstableApiUsage")
    override fun onLoad() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().load();

        val settings: PacketEventsSettings = PacketEvents.getAPI().settings
        settings.checkForUpdates(false)
    }

    @Suppress("UnstableApiUsage")
    override fun onEnable() {
        this.dataPath.createDirectories()

        database = Iron("jdbc:sqlite:${this.dataPath.resolve("data.db").toAbsolutePath()}") {
            defaultAdapters {
                adapter(UUID::class, UUIDAdapter)
            }
        }.connect()

        commandManager = PaperCommandManager.builder<Source>(PaperSimpleSenderMapper.simpleSenderMapper())
            .executionCoordinator(ExecutionCoordinator.asyncCoordinator<Source>())
            .buildOnEnable(this)
        val annotationParser = AnnotationParser(commandManager, Source::class.java)

        Database.migrate()
        ProcessorLoader.load()
        RuleSet.loadRules()
        PacketDumper.create()

        ServiceLoader.load(Listener::class.java, this.javaClass.classLoader).forEach {
            Bukkit.getPluginManager().registerEvents(it, this)
        }

        ServiceLoader.load(BaseCommand::class.java, this.javaClass.classLoader).forEach {
            annotationParser.parse(it)
        }

        PacketEvents.getAPI().init();
    }

    override fun onDisable() {
        PacketEvents.getAPI().terminate();

        runBlocking {
            for (player in NPEUser.users.values) {
                player.save()
            }
        }
    }

    companion object {
        private val serializer = PlainTextComponentSerializer.plainText()
        val primaryColor = TextColor.fromHexString("#f44d1a")!!
        val logger: Logger = LoggerFactory.getLogger(NPE::class.java)
        val instance: NPE by lazy { getPlugin(NPE::class.java) }

        lateinit var database: Iron
        lateinit var commandManager: PaperCommandManager<Source>

        fun broadcast(message: Component) {
            logger.info(serializer.serialize(message))

            for (player in Bukkit.getOnlinePlayers()) {
                player.npe.alert(message)
            }
        }
    }

}
