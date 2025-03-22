package me.santio.npe.inspection

import com.github.retrooper.packetevents.event.PacketEvent
import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.event.PacketSendEvent
import com.github.retrooper.packetevents.event.ProtocolPacketEvent
import com.github.retrooper.packetevents.protocol.item.ItemStack
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon
import com.github.retrooper.packetevents.wrapper.PacketWrapper
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.entity.Player
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.lang.reflect.InvocationTargetException
import java.util.*

object PacketInspection {

    private val ignored = mutableSetOf<String>("copy", "read", "write")
    private val lookup: MethodHandles.Lookup = MethodHandles.lookup()
    private val serializer = LegacyComponentSerializer.legacyAmpersand()

    // This is really stupid, but I don't believe there's a way to map the packet
    private fun getWrapperPacketClassName(wrapper: PacketTypeCommon): String {
        val value = wrapper as Enum<*>?
        val group = wrapper.javaClass.getName()

        val namespace = group.substringAfter('$').replace("$", "")
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

    private fun readable(value: Any?): String {
        if (value == null) return "null"

        return when (value) {
            is ItemStack -> value.type.name.toString()
            is Optional<*> -> value.map { readable(it) }.orElse("Optional.empty()")
            is Component -> serializer.serialize(value)
            else -> {
                val method = value.javaClass.getMethod("toString")
                if (method.declaringClass == java.lang.Object::class.java) {
                    return value.javaClass.simpleName
                }

                return value.toString()
            }
        }
    }

    fun getData(player: String, wrapper: PacketWrapper<*>): MutableMap<String, String> {
        // Find all getters for the wrapper
        val methods = wrapper.javaClass.getDeclaredMethods()

        val data: MutableMap<String, String> = mutableMapOf(
            "player" to player,
            "wrapper" to wrapper.javaClass.getSimpleName()
        )

        for (method in methods) {
            if (ignored.contains(method.name) || method.parameterCount != 0) continue
            method.isAccessible = true

            try {
                val name = getGetterName(method.name).lowercase()
                val value = method.invoke(wrapper)
                data.put(name, readable(value))
            } catch (e: InvocationTargetException) {
                e.printStackTrace()
            } catch (e: IllegalAccessException) {
                e.printStackTrace()
            }
        }

        return data
    }

    fun inspect(event: ProtocolPacketEvent): Component? {
        val wrapper = getWrapper(event, event.packetType)
        if (wrapper == null) return null

        val player = event.getPlayer<Player>()?.name ?: "null"
        val data = getData(player, wrapper)
        var hover: Component = Component.newline()

        for (entry in data.entries) {
            hover = hover.append(
                Component.text(entry.key, NamedTextColor.AQUA)
                    .append(Component.text(": ", NamedTextColor.GRAY))
                    .append(Component.text(entry.value, NamedTextColor.WHITE))
                    .appendNewline()
            )
        }

        return Component.text(
            toPascalCase(event.packetType.name),
            NamedTextColor.AQUA
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