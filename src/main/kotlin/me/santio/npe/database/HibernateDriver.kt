package me.santio.npe.database

import com.google.errorprone.annotations.CanIgnoreReturnValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import org.hibernate.Session
import org.hibernate.SessionFactory
import org.hibernate.boot.registry.BootstrapServiceRegistry
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder
import org.hibernate.boot.registry.StandardServiceRegistryBuilder
import org.hibernate.boot.registry.classloading.internal.TcclLookupPrecedence
import org.hibernate.cfg.Configuration
import org.hibernate.query.NativeQuery
import org.hibernate.service.ServiceRegistry
import org.intellij.lang.annotations.Language
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import java.util.function.Consumer
import javax.annotation.WillClose

/**
 * Handles connecting to the database using Hibernate.
 * @author santio
 */
class HibernateDriver {
    var configuration: Configuration? = null
        private set
    var serviceRegistry: ServiceRegistry? = null
        private set
    var bootstrapServiceRegistry: BootstrapServiceRegistry? = null
        private set

    private var sessionFactory: SessionFactory? = null

    fun connect(credentials: HibernateConfig): HibernateDriver {
        val builder: BootstrapServiceRegistryBuilder = BootstrapServiceRegistryBuilder().disableAutoClose()
        builder.applyTcclLookupPrecedence(TcclLookupPrecedence.NEVER)
        this.bootstrapServiceRegistry = builder.build()

        val properties = Properties()
        val username = (credentials.username ?: "")
        val password = (credentials.password ?: "")

        properties.setProperty("hibernate.connection.url", credentials.url)
        properties.setProperty("hibernate.connection.username", username)
        properties.setProperty("hibernate.connection.password", password)
        properties.setProperty("hibernate.c3p0.min_size", "2")
        properties.setProperty("hibernate.c3p0.max_size", "10")
        properties.setProperty("hibernate.c3p0.acquire_increment", "2")
        properties.setProperty("hibernate.current_session_context_class", "thread")
        properties.setProperty("hibernate.hbm2ddl.auto", "update")

        configuration = Configuration().setProperties(properties)
        this.serviceRegistry = StandardServiceRegistryBuilder(this.bootstrapServiceRegistry)
            .applySettings(configuration!!.properties)
            .build()

        return this
    }

    fun close() {
        this.sessionFactory?.close()
        BootstrapServiceRegistryBuilder.destroy(this.bootstrapServiceRegistry)
    }

    /**
     * Get or create the Hibernate session factory
     * @return A hibernate session factory
     */
    @WillClose
    fun sessionFactory(): SessionFactory {
        if (this.sessionFactory != null) return this.sessionFactory!!
        checkNotNull(this.configuration)

        this.sessionFactory = this.configuration?.buildSessionFactory(this.serviceRegistry)
        return this.sessionFactory!!
    }

    /**
     * Create a new query for the provided SQL
     * @param clazz the class of the result
     * @param query the query to execute
     * @param params   the parameters to use in the query
     * @param callback the function to run on the
     * @param <T> the type of the result
     * @param <R> the return type of the callback
     * @return The value of the function return
     */
    @Suppress("SqlSourceToSinkFlow")
    suspend fun <T: Any, R> query(
        clazz: Class<T>,
        @Language("SQL") query: String,
        params: Map<String, Any>,
        callback: (NativeQuery<T>) -> R
    ): R? {
        return withContext(Dispatchers.IO) {
            try {
                val namedQuery = sessionFactory().openSession().use { session ->
                    val namedQuery = session.createNativeQuery(query, clazz)
                    for (entry in params.entries) {
                        namedQuery.setParameter(entry.key, entry.value)
                    }
                    namedQuery
                }

                return@withContext callback(namedQuery)
            } catch (e: Throwable) {
                logger.error("Failed to execute query", e)
                return@withContext null
            }
        }
    }

    /**
     * Opens a new session and uses it to execute the given consumer.
     * @param consumer the consumer to execute
     */
    suspend fun useSession(consumer: Consumer<Session>) {
        withContext(Dispatchers.IO) {
            val session: Session = sessionFactory().openSession()

            try {
                session.beginTransaction()
                consumer.accept(session)
                session.transaction.commit()
            } catch (e: Throwable) {
                session.transaction.rollback()
                logger.error("Failed to commit database transaction", e)
            } finally {
                session.close()
            }
        }
    }

    /**
     * Use the given query to retrieve a single result from the database.
     * @param query the query to execute
     * @param params the parameters to use in the query
     * @param <T> the type of the result
     * @return the result of the query
     */
    suspend inline fun <reified T: Any> get(
        @Language("HQL") query: String,
        params: Map<String, Any> = mapOf()
    ): T? {
        return this.query(T::class.java, query, params) {
            it.uniqueResult()
        }
    }

    /**
     * Use the given query to retrieve a single result from the database.
     * @param query the query to execute
     * @param params the parameters to use in the query
     * @param <T> the type of the result
     * @return the result of the query
     */
    suspend inline fun <reified T: Any> list(
        @Language("SQL") query: String,
        params: Map<String, Any> = mapOf()
    ): List<T> {
        return this.query(T::class.java, query, params) {
            it.list()
        } ?: listOf()
    }

    /**
     * Manually registers entities to be added to the configuration
     * @param entities The entities to register
     * @return The current instance for chaining
     */
    @CanIgnoreReturnValue
    fun registerEntities(vararg entities: Class<*>?): HibernateDriver {
        for (entity in entities) {
            this.configuration?.addAnnotatedClass(entity)
        }

        return this
    }

    private companion object {
        val logger: Logger = LoggerFactory.getLogger(HibernateDriver::class.java)
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    }
}
