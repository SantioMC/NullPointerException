package me.santio.npe.inspection

import com.github.retrooper.packetevents.event.PacketEvent
import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.event.PacketSendEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType.Play
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon
import com.github.retrooper.packetevents.wrapper.PacketWrapper
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.lang.reflect.InvocationTargetException
import java.util.*

object PacketInspection {

    private val ignored = mutableSetOf<String>("copy", "read", "write")
    private val lookup: MethodHandles.Lookup = MethodHandles.lookup()

    // This is really stupid, but I don't believe there's a way to map the packet
    private fun getWrapperPacketClassName(wrapper: PacketTypeCommon): String {
        val value = wrapper as Enum<*>?
        val group = wrapper.javaClass.getName()

        val namespace = group.substringBefore('$').replace("$", "")
        val wrapperName = "Wrapper$namespace${toPascalCase(value!!.name)}".replace("ClientClient", "Client")

        val wrapperGroup = group.replace("protocol.packettype.PacketType", "wrapper")
            .replace("$", ".")
            .lowercase()

        return "$wrapperGroup.$wrapperName"
    }

    @Suppress("UNCHECKED_CAST")
    fun getWrapper(event: PacketEvent, packetType: PacketTypeCommon): PacketWrapper<*>? {
        val wrapperClassName = getWrapperPacketClassName(packetType)

        val wrapperClass: Class<out PacketWrapper<*>?>
        try {
            wrapperClass = Class.forName(wrapperClassName) as Class<out PacketWrapper<*>>
        } catch (_: ClassNotFoundException) {
            return null
        }

        var packetClass: Class<*>? = null
        if (event is PacketReceiveEvent) {
            packetClass = PacketReceiveEvent::class.java
        } else if (event is PacketSendEvent) {
            packetClass = PacketSendEvent::class.java
        }

        if (packetClass == null) {
            return null
        }

        val methodType = MethodType.methodType(
            Void.TYPE,
            packetClass
        )

        val constructor: MethodHandle
        try {
            constructor = lookup.findConstructor(wrapperClass, methodType)
        } catch (e: NoSuchMethodException) {
            e.printStackTrace()
            return null
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
            return null
        }

        try {
            return constructor.invoke(event) as PacketWrapper<*>?
        } catch (e: Throwable) {
            e.printStackTrace()
            return null
        }
    }

    fun getData(wrapper: PacketWrapper<*>): MutableMap<String, String> {
        // Find all getters for the wrapper
        val methods = wrapper.javaClass.getDeclaredMethods()

        val data: MutableMap<String, String> = mutableMapOf(
            "wrapper" to wrapper.javaClass.getSimpleName()
        )

        for (method in methods) {
            if (ignored.contains(method.name) || method.parameterCount != 0) continue

            try {
                val name = getGetterName(method.name).lowercase()
                val value = method.invoke(wrapper)
                data.put(name, value?.toString() ?: "null")
            } catch (e: InvocationTargetException) {
                e.printStackTrace()
            } catch (e: IllegalAccessException) {
                e.printStackTrace()
            }
        }

        return data
    }

    fun inspect(event: PacketEvent, packetType: PacketTypeCommon, player: Player): Component? {
        val wrapper = getWrapper(event, packetType)
        if (wrapper == null) return null

        val data = getData(wrapper)
        val isSent = packetType is Play.Server // Server -> Client

        var hover: Component = Component.newline()
        for (entry in data.entries) {
            hover = hover.append(
                Component.text(entry.key, NamedTextColor.AQUA)
                    .append(Component.text(": ", NamedTextColor.GRAY))
                    .append(Component.text(entry.value, NamedTextColor.WHITE))
                    .appendNewline()
            )
        }

        return Component.text(player.name + ": ", NamedTextColor.GRAY)
            .append(
                Component.text(
                    if (isSent) "<- " else "-> ",
                    if (isSent) NamedTextColor.RED else NamedTextColor.GREEN
                )
            )
            .append(
                Component.text(
                    toPascalCase(packetType.name),
                    NamedTextColor.AQUA
                )
            ).hoverEvent(HoverEvent.showText(hover))
    }

    /**
     * Converts snake case to pascal case
     * @param string The string in snake case
     * @return The provided string, converted to pascal case
     */
    private fun toPascalCase(string: String): String {
        val split = string.split("_".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val builder = StringBuilder()

        for (part in split) {
            builder.append(part.substring(0, 1).uppercase(Locale.getDefault()))
            builder.append(part.substring(1).lowercase(Locale.getDefault()))
        }

        return builder.toString()
    }

    /**
     * Removes the "get" or "is" prefix from a method name
     * @param methodName The method name
     * @return The method name without the prefix
     */
    private fun getGetterName(methodName: String): String {
        val chars = methodName.toCharArray()
        var index = 0

        for (i in chars.indices) {
            if (chars[i] >= 'A' && chars[i] <= 'Z') {
                index = i
                break
            }
        }

        return methodName.substring(index)
    }


}