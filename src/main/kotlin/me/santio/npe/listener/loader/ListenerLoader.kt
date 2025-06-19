package me.santio.npe.listener.loader

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import me.santio.npe.listener.loader.annotations.Toggle
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.plugin.EventExecutor
import org.bukkit.plugin.java.JavaPlugin
import java.lang.reflect.Method
import java.util.*
import kotlin.reflect.jvm.kotlinFunction

/**
 * A helper object for loading bukkit listeners
 * @author santio
 */
class ListenerLoader(val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())) {

    @Suppress("UNCHECKED_CAST")
    private fun registerMethod(plugin: JavaPlugin, listener: Listener, method: Method) {
        method.isAccessible = true
        val toggle = method.annotations.find { it is Toggle } as? Toggle

        if (toggle != null && !plugin.config.getBoolean(toggle.key, false)) {
            // Listener is disabled, don't register it
            return
        }

        if (!method.isAnnotationPresent(EventHandler::class.java)) return
        val handler = method.getAnnotation(EventHandler::class.java) ?: return

        val parameter = method.parameters.firstOrNull() ?: return
        val type = parameter.type as? Class<out Event> ?: run {
            plugin.logger.severe("Invalid event argument provided, ${parameter.type.simpleName} is not a bukkit event!")
            return
        }

        val isSuspend = method.kotlinFunction?.isSuspend ?: false
        val executor = when(isSuspend) {
            true -> SuspendingEventExecutor(scope, method, type)
            false -> EventExecutor.create(method, type)
        }

        plugin.server.pluginManager.registerEvent(
            type,
            listener,
            handler.priority,
            executor,
            plugin,
            handler.ignoreCancelled
        )
    }

    fun load(plugin: JavaPlugin, instance: Listener) {
        val clazz = instance.javaClass
        val toggle = clazz.annotations.find { it is Toggle } as? Toggle

        if (toggle != null && !plugin.config.getBoolean(toggle.key, toggle.default)) {
            // Listener is disabled, don't register it
            return
        }

        val methods = clazz.methods.plus(clazz.declaredMethods).toSet().filterNot {
            it.isBridge || it.isSynthetic
        }

        methods.forEach {
            registerMethod(plugin, instance, it)
        }
    }

    fun load(
        plugin: JavaPlugin = JavaPlugin.getProvidingPlugin(ListenerLoader::class.java),
        classLoader: ClassLoader = plugin::class.java.classLoader
    ) {
        ServiceLoader.load(Listener::class.java, classLoader).forEach {
            load(plugin, it)
        }
    }

    fun reload(plugin: JavaPlugin = JavaPlugin.getProvidingPlugin(ListenerLoader::class.java)) {
        HandlerList.getRegisteredListeners(plugin).forEach {
            HandlerList.unregisterAll(it.listener)
        }

        this.load(plugin)
    }

}