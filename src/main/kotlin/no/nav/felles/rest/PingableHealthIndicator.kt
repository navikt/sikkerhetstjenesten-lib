package no.nav.felles.rest

import org.slf4j.LoggerFactory.getLogger
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator

class PingableHealthIndicator(private val pingable: Pingable) : HealthIndicator {
    private val log = getLogger(javaClass)

    override fun health() =
        runCatching {
            pingable.ping()
            Health.up()
                .withDetail(ENDPOINT, pingable.pingEndpoint)
                .build()
        }.getOrElse {
            log.warn("Kunne ikke pinge ${pingable.pingEndpoint}", it)
            Health.down(it)
                .withDetail(ENDPOINT, pingable.pingEndpoint)
                .build()
        }

    companion object {
        const val ENDPOINT = "endpoint"
    }
}
