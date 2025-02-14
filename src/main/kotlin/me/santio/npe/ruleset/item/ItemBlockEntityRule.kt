package me.santio.npe.ruleset.item

import com.github.retrooper.packetevents.protocol.component.ComponentTypes
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound
import com.google.auto.service.AutoService
import me.santio.npe.ruleset.Rule

/**
 * Rule for item block entity data.
 *
 * We only want to accept very certain entity data, we will drop all other data
 *
 * @author santio
  */
@AutoService(Rule::class)
class ItemBlockEntityRule: GenericItemRule<NBTCompound>(
    clazz = NBTCompound::class,
    componentType = ComponentTypes.BLOCK_ENTITY_DATA,
    message = "Invalid Item Block Entity",
) {

    override fun check(value: NBTCompound): Boolean {
        println(value.toString())
        return false
    }

}