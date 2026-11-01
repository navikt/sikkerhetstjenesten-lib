package no.nav.sikkerhetstjenesten.felles.cache

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.valkey.springframework.data.valkey.core.StringValkeyTemplate
import tools.jackson.core.JacksonException

class ValkeyCacheOperationsTest : BehaviorSpec({
    val valkey = mockk<StringValkeyTemplate>(relaxed = true)
    val operations = ValkeyCacheOperations(valkey)
    val caches = arrayOf(CacheNøkkelConfig("first"), CacheNøkkelConfig("second"))

    Given("cache-størrelser fra Lua-skript") {
        When("skriptet returnerer størrelser som JSON") {
            Then("kobles alle størrelser til riktig cache") {
                every { valkey.execute<String>(any(), any(), *anyVararg()) } returns "[2,3]"

                operations.sizes(*caches) shouldBe mapOf("first" to 2L, "second" to 3L)
            }
        }

        When("ingen cacher telles") {
            Then("returneres et tomt resultat") {
                every { valkey.execute<String>(any(), any(), *anyVararg()) } returns "[]"

                operations.sizes() shouldBe emptyMap()
            }
        }

        When("skriptet returnerer feil antall størrelser") {
            Then("avvises resultatet") {
                every { valkey.execute<String>(any(), any(), *anyVararg()) } returns "[2]"

                shouldThrow<IllegalStateException> { operations.sizes(*caches) }
            }
        }

        When("skriptet returnerer en verdi som ikke er et tall") {
            Then("avvises resultatet") {
                every { valkey.execute<String>(any(), any(), *anyVararg()) } returns "[2,\"invalid\"]"

                shouldThrow<JacksonException> { operations.sizes(*caches) }
            }
        }
    }
})
