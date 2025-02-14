package me.santio.npe.database

import me.santio.npe.NPE
import org.flywaydb.core.Flyway

object Database {

    fun migrate() {
        Flyway.configure(this.javaClass.classLoader)
            .dataSource(NPE.database.pool)
            .locations("classpath:migration")
            .sqlMigrationPrefix("")
            .sqlMigrationSeparator("_")
            .load()
            .migrate()
    }

}