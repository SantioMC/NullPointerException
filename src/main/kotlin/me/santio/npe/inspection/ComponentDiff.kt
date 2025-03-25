package me.santio.npe.inspection

import com.github.retrooper.packetevents.protocol.component.ComponentType
import me.santio.npe.helper.mm
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder

sealed class ComponentDiff(
    val message: Component
) {

    data class Missing(val missing: ComponentType<*>): ComponentDiff(
        mm("<red>- <white><missing>", Placeholder.unparsed("missing", missing.name.toString()))
    )

    data class Extra(val extra: ComponentType<*>): ComponentDiff(
        mm("<green>+ <white><extra>", Placeholder.unparsed("extra", extra.name.toString()))
    )

    data class Mismatch(val value: Any, val expected: Any): ComponentDiff(
        mm(
            "<yellow>~ <white><value> != <white><expected>",
            Placeholder.unparsed("value", PacketInspection.readable(value)),
            Placeholder.unparsed("expected", PacketInspection.readable(expected))
        )
    )

}