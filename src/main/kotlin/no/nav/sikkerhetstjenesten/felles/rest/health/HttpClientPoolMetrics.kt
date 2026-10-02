package no.nav.sikkerhetstjenesten.felles.rest.health

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import org.apache.hc.client5.http.classic.HttpClient
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager
import org.springframework.http.client.ClientHttpRequestFactory
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.text.Normalizer
import java.text.Normalizer.Form
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

class HttpClientPoolMetrics(private val registry: MeterRegistry) {

    private val connectionManagers = ConcurrentHashMap.newKeySet<PoolingHttpClientConnectionManager>()
    private val registeredClients = ConcurrentHashMap.newKeySet<String>()
    private val pendingClients = ConcurrentHashMap.newKeySet<String>()
    private val defaultManager = AtomicReference<PoolingHttpClientConnectionManager?>()

    init {
        Gauge.builder("tilgangsmaskin.http.client.pool", this) { metrics ->
            metrics.registeredClients.size.toDouble()
        }
            .tag("state", "managers")
            .strongReference(true)
            .register(registry)
        Gauge.builder("tilgangsmaskin.http.client.pool", this) { metrics ->
            metrics.connectionManagers.size.toDouble()
        }
            .tag("state", "pools")
            .strongReference(true)
            .register(registry)
    }

    fun bind(beanName: String, restClient: RestClient) {
        val requestFactory = requestFactory(restClient)
        if (requestFactory !is HttpComponentsClientHttpRequestFactory) return
        bind(beanName, requestFactory)
    }

    fun bind(beanName: String, factory: HttpComponentsClientHttpRequestFactory) {
        val connectionManager = connectionManager(factory.httpClient) ?: return
        connectionManagers.add(connectionManager)
        defaultManager.compareAndSet(null, connectionManager)
        registerClient(beanName, connectionManager)
        bindPendingClients(connectionManager)
    }

    fun bindClient(beanName: String) {
        val manager = defaultManager.get()
        if (manager == null) {
            pendingClients.add(sanitizeManagerName(beanName))
            return
        }
        registerClient(beanName, manager)
    }

    private fun bindPendingClients(manager: PoolingHttpClientConnectionManager) {
        val pending = pendingClients.toList()
        pending.forEach { clientName ->
            if (pendingClients.remove(clientName)) {
                registerClient(clientName, manager)
            }
        }
    }

    private fun registerClient(beanName: String, connectionManager: PoolingHttpClientConnectionManager) {
        val managerName = sanitizeManagerName(beanName)
        if (!registeredClients.add(managerName)) return

        Gauge.builder("tilgangsmaskin.http.client.pool", connectionManager) { cm ->
            cm.totalStats.leased.toDouble()
        }
            .tag("state", "leased")
            .tag("manager", managerName)
            .strongReference(true)
            .register(registry)

        Gauge.builder("tilgangsmaskin.http.client.pool", connectionManager) { cm ->
            cm.totalStats.pending.toDouble()
        }
            .tag("state", "pending")
            .tag("manager", managerName)
            .strongReference(true)
            .register(registry)

        Gauge.builder("tilgangsmaskin.http.client.pool", connectionManager) { cm ->
            cm.totalStats.available.toDouble()
        }
            .tag("state", "available")
            .tag("manager", managerName)
            .strongReference(true)
            .register(registry)

        Gauge.builder("tilgangsmaskin.http.client.pool", connectionManager) { cm ->
            cm.totalStats.max.toDouble()
        }
            .tag("state", "max")
            .tag("manager", managerName)
            .strongReference(true)
            .register(registry)
    }

    private fun requestFactory(restClient: RestClient): ClientHttpRequestFactory? {
        return try {
            val field = restClient.javaClass.getDeclaredField("clientRequestFactory").apply {
                isAccessible = true
            }
            field.get(restClient) as? ClientHttpRequestFactory
        } catch (_: ReflectiveOperationException) {
            null
        }
    }

    private fun connectionManager(httpClient: HttpClient): PoolingHttpClientConnectionManager? {
        val field = httpClient.javaClass.getDeclaredField("connManager").apply {
            isAccessible = true
        }
        return field.get(httpClient) as? PoolingHttpClientConnectionManager
    }

    private fun sanitizeManagerName(beanName: String): String {
        val simpleName = beanName.substringAfterLast('.').removeSuffix("Client")
        val normalized = Normalizer.normalize(simpleName, Form.NFKD)
            .replace(Regex("\\p{Mn}+"), "")
        return normalized.replace(Regex("[^a-zA-Z0-9._-]"), "_")
    }
}
