package me.santio.npe.ruleset.item

import com.github.retrooper.packetevents.protocol.component.ComponentTypes
import com.github.retrooper.packetevents.protocol.component.builtin.item.ItemProfile
import com.google.auto.service.AutoService
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import me.santio.npe.base.Processor
import me.santio.npe.inspection.URLInspector
import me.santio.npe.ruleset.Rule
import java.util.*

/**
 * Rule for player head profile components.
 *
 * The profile data should be valid, have no missing data, no additional data, and resolve to a valid URL.
 *
 * @author santio
  */
@AutoService(Rule::class)
class ItemProfileRule: GenericItemRule<ItemProfile>(
    clazz = ItemProfile::class,
    componentType = ComponentTypes.PROFILE,
    config = "profile-data",
    message = "Invalid Player Head Profile"
) {

    private val decoder = Base64.getDecoder()
    private val gson = Gson()

    private fun validateTextures(processor: Processor, value: String): Boolean {
        val allowedDomains = config(processor, "allowed_domains", listOf("minecraft.net"))

        if (value.isBlank()) return false
        val decoded = String(decoder.decode(value))

        // See if the texture conforms to the format
        return try {
            val textureData = gson.fromJson(decoded, TextureData::class.java)
            val url = textureData.textures.skin.url

            if (url.isBlank()) return false
            if (url.length > 2048) return false
            if (url.contains("\n")) return false

            val data = URLInspector.inspect(url)
            data.domain in allowedDomains
        } catch(e: Exception) {
            false
        }
    }

    override fun check(processor: Processor, value: ItemProfile): Boolean {
        value.name?.let { name ->
            if (name.trim().isBlank()) return false
            if (name.length > 256) return false
        }

        // Check properties
        if (value.properties.size > config(processor, "max_properties_data", 8)) return false // Too many properties

        value.properties.forEach { property ->
            if (property.name.isBlank() || property.value.isBlank() || property.name.length > 128) return false
            if (property.name == "textures" && !validateTextures(processor, property.value)) return false
            if (property.value.length > 2048) return false
        }

        return true
    }

    private data class TextureData(
        val textures: Textures
    )

    private data class Textures(
        @SerializedName("SKIN")
        val skin: Texture
    )

    private data class Texture(
        val url: String
    )

}