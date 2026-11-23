package no.nav.sikkerhetstjenesten.felles.cache

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.sikkerhetstjenesten.felles.leder.LederHendelse
import no.nav.sikkerhetstjenesten.felles.rest.ConsumerAwareHandlerInterceptor.Companion.USER_ID
import org.slf4j.MDC
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import java.net.InetAddress.getLocalHost
import java.util.function.Supplier

class ValkeyEventListeningCacheOppfriskerTest : BehaviorSpec({
    lateinit var pdl: CacheOppfrisker
    lateinit var entra: CacheOppfrisker

    beforeEach {
        MDC.clear()
        pdl = mockk(relaxed = true)
        entra = mockk(relaxed = true)
        every { pdl.cacheName } returns "pdl"
        every { entra.cacheName } returns "entra"
    }

    afterEach {
        MDC.clear()
    }

    Given("en hendelsesstyrt cache-oppfrisker") {
        Then("oppfriskes bare cachen som samsvarer med nøkkelen") {
            val lytter = ValkeyEventListeningCacheOppfrisker(true, entra, pdl)
            val nøkkel = CacheNøkkel("pdl::medFamilie:01011111111")

            lytter.onEvent(nøkkel)

            verify(exactly = 1) { pdl.oppfrisk(nøkkel) }
            verify(exactly = 0) { entra.oppfrisk(any()) }
        }

        Then("ignoreres nøkler uten en tilhørende oppfrisker") {
            val lytter = ValkeyEventListeningCacheOppfrisker(true, pdl, entra)

            shouldNotThrowAny { lytter.onEvent(CacheNøkkel("ukjent::id")) }

            verify(exactly = 0) { pdl.oppfrisk(any()) }
            verify(exactly = 0) { entra.oppfrisk(any()) }
        }

        Then("håndteres hendelser uten registrerte oppfriskere") {
            val lytter = ValkeyEventListeningCacheOppfrisker()

            shouldNotThrowAny { lytter.onEvent(CacheNøkkel("pdl::id")) }
        }

        Then("brukes bare den første oppfriskeren når flere har samme cache-navn") {
            val annenPdl = mockk<CacheOppfrisker>(relaxed = true)
            every { annenPdl.cacheName } returns "pdl"
            val lytter = ValkeyEventListeningCacheOppfrisker(true, pdl, annenPdl)
            val nøkkel = CacheNøkkel("pdl::id")

            lytter.onEvent(nøkkel)

            verify(exactly = 1) { pdl.oppfrisk(nøkkel) }
            verify(exactly = 0) { annenPdl.oppfrisk(any()) }
        }

        Then("ignoreres hendelser på en følgerinstans uten å endre MDC") {
            val lytter = ValkeyEventListeningCacheOppfrisker(false, pdl, entra)
            MDC.put(USER_ID, "opprinnelig")

            lytter.onEvent(CacheNøkkel("pdl::01011111111"))

            verify(exactly = 0) { pdl.oppfrisk(any()) }
            verify(exactly = 0) { entra.oppfrisk(any()) }
            MDC.get(USER_ID) shouldBe "opprinnelig"
        }

        Then("styres oppfrisking av lederhendelser publisert av Spring") {
            val lytter = ValkeyEventListeningCacheOppfrisker(false, pdl)
            val nøkkel = CacheNøkkel("pdl::id")
            val hostname = getLocalHost().hostName

            AnnotationConfigApplicationContext().use { context ->
                context.registerBean(ValkeyEventListeningCacheOppfrisker::class.java, Supplier { lytter })
                context.refresh()

                lytter.onEvent(nøkkel)
                verify(exactly = 0) { pdl.oppfrisk(any()) }

                context.publishEvent(LederHendelse(this, hostname))
                lytter.onEvent(nøkkel)
                verify(exactly = 1) { pdl.oppfrisk(nøkkel) }

                context.publishEvent(LederHendelse(this, "$hostname-annen"))
                lytter.onEvent(nøkkel)
                verify(exactly = 1) { pdl.oppfrisk(nøkkel) }
            }
        }

        Then("er bruker-ID maskert i MDC når oppfriskeren kalles") {
            val lytter = ValkeyEventListeningCacheOppfrisker(true, pdl)
            val nøkkel = CacheNøkkel("pdl::01011111111")
            every { pdl.oppfrisk(nøkkel) } answers {
                MDC.get(USER_ID) shouldBe "0101*******"
            }

            lytter.onEvent(nøkkel)

            verify(exactly = 1) { pdl.oppfrisk(nøkkel) }
        }

        Then("videreføres feil fra den valgte oppfriskeren") {
            val lytter = ValkeyEventListeningCacheOppfrisker(true, pdl)
            val nøkkel = CacheNøkkel("pdl::id")
            val feil = IllegalStateException("Oppfrisking feilet")
            every { pdl.oppfrisk(nøkkel) } throws feil

            shouldThrow<IllegalStateException> {
                lytter.onEvent(nøkkel)
            } shouldBeSameInstanceAs feil
        }
    }
})
