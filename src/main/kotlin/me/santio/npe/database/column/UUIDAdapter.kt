package me.santio.npe.database.column

import gg.ingot.iron.serialization.ColumnAdapter
import java.util.*

object UUIDAdapter: ColumnAdapter<String, UUID> {
    override fun toDatabaseValue(value: UUID): String {
        return value.toString()
    }

    override fun fromDatabaseValue(value: String): UUID {
        return UUID.fromString(value)
    }
}