package no.nav.sikkerhetstjenesten.felles.cache

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.sikkerhetstjenesten.felles.cache.CachePingable
import io.valkey.springframework.boot.autoconfigure.data.valkey.ValkeyProperties
import io.valkey.springframework.data.valkey.connection.ValkeyConnection
import io.valkey.springframework.data.valkey.core.ValkeyCallback
import io.valkey.springframework.data.valkey.core.StringValkeyTemplate

class CachePingableTest : BehaviorSpec({

    val connection = mockk<ValkeyConnection>(relaxed = true)
    val valkey = mockk<StringValkeyTemplate>()
    every { valkey.execute<Unit>(any<ValkeyCallback<Unit>>()) } answers {
        firstArg<ValkeyCallback<Unit>>().doInValkey(connection)
    }
    val pingable = CachePingable(valkey, ValkeyProperties().apply {
        host = "localhost"
        port = 6379
    })

    Given("ping mot cache-tilkobling") {
        When("Redis returnerer PONG") {
            Then("kaster ingen feil") {
                every {
                    connection.ping()
                } returns "PONG"
                shouldNotThrowAny {
                    pingable.ping()
                }
            }
        }
        When("Redis returnerer pong lowercase") {
            Then("kaster ingen feil") {
                every { connection.ping() } returns "pong"
                shouldNotThrowAny {
                    pingable.ping()
                }
            }
        }
        When("Redis returnerer noe annet enn pong") {
            Then("kaster IllegalStateException") {
                every {
                    connection.ping()
                } returns "ERROR"
                shouldThrow<IllegalStateException> {
                    pingable.ping()
                }
            }
        }
        When("Redis returnerer null") {
            Then("kaster IllegalStateException") {
                every {
                    connection.ping()
                } returns null
                shouldThrow<IllegalStateException> {
                    pingable.ping()
                }
            }
        }
    }
})
