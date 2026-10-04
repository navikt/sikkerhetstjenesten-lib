package no.nav.sikkerhetstjenesten.felles.cache

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class CacheNøkkelTest : BehaviorSpec({
    val id = "01011111111"
    val cacheWithoutExtraPrefix = CacheNøkkelConfig("entraoid")
    val cacheWithExtraPrefix = CacheNøkkelConfig("pdl", "medFamilie")

    Given("en cache uten ekstra-prefiks") {
        When("tilNøkkel kalles") {
            Then("legger til prefiks og id") {
                cacheWithoutExtraPrefix.tilNøkkel(id) shouldBe "${cacheWithoutExtraPrefix.name}::$id"
            }
        }
    }

    Given("en cache med ekstra-prefiks") {
        When("tilNøkkel kalles") {
            Then("legger til prefiks, ekstra-prefiks og id") {
                cacheWithExtraPrefix.tilNøkkel(id) shouldBe "${cacheWithExtraPrefix.name}::${cacheWithExtraPrefix.extraPrefix}:$id"
            }
        }
    }
})
