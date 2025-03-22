package me.santio.npe.helper

import me.santio.npe.NPE
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver

operator fun String.not(): Component {
    return NPE.miniMessage.deserialize(this)
}

fun mm(text: String, vararg resolvers: TagResolver): Component {
    return NPE.miniMessage.deserialize(text, *resolvers)
}