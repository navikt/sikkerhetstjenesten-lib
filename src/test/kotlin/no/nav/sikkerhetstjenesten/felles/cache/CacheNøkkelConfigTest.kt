package no.nav.sikkerhetstjenesten.felles.cache

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class CacheNøkkelConfigTest : BehaviorSpec({

    Given("format med metode: cacheName::metode:id") {

        val nøkkel = CacheNøkkel("graph::geoGrupper:Z999999")

        When("cacheName parses") {
            Then("cacheName er graph") {
                nøkkel.cacheName shouldBe "graph"
            }
        }
        When("metode parses") {
            Then("metode er geoGrupper") {
                nøkkel.metode shouldBe "geoGrupper"
            }
        }
        When("id parses") {
            Then("id er Z999999") {
                nøkkel.id shouldBe "Z999999"
            }
        }
        When("original key is read") {
            Then("the original key is preserved") {
                nøkkel.nøkkel shouldBe "graph::geoGrupper:Z999999"
            }
        }
    }

    Given("format uten metode: cacheName::id") {

        val nøkkel = CacheNøkkel("oppfolging::08526835670")

        When("cacheName parses") {
            Then("cacheName er oppfolging") {
                nøkkel.cacheName shouldBe "oppfolging"
            }
        }
        When("metode parses") {
            Then("metode er null") {
                nøkkel.metode shouldBe null
            }
        }
        When("id parses") {
            Then("id er fnr") {
                nøkkel.id shouldBe "08526835670"
            }
        }
        When("original key is read") {
            Then("the original key is preserved") {
                nøkkel.nøkkel shouldBe "oppfolging::08526835670"
            }
        }
    }
})
