package me.santio.npe.inspection

/**
 * A super simple url inspector to get parts of a url by splitting it up
 * @author santio
 */
object URLInspector {

    fun inspect(url: String): UrlData {
        val protocol = url.substring(0, url.indexOf("://")) // http
        val query = url.substring(url.indexOf("?") + 1).takeIf { it.isNotBlank() }
        val parts = url.substring(protocol.length + 3, query?.length ?: url.length).split("/")

        val fullDomain = parts[0]
        val (domain, port) = fullDomain.split(":").let {
            if (it.size == 1) it[0] to null
            else it[0] to it[1].toInt()
        }

        val path = parts.drop(1).joinToString("/")
        val subdomain = domain.split(".").takeIf { it.size > 1 }?.let { it[0] }
        val baseDomain = if (subdomain != null) domain.substring(subdomain.length + 1) else domain

        return UrlData(protocol, subdomain, baseDomain, port, path, query)
    }

    data class UrlData(
        val protocol: String,
        val subdomain: String?,
        val domain: String,
        val port: Int?,
        val path: String,
        val query: String?
    )

}