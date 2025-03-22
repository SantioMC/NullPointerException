package me.santio.npe.database


/**
 * A configuration for Hibernate, this record holds the URL, username, and password for the database.
 * @author santio
 * @param url the URL to the database
 * @param username the username to use to connect to the database
 * @param password the password to use to connect to the database
 */
data class HibernateConfig(
    val url: String,
    val username: String? = null,
    val password: String? = null
) 