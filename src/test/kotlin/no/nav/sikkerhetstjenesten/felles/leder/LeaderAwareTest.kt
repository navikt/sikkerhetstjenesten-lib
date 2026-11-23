package no.nav.sikkerhetstjenesten.felles.leder

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import java.net.InetAddress.getLocalHost
import java.util.function.Supplier

class LeaderAwareTest : BehaviorSpec({
    Given("en lederbevisst komponent") {
        Then("kjøres ikke blokken før instansen er valgt som leder") {
            val komponent = TestLeaderAware()
            var antallKjøringer = 0

            komponent.kjør { antallKjøringer++ } shouldBe null

            antallKjøringer shouldBe 0
        }

        Then("kjøres blokken når instansen opprettes som leder og resultatet returneres") {
            val komponent = TestLeaderAware(true)
            var antallKjøringer = 0

            komponent.kjør("Utfører lederoppgave") {
                antallKjøringer++
                "utført"
            } shouldBe "utført"

            antallKjøringer shouldBe 1
        }

        Then("kjøres ikke blokken når instansen opprettes som følger") {
            val komponent = TestLeaderAware(false)
            var antallKjøringer = 0

            komponent.kjør { antallKjøringer++ } shouldBe null

            antallKjøringer shouldBe 0
        }

        Then("oppdateres lederstatus ved lederhendelser publisert av Spring") {
            val komponent = TestLeaderAware()
            val hostname = getLocalHost().hostName
            var antallKjøringer = 0

            AnnotationConfigApplicationContext().use { context ->
                context.registerBean(TestLeaderAware::class.java, Supplier { komponent })
                context.refresh()

                context.publishEvent(LederHendelse(this, hostname))
                komponent.kjør { ++antallKjøringer } shouldBe 1

                context.publishEvent(LederHendelse(this, "$hostname-annen"))
                komponent.kjør { ++antallKjøringer } shouldBe null
                antallKjøringer shouldBe 1

                context.publishEvent(LederHendelse(this, hostname))
                komponent.kjør { ++antallKjøringer } shouldBe 2
            }
        }

        Then("forblir instansen følger når en annen instans velges som leder") {
            val komponent = TestLeaderAware()
            var antallKjøringer = 0
            komponent.onApplicationEvent(LederHendelse(this, "${getLocalHost().hostName}-annen"))

            komponent.kjør { antallKjøringer++ } shouldBe null

            antallKjøringer shouldBe 0
        }

        Then("videreføres feil fra blokken når instansen er leder") {
            val komponent = TestLeaderAware(true)
            val feil = IllegalStateException("Lederoppgaven feilet")

            shouldThrow<IllegalStateException> {
                komponent.kjør { throw feil }
            } shouldBeSameInstanceAs feil
        }
    }
}) {
    class TestLeaderAware(erLeder: Boolean = false) : LeaderAware(erLeder) {
        fun <T> kjør(beskrivelse: String? = null, block: () -> T): T? = somLeder(beskrivelse, block)
    }
}
