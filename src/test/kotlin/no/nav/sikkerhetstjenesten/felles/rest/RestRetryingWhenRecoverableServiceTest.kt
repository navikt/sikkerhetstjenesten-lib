package no.nav.sikkerhetstjenesten.felles.rest

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.lettuce.core.RedisCommandTimeoutException
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.dao.QueryTimeoutException
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.resilience.annotation.EnableResilientMethods
import org.springframework.web.client.ResourceAccessException
import java.net.SocketTimeoutException
import java.net.URI

class RestRetryingWhenRecoverableServiceTest : BehaviorSpec({
    val uri = URI.create("http://test/tjeneste")
    lateinit var context: AnnotationConfigApplicationContext
    lateinit var operasjon: TestOperasjon

    beforeEach {
        context = AnnotationConfigApplicationContext(RetryTestConfig::class.java)
        operasjon = context.getBean(TestOperasjon::class.java)
    }

    afterEach {
        context.close()
    }

    val gjenoppretteligeFeil = listOf(
        "RecoverableRestException" to { RecoverableRestException(INTERNAL_SERVER_ERROR, uri) },
        "SocketTimeoutException" to { SocketTimeoutException("Tidsavbrudd") },
        "ResourceAccessException" to { ResourceAccessException("Tjenesten er utilgjengelig") },
        "RedisCommandTimeoutException" to { RedisCommandTimeoutException("Tidsavbrudd i Redis") },
        "QueryTimeoutException" to { QueryTimeoutException("Tidsavbrudd i spørringen") },
    )
    val uoppretteligeFeil = listOf(
        "IrrecoverableRestException" to { IrrecoverableRestException(BAD_REQUEST, uri) },
        "IllegalArgumentException" to { IllegalArgumentException("Ugyldig argument") },
        "IllegalStateException" to { IllegalStateException("Ugyldig tilstand") },
    )

    listOf(
        Triple("annotasjon på metodenivå", "metodeTjeneste", MetodeTjeneste::class.java),
        Triple("annotasjon på klassenivå", "klasseTjeneste", KlasseTjeneste::class.java),
        Triple("arvet annotasjon på klassenivå", "arvetTjeneste", ArvetTjeneste::class.java),
    ).forEach { (beskrivelse, beanName, type) ->
        Given(beskrivelse) {
            Then("utføres et vellykket kall bare én gang") {
                context.getBean(beanName, type).kall() shouldBe "utført"

                operasjon.antallForsøk shouldBe 1
            }

            gjenoppretteligeFeil.forEach { (navn, lagFeil) ->
                Then("forsøkes kallet på nytt etter $navn og stopper ved suksess") {
                    operasjon.feil.add(lagFeil())

                    context.getBean(beanName, type).kall() shouldBe "utført"

                    operasjon.antallForsøk shouldBe 2
                }
            }

            uoppretteligeFeil.forEach { (navn, lagFeil) ->
                Then("videreføres $navn uten nye forsøk") {
                    val feil = lagFeil()
                    operasjon.feil.add(feil)

                    shouldThrow<Exception> {
                        context.getBean(beanName, type).kall()
                    } shouldBeSameInstanceAs feil

                    operasjon.antallForsøk shouldBe 1
                }
            }

            Then("gjøres tre nye forsøk før den siste feilen videreføres") {
                val feil = List(4) { RecoverableRestException(INTERNAL_SERVER_ERROR, uri, "Forsøk ${it + 1}") }
                operasjon.feil.addAll(feil)

                shouldThrow<RecoverableRestException> {
                    context.getBean(beanName, type).kall()
                } shouldBeSameInstanceAs feil.last()

                operasjon.antallForsøk shouldBe 4
            }

            Then("forsøkes kallet på nytt når en gjenopprettelig feil er pakket inn i en annen feil") {
                operasjon.feil.add(IllegalStateException("Innpakket feil", SocketTimeoutException("Tidsavbrudd")))

                context.getBean(beanName, type).kall() shouldBe "utført"

                operasjon.antallForsøk shouldBe 2
            }

            Then("stoppes nye forsøk når en uopprettelig feil følger en gjenopprettelig feil") {
                val feil = IrrecoverableRestException(BAD_REQUEST, uri)
                operasjon.feil.add(RecoverableRestException(INTERNAL_SERVER_ERROR, uri))
                operasjon.feil.add(feil)

                shouldThrow<IrrecoverableRestException> {
                    context.getBean(beanName, type).kall()
                } shouldBeSameInstanceAs feil

                operasjon.antallForsøk shouldBe 2
            }
        }
    }

    listOf(
        "egendefinerte feilklasser på klassenivå" to TilpassetKlasseTjeneste::class.java,
        "egendefinerte feilklasser på metodenivå overstyrer klassenivå" to TilpassetMetodeTjeneste::class.java,
    ).forEach { (beskrivelse, type) ->
        Given(beskrivelse) {
            Then("forsøkes kallet på nytt for den egendefinerte feilklassen") {
                operasjon.feil.add(IllegalStateException("Midlertidig feil"))

                context.getBean(type).kall() shouldBe "utført"

                operasjon.antallForsøk shouldBe 2
            }

            gjenoppretteligeFeil.forEach { (navn, lagFeil) ->
                Then("erstatter den egendefinerte listen standardlisten slik at $navn ikke gir nye forsøk") {
                    val feil = lagFeil()
                    operasjon.feil.add(feil)

                    shouldThrow<Exception> {
                        context.getBean(type).kall()
                    } shouldBeSameInstanceAs feil

                    operasjon.antallForsøk shouldBe 1
                }
            }
        }
    }
}) {
    class TestOperasjon {
        var antallForsøk = 0
        val feil = mutableListOf<Throwable>()

        fun utfør(): String {
            antallForsøk++
            if (feil.isNotEmpty()) throw feil.removeAt(0)
            return "utført"
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableResilientMethods
    class RetryTestConfig {
        @Bean
        fun testOperasjon() = TestOperasjon()

        @Bean
        fun metodeTjeneste(operasjon: TestOperasjon) = MetodeTjeneste(operasjon)

        @Bean
        fun klasseTjeneste(operasjon: TestOperasjon) = KlasseTjeneste(operasjon)

        @Bean
        fun arvetTjeneste(operasjon: TestOperasjon) = ArvetTjeneste(operasjon)

        @Bean
        fun tilpassetKlasseTjeneste(operasjon: TestOperasjon) = TilpassetKlasseTjeneste(operasjon)

        @Bean
        fun tilpassetMetodeTjeneste(operasjon: TestOperasjon) = TilpassetMetodeTjeneste(operasjon)
    }

    open class TestTjeneste(private val operasjon: TestOperasjon) {
        open fun kall() = operasjon.utfør()
    }

    open class MetodeTjeneste(operasjon: TestOperasjon) : TestTjeneste(operasjon) {
        @RestRetryingWhenRecoverableService
        override fun kall() = super.kall()
    }

    @RestRetryingWhenRecoverableService
    open class KlasseTjeneste(operasjon: TestOperasjon) : TestTjeneste(operasjon)

    open class ArvetTjeneste(operasjon: TestOperasjon) : KlasseTjeneste(operasjon)

    @RestRetryingWhenRecoverableService([IllegalStateException::class])
    open class TilpassetKlasseTjeneste(operasjon: TestOperasjon) : TestTjeneste(operasjon)

    open class TilpassetMetodeTjeneste(operasjon: TestOperasjon) : KlasseTjeneste(operasjon) {
        @RestRetryingWhenRecoverableService([IllegalStateException::class])
        override fun kall() = super.kall()
    }
}
