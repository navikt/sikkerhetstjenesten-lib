package no.nav.sikkerhetstjenesten.felles.cache

import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.sikkerhetstjenesten.felles.cache.ResilientValkeySerializer
import org.springframework.data.redis.serializer.RedisSerializer
import org.springframework.data.redis.serializer.SerializationException

class ResilientValkeySerializerTest : BehaviorSpec({

    val bytes = "data".toByteArray()
    val obj = "verdi"

    fun serializer(delegate: RedisSerializer<Any>, registry: SimpleMeterRegistry = SimpleMeterRegistry()) =
        ResilientValkeySerializer(delegate)

    Given("deserialisering") {

        When("deserialisering lykkes") {
            Then("returnerer verdien fra delegate") {
                val delegate = mockk<RedisSerializer<Any>> {
                    every { deserialize(bytes) } returns obj
                }
                serializer(delegate).deserialize(bytes) shouldBe obj
            }
        }

        When("delegate kaster SerializationException") {
            Then("returnerer null i stedet for å propagere unntaket") {
                val delegate = mockk<RedisSerializer<Any>> {
                    every { deserialize(bytes) } throws SerializationException("Ugyldig JSON")
                }
                serializer(delegate).deserialize(bytes).shouldBeNull()
            }
        }

        When("bytes er null") {
            Then("delegerer null til underliggende serializer") {
                val delegate = mockk<RedisSerializer<Any>> {
                    every { deserialize(null) } returns null
                }
                serializer(delegate).deserialize(null).shouldBeNull()
                verify { delegate.deserialize(null) }
            }
        }
    }

    Given("serialisering") {

        When("serialisering lykkes") {
            Then("delegerer direkte til underliggende serializer") {
                val delegate = mockk<RedisSerializer<Any>> {
                    every { serialize(obj) } returns bytes
                }
                assertSoftly {
                    serializer(delegate).serialize(obj) shouldBe bytes
                    verify { delegate.serialize(obj) }
                }
            }
        }

        When("delegate kaster SerializationException under serialisering") {
            Then("propagerer unntaket uten å fange det") {
                val delegate = mockk<RedisSerializer<Any>> {
                    every { serialize(any()) } throws SerializationException("Kan ikke serialisere")
                }
                val result = runCatching { serializer(delegate).serialize(obj) }
                result.isFailure shouldBe true
                result.exceptionOrNull().shouldBeInstanceOf<SerializationException>()
            }
        }
    }
})
