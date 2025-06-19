package me.santio.npe.listener.loader

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.bukkit.event.Event
import org.bukkit.event.Listener
import org.bukkit.plugin.EventExecutor
import java.lang.reflect.Method
import kotlin.reflect.full.callSuspend
import kotlin.reflect.jvm.kotlinFunction

internal class SuspendingEventExecutor(
    private val scope: CoroutineScope,
    private val method: Method,
    private val type: Class<out Event>,
): EventExecutor {

    override fun execute(listener: Listener, event: Event) {
        if (!type.isInstance(event)) return

        scope.launch {
            method.kotlinFunction?.callSuspend(listener, event)
                ?: error("Failed to call suspending function")
        }
    }

}