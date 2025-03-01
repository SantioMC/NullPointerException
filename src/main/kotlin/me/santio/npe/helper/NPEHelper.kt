package me.santio.npe.helper

import me.santio.npe.NPE
import net.kyori.adventure.text.Component

operator fun String.not(): Component {
    return NPE.miniMessage.deserialize(this)
}