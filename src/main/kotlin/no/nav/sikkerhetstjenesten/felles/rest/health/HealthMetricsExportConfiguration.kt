package no.nav.sikkerhetstjenesten.felles.rest.health

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint
import org.springframework.boot.health.contributor.Status.DOWN
import org.springframework.boot.health.contributor.Status.OUT_OF_SERVICE
import org.springframework.boot.health.contributor.Status.UP
import org.springframework.context.annotation.Configuration


@Configuration(proxyBeanMethods = false)
class HealthMetricsExportConfiguration(registry: MeterRegistry, healthEndpoint: HealthEndpoint) {

    init {
        Gauge.builder("health", healthEndpoint) { health ->
            getStatusCode(health).toDouble()
        }.strongReference(true).register(registry)
    }

    private fun getStatusCode(health: HealthEndpoint) =
        when (health.health().status) {
            UP -> 3
            OUT_OF_SERVICE -> 2
            DOWN -> 1
            else -> 0
        }

}