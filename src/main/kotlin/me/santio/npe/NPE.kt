package me.santio.npe

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.settings.PacketEventsSettings
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder
import kotlinx.coroutines.runBlocking
import me.santio.npe.base.ProcessorLoader
import me.santio.npe.command.BaseCommand
import me.santio.npe.data.NPEUser
import me.santio.npe.data.PacketDumper
import me.santio.npe.data.npe
import me.santio.npe.ruleset.RuleSet
import me.santio.npe.tasks.BufferResetTask
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
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
        this.saveDefaultConfig()

        commandManager = PaperCommandManager.builder<Source>(PaperSimpleSenderMapper.simpleSenderMapper())
            .executionCoordinator(ExecutionCoordinator.asyncCoordinator<Source>())
            .buildOnEnable(this)
        val annotationParser = AnnotationParser(commandManager, Source::class.java)

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
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, BufferResetTask, 20L, 20L)
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
        val debugColor = TextColor.fromHexString("#f51b55")!!

        val logger: Logger = LoggerFactory.getLogger(NPE::class.java)
        val instance: NPE by lazy { getPlugin(NPE::class.java) }

        val miniMessage = MiniMessage.builder()
            .editTags { builder ->
                builder.resolver(Placeholder.styling(
                    "primary",
                    primaryColor
                ))
                builder.resolver(Placeholder.styling(
                    "debug",
                    debugColor
                ))
                builder.resolver(Placeholder.styling(
                    "body",
                    NamedTextColor.GRAY
                ))
            }
            .postProcessor { component -> component.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE) }
            .build()

        lateinit var commandManager: PaperCommandManager<Source>

        fun broadcast(message: Component) {
            logger.info(serializer.serialize(message))

            for (player in Bukkit.getOnlinePlayers()) {
                player.npe.alert(message)
            }
        }
    }

}
