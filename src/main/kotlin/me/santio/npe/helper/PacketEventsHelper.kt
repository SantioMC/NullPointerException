package me.santio.npe.helper

import com.github.retrooper.packetevents.resources.ResourceLocation
import net.kyori.adventure.key.Key

fun ResourceLocation.toKey(): Key {
    return Key.key(this.namespace, this.key)
}