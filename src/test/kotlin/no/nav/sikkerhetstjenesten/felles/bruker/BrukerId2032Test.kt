package no.nav.sikkerhetstjenesten.felles.bruker

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class BrukerId2032Test : BehaviorSpec({
    Given("syntetiske numre som følger kontrollberegningen fra 2032") {
        Then("aksepteres rest 0, 1, 2 og 3 for både fødselsnummer og d-nummer") {
            listOf(
                "02813299952", "02813299960", "02813299979", "02813299987",
                "42813299946", "42813299954", "42813299962", "42813299970",
                "01810099905"
            ).forEach { verdi ->
                BrukerId2032(verdi).verdi shouldBe verdi
                BrukerId2032.parse(verdi) shouldBe BrukerId2032(verdi)
            }
        }

        Then("aksepteres bare de fire gyldige kontrollsifferparene for samme grunnlag") {
            val gyldige = setOf("02813299952", "02813299960", "02813299979", "02813299987")
            for (k1 in 0..9) {
                for (k2 in 0..9) {
                    val verdi = "028132999$k1$k2"
                    if (verdi in gyldige) {
                        BrukerId2032.parse(verdi)?.verdi shouldBe verdi
                    } else {
                        BrukerId2032.parse(verdi) shouldBe null
                        shouldThrow<IllegalArgumentException> { BrukerId2032(verdi) }
                    }
                }
            }
        }
    }

    Given("Skatteetatens eksempel på et fødselsnummer fra 2032") {
        Then("aksepteres 02013299997 med rest 0") {
            BrukerId2032("02013299997").verdi shouldBe "02013299997"
            BrukerId2032.parse("02013299997") shouldBe BrukerId2032("02013299997")
        }
    }

    Given("numre som ikke følger kontrollberegningen fra 2032") {
        Then("avvises rest 4 utenfor ny beregning") {
            listOf("02813299995", "42813299989").forEach { verdi ->
                shouldThrow<IllegalArgumentException> {
                    BrukerId2032(verdi)
                }
                BrukerId2032.parse(verdi) shouldBe null
            }
        }

        Then("avvises ugyldig lengde, mellomrom og tegn som ikke er ASCII-siffer") {
            listOf("", "0281329996", "028132999600", " 02813299960", "0281329996a", "０2813299960")
                .forEach { verdi ->
                    shouldThrow<IllegalArgumentException> {
                        BrukerId2032(verdi)
                    }
                    BrukerId2032.parse(verdi) shouldBe null
                }
        }
    }
})
