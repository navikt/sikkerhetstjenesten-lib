package no.nav.sikkerhetstjenesten.felles.rest

import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldExist
import io.kotest.matchers.shouldBe
import no.nav.sikkerhetstjenesten.felles.rest.PingableHealthIndicator.Companion.ENDPOINT
import org.springframework.boot.health.contributor.Status.DOWN
import org.springframework.boot.health.contributor.Status.UP
import java.net.URI

class PingableHealthIndicatorTest : BehaviorSpec({

    class StubPingable(
        override val pingEndpoint: URI = URI.create("http://example.com/ping"),
        override val name: String = "test",
        val onPing: () -> Any? = { }) : Pingable {
        var pinged = false
        override fun ping(): Any? { pinged = true; return onPing() }
    }

    Given("helseindikator for pingable") {
        When("ping lykkes") {
            Then("returneres UP med riktig endpoint og pinged=true") {
                val pingable = StubPingable()
                val health = PingableHealthIndicator(pingable).health()
                health.status shouldBe UP
                health.details[ENDPOINT].toString() shouldBe "http://example.com/ping"
                pingable.pinged shouldBe true
            }
        }
        When("ping kaster exception") {
            Then("returneres DOWN med feilmelding og riktig endpoint") {
                val pingable = StubPingable(onPing = { throw RuntimeException("Connection refused") })
                val health = PingableHealthIndicator(pingable).health()
                assertSoftly(health) {
                    status shouldBe DOWN
                    details[ENDPOINT].toString() shouldBe "http://example.com/ping"
                    details.values.map { it.toString() }.shouldExist { it.contains("Connection refused") }
                }

            }
        }
        When("annen pingEndpoint er konfigurert og ping feiler") {
            Then("inkluderes riktig pingEndpoint i DOWN-detaljer") {
                val pingable = StubPingable(
                    pingEndpoint = URI.create("http://other.com/health"),
                    onPing = { throw RuntimeException("timeout") }
                )
                PingableHealthIndicator(pingable).health().details[ENDPOINT].toString() shouldBe "http://other.com/health"
            }
        }
    }
})
