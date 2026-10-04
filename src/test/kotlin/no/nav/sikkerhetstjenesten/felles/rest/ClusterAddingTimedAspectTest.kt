package no.nav.sikkerhetstjenesten.felles.rest

import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.extensions.ApplyExtension
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.micrometer.core.annotation.Timed
import io.micrometer.core.instrument.MeterRegistry
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import no.nav.sikkerhetstjenesten.felles.rest.ClusterAddingTimedAspectTest.TestConfig
import no.nav.sikkerhetstjenesten.felles.rest.health.ObservabilityAutoConfiguration
import no.nav.sikkerhetstjenesten.felles.security.AuthContext
import no.nav.sikkerhetstjenesten.felles.utils.cluster.ClusterUtils
import no.nav.sikkerhetstjenesten.felles.utils.cluster.ClusterUtils.DEV_GCP_CLUSTER
import no.nav.sikkerhetstjenesten.felles.utils.cluster.ClusterUtils.PROD_GCP_CLUSTER
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.micrometer.metrics.test.autoconfigure.AutoConfigureMetrics
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.test.context.ContextConfiguration

@ApplyExtension(SpringExtension::class)
@ContextConfiguration(classes = [TestConfig::class, ObservabilityAutoConfiguration::class])
@AutoConfigureMetrics
class ClusterAddingTimedAspectTest(
) : BehaviorSpec() {

    @Autowired
    lateinit var registry: MeterRegistry

    @Autowired
    lateinit var timedService: TimedService

    @MockkBean
    lateinit var authContext: AuthContext

    init {
        beforeEach {
            mockkObject(ClusterUtils)
            every { ClusterUtils.current } returns DEV_GCP_CLUSTER
            every { authContext.systemNavn } returns "my-app"
        }

        afterEach {
            unmockkObject(ClusterUtils)
        }
        Given("clusterAddingTimedAspect") {
            When("tjeneste kalles") {
                Then("registreres timer med cluster-, method- og client-tagg fra token") {
                    timedService.execute()
                    registry.get("test.execute")
                        .tag("cluster", "dev-gcp")
                        .tag("method", "execute")
                        .tag("client", "my-app")
                        .timer().count() shouldBeGreaterThan 0L
                }
            }
            When("token-verdier endres mellom kall") {
                Then("brukes oppdaterte verdier per kall") {
                    every { authContext.systemNavn } returns "annen-app"
                    every { ClusterUtils.current } returns PROD_GCP_CLUSTER
                    timedService.execute()
                    registry.get("test.execute")
                        .tag("cluster", "prod-gcp")
                        .tag("client", "annen-app")
                        .timer().count() shouldBeGreaterThan 0L
                }
            }
        }
    }

    @Configuration
    class TestConfig {
        @Bean
        fun timedService() = TimedService()
    }

    open class TimedService {
        @Timed("test.execute")
        open fun execute() {}
    }
}
