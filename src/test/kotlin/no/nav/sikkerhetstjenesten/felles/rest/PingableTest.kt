package no.nav.sikkerhetstjenesten.felles.rest

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import java.net.URI

class PingableTest : BehaviorSpec({
    val cfg = object : RestConfig(URI.create("https://example.test"), "/health", "test") {}

    Given("en ping-adapter") {
        Then("beholdes navn og endpoint fra konfigurasjonen") {
            val adapter = pingable(cfg) { "OK" }

            adapter.name shouldBe cfg.name
            adapter.pingEndpoint shouldBe cfg.pingEndpoint
        }

        Then("kjøres ping først ved kall og på nytt for hvert kall") {
            var calls = 0
            val adapter = pingable(cfg) { ++calls }

            calls shouldBe 0
            adapter.ping() shouldBe 1
            adapter.ping() shouldBe 2
        }

        Then("returneres resultatet fra klienten uendret") {
            val result = Any()

            pingable(cfg) { result }.ping() shouldBeSameInstanceAs result
        }

        Then("kan klienten returnere null") {
            pingable(cfg) { null }.ping() shouldBe null
        }

        Then("propageres feil fra klienten uendret") {
            val failure = IllegalStateException("Ping feilet")
            val adapter = pingable(cfg) { throw failure }

            shouldThrow<IllegalStateException> { adapter.ping() } shouldBeSameInstanceAs failure
        }
    }
})
