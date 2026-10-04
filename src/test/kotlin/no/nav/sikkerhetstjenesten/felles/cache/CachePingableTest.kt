package no.nav.sikkerhetstjenesten.felles.cache

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.sikkerhetstjenesten.felles.cache.CachePingable
import org.springframework.boot.data.redis.autoconfigure.DataRedisProperties
import org.springframework.data.redis.connection.RedisConnection
import org.springframework.data.redis.core.RedisCallback
import org.springframework.data.redis.core.StringRedisTemplate

class CachePingableTest : BehaviorSpec({

    val connection = mockk<RedisConnection>(relaxed = true)
    val valkey = mockk<StringRedisTemplate>()
    every { valkey.execute<Unit>(any<RedisCallback<Unit>>()) } answers {
        firstArg<RedisCallback<Unit>>().doInRedis(connection)
    }
    val pingable = CachePingable(valkey, DataRedisProperties().apply {
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
