package no.nav.sikkerhetstjenesten.felles.utils.extensions

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import no.nav.sikkerhetstjenesten.felles.utils.extensions.DomainExtensions.maskFnr
import no.nav.sikkerhetstjenesten.felles.utils.extensions.DomainExtensions.requireDigits
import no.nav.sikkerhetstjenesten.felles.utils.extensions.DomainExtensions.upcase
import org.slf4j.MDC

class DomainExtensionsTest : BehaviorSpec({

    Given("requireDigits") {

        When("strengen har kun siffer og riktig lengde") {
            Then("kaster ikke") {
                shouldNotThrowAny { requireDigits("08526835670", 11) }
            }
        }

        When("strengen inneholder en bokstav") {
            Then("kaster IllegalArgumentException") {
                shouldThrow<IllegalArgumentException> { requireDigits("0852683567a", 11) }
            }
        }

        When("strengen inneholder spesialtegn") {
            Then("kaster IllegalArgumentException") {
                shouldThrow<IllegalArgumentException> { requireDigits("0852683567-", 11) }
            }
        }

        When("strengen er for kort") {
            Then("kaster IllegalArgumentException") {
                shouldThrow<IllegalArgumentException> { requireDigits("0852683567", 11) }
            }
        }

        When("strengen er for lang") {
            Then("kaster IllegalArgumentException") {
                shouldThrow<IllegalArgumentException> { requireDigits("085268356701", 11) }
            }
        }

        When("strengen er tom") {
            Then("kaster IllegalArgumentException") {
                shouldThrow<IllegalArgumentException> { requireDigits("", 11) }
            }
        }

        When("strengen er gyldig 13-sifret aktørId") {
            Then("kaster ikke") {
                shouldNotThrowAny { requireDigits("1234567890123", 13) }
            }
        }
    }

    Given("maskFnr") {

        When("strengen er et 11-sifret fnr") {
            Then("maskeres fra posisjon 4") {
                "08526835670".maskFnr() shouldBe "0852*******"
            }
        }

        When("strengen er et 13-sifret aktørId") {
            Then("maskeres fra posisjon 6") {
                "1234567890123".maskFnr() shouldBe "123456*******"
            }
        }

        When("strengen er kortere enn forventet") {
            Then("returneres uendret") {
                "Z999999".maskFnr() shouldBe "Z999999"
            }
        }

        When("strengen er tom") {
            Then("returneres uendret") {
                "".maskFnr() shouldBe ""
            }
        }

        When("strengen har annen lengde") {
            Then("returneres uendret") {
                "12345".maskFnr() shouldBe "12345"
            }
        }

        When("fnr maskeres") {
            Then("har alltid 11 tegn totalt") {
                "08526835670".maskFnr().length shouldBe 11
            }
        }

        When("aktørId maskeres") {
            Then("har alltid 13 tegn totalt") {
                "1234567890123".maskFnr().length shouldBe 13
            }
        }
    }

    Given("upcase") {

        When("strengen starter med liten bokstav") {
            Then("gjør første bokstav stor") {
                "hello".upcase() shouldBe "Hello"
            }
        }

        When("resten av strengen er store bokstaver") {
            Then("endres ikke") {
                "hELLO".upcase() shouldBe "HELLO"
            }
        }

        When("første bokstav allerede er stor") {
            Then("returneres uendret") {
                "Hello".upcase() shouldBe "Hello"
            }
        }

        When("strengen er tom") {
            Then("returneres uendret") {
                "".upcase() shouldBe ""
            }
        }
    }

    Given("withMDC") {
        When("verdier sendes inn som map") {
            Then("settes i MDC under block og fjernes etterpå") {
                val traceId = "trace-id-test"
                val userId = "user-id-test"
                DomainExtensions.withMDC(verdier = mapOf("traceId" to traceId, "userId" to userId)) {
                    MDC.get("traceId") shouldBe traceId
                    MDC.get("userId") shouldBe userId
                }
                MDC.get("traceId") shouldBe null
                MDC.get("userId") shouldBe null
            }
        }
    }
})
