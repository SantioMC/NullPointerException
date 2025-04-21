package me.santio.npe.inspection

import com.github.retrooper.packetevents.event.PacketEvent
import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.event.PacketSendEvent
import com.github.retrooper.packetevents.event.ProtocolPacketEvent
import com.github.retrooper.packetevents.protocol.component.ComponentTypes
import com.github.retrooper.packetevents.protocol.item.ItemStack
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon
import com.github.retrooper.packetevents.wrapper.PacketWrapper
import com.google.common.reflect.ClassPath
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import me.santio.npe.helper.mm
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.entity.Player
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.lang.reflect.InvocationTargetException
import java.util.*

object PacketInspection {

    private val ignored = mutableSetOf<String>("copy", "read", "write")
    private val lookup: MethodHandles.Lookup = MethodHandles.lookup()
    private val serializer = LegacyComponentSerializer.legacyAmpersand()
    private val logger = LoggerFactory.getLogger(PacketInspection::class.java)
    private var wrappers: List<Class<PacketWrapper<*>>> = emptyList()

    private val gson: Gson = GsonBuilder()
        .serializeNulls()
        .create()

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

    /**
     * Find the class of a wrapper by its name
     * @param name The name of the wrapper (ex: WrapperPlayClientSelectBundleItem)
     * @return The class for the wrapper
     */
    fun findWrapper(name: String): Class<PacketWrapper<*>>? {
        val root = PacketWrapper::class.java.packageName
        val parts = getWrapperParts(name)
        if (parts.size < 3) return null

        @Suppress("UNCHECKED_CAST")
        return Class.forName(
            "$root.${parts[1]}.${parts[2]}.$name"
        ) as? Class<PacketWrapper<*>>
    }

    private fun getWrapperParts(wrapper: String): List<String> {
        val parts = mutableListOf<String>()
        var buffer = ""

        wrapper.forEach {
            if (it.isUpperCase() && buffer.isNotBlank()) {
                parts.add(buffer)
                buffer = ""
            }

            buffer += it.lowercase()
        }

        parts.add(buffer)
        return parts
    }

    @Suppress("UNCHECKED_CAST")
    fun getWrapper(event: PacketEvent, packetType: PacketTypeCommon): PacketWrapper<*>? {
        val wrapperClassName = getWrapperPacketClassName(packetType)

        val wrapperClass: Class<out PacketWrapper<*>?>
        try {
            wrapperClass = Class.forName(wrapperClassName) as Class<out PacketWrapper<*>>
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
            logger.warn("Failed to find wrapper class for ${packetType.name}, name tried: $wrapperClassName")
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

    fun readable(value: Any?): String {
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

    private fun prettyPrint(value: Any?): Component {
        if (value is Component) {
            return value
        }

        return Component.text(try {
            gson.toJson(value)
        } catch (e: Exception) {
            "${e.javaClass.simpleName}: ${e.message}"
        })
    }

    /**
     * Reads an ItemStack from PacketEvents and converts it to a nicely readable component
     * @param item The item stack to read
     * @return A component representing the item stack
     */
    fun readItem(item: ItemStack): Component {
        if (item.isEmpty) return Component.text("AIR", NamedTextColor.GRAY)

        var data = Component.empty()
        val clipboard = StringBuilder()

        data = data.append(Component.newline())
        data = data.append(mm("<body>Amount: <primary><amount><br>", Placeholder.unparsed("amount", "" + item.amount)))
        data = data.append(mm("<body>Components: <primary><count><br>", Placeholder.unparsed("count", "" + item.components.base.size)))

        clipboard.append("""
            Amount: ${item.amount}
            Components: ${item.components.base.size}
            
        """.trimIndent())

        for (component in ComponentTypes.values()) {
            val componentData = item.getComponent(component)
            if (componentData.isEmpty) continue

            var value = prettyPrint(componentData.get())
            val raw = try {
                gson.toJson(componentData.get())
            } catch (e: Exception) {
                "${e.javaClass.simpleName}: ${e.message}"
            }

            clipboard.append("\n| ${component.name}\n${raw}\n")

            if (raw.length > 2048) {
                value = Component.text("Too long to display, please copy to clipboard", NamedTextColor.RED)
            }

            data = data.append(mm(
                "<br><primary>│ <body><name><br><white><value><br>",
                Placeholder.unparsed("name", component.name.toString()),
                Placeholder.component("value", value)
            ))
        }

        data = data.append(mm("<br><yellow>Click to copy to clipboard<br>"))
        val hoverData = clipboard.toString()

        return Component.text(item.type.name.toString(), NamedTextColor.GRAY)
            .hoverEvent(HoverEvent.showText(data))
            .clickEvent(ClickEvent.copyToClipboard(
                hoverData.takeIf { it.length <= 20_480 } ?: hoverData.substring(0, 20_480)
            ))
    }

    /**
     * Gets the different components between two item-stacks
     * @param original The original item-stack
     * @param expected The expected item-stack
     * @return The different components between the two item-stacks
     */
    fun diff(original: ItemStack, expected: ItemStack): List<ComponentDiff> {
        val diff = mutableListOf<ComponentDiff>()

        for (component in ComponentTypes.values()) {
            val originalComponent = original.getComponent(component)
            val expectedComponent = expected.getComponent(component)

            if (originalComponent.isEmpty && expectedComponent.isEmpty) {
                continue
            } else if (originalComponent.isPresent && expectedComponent.isEmpty) {
                diff.add(ComponentDiff.Extra(component))
            } else if (originalComponent.isEmpty && expectedComponent.isPresent) {
                diff.add(ComponentDiff.Missing(component))
            } else if (originalComponent.get() != expectedComponent.get()) {
                diff.add(ComponentDiff.Mismatch(originalComponent.get(), expectedComponent.get()))
            }
        }

        return diff
    }

    fun allWrappers(): List<Class<*>> {
        if (wrappers.isNotEmpty()) return wrappers

        return ClassPath.from(this.javaClass.classLoader)
            .getTopLevelClassesRecursive(PacketWrapper::class.java.packageName)
            .map { it.load() }
            .filter {
                PacketWrapper::class.java.isAssignableFrom(it)
                    && it != PacketWrapper::class.java
            }
            .map {
                @Suppress("UNCHECKED_CAST")
                it as Class<PacketWrapper<*>>
            }
            .also {
                this.wrappers = it
            }
    }
}