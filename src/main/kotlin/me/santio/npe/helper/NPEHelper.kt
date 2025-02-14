package me.santio.npe.helper

import me.santio.npe.NPE
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder

private val miniMessage = MiniMessage.builder()
    .editTags { builder ->
        builder.resolver(Placeholder.styling(
            "primary",
            NPE.primaryColor
        ))
        builder.resolver(Placeholder.styling(
            "body",
            NamedTextColor.GRAY
        ))
    }
    .postProcessor { component -> component.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE) }
    .build()

operator fun String.not(): Component {
    return miniMessage.deserialize(this)
}