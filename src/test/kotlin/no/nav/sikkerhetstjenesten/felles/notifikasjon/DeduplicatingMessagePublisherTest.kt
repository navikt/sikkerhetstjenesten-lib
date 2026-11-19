package no.nav.sikkerhetstjenesten.felles.notifikasjon

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.time.Duration
import java.time.Duration.ofDays
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.atomic.AtomicLong

class DeduplicatingMessagePublisherTest : BehaviorSpec({
    lateinit var delegate: MessagePublisher
    lateinit var time: AtomicLong
    lateinit var deduplicator: MessageDeduplicator
    lateinit var publisher: DeduplicatingMessagePublisher
    val ttl = ofDays(30)

    fun publisher(application: String = "test", cluster: String = "dev-gcp") =
        DeduplicatingMessagePublisher(delegate, deduplicator, application, cluster)

    beforeEach {
        delegate = mockk(relaxed = true)
        time = AtomicLong()
        deduplicator = MessageDeduplicator(ticker = time::get)
        publisher = publisher()
    }

    Given("a deduplicating message publisher") {
        Then("once methods deduplicate independently by level and key") {
            repeat(2) {
                publisher.infoOnce("image:v1", ttl, "header", "message")
                publisher.warnOnce("image:v1", ttl, "header", "message")
                publisher.errorOnce("image:v1", ttl, "header", "message")
            }
            publisher.infoOnce("image:v2", ttl, "header", "message")

            verify(exactly = 2) { delegate.info("header", "message") }
            verify(exactly = 1) { delegate.warn("header", "message") }
            verify(exactly = 1) { delegate.error("header", "message") }
        }

        Then("markers expire at the requested TTL and duplicate calls do not extend it") {
            publisher.infoOnce("image:v1", ttl, "header", "message")
            time.set(ttl.toNanos() - 1)
            publisher.infoOnce("image:v1", ttl, "header", "message")
            verify(exactly = 1) { delegate.info(any(), any()) }

            time.incrementAndGet()
            publisher.infoOnce("image:v1", ttl, "header", "message")
            verify(exactly = 2) { delegate.info(any(), any()) }
        }

        Then("different keys can have different TTLs") {
            publisher.infoOnce("short", ofDays(1), "header", "short")
            publisher.infoOnce("long", ttl, "header", "long")
            time.set(ofDays(1).toNanos())
            publisher.infoOnce("short", ofDays(1), "header", "short")
            publisher.infoOnce("long", ttl, "header", "long")

            verify(exactly = 2) { delegate.info("header", "short") }
            verify(exactly = 1) { delegate.info("header", "long") }
        }

        Then("application and cluster namespaces are independent") {
            publisher().infoOnce("image:v1", ttl, "header", "message")
            publisher(application = "another").infoOnce("image:v1", ttl, "header", "message")
            publisher(cluster = "prod-gcp").infoOnce("image:v1", ttl, "header", "message")

            verify(exactly = 3) { delegate.info(any(), any()) }
        }

        Then("normal publisher methods are not deduplicated") {
            repeat(2) {
                publisher.info("header", "message")
                publisher.warn("header", "message")
                publisher.error("header", "message")
                publisher.publish("header", "message", ":rocket:", ":info:")
            }

            verify(exactly = 2) { delegate.info("header", "message") }
            verify(exactly = 2) { delegate.warn("header", "message") }
            verify(exactly = 2) { delegate.error("header", "message") }
            verify(exactly = 2) { delegate.publish("header", "message", ":rocket:", ":info:") }
        }

        Then("concurrent publishers sharing a cache make only one attempt") {
            val count = 8
            val ready = CountDownLatch(count)
            val start = CountDownLatch(1)
            Executors.newFixedThreadPool(count).use { executor ->
                val results = (1..count).map {
                    executor.submit(Callable {
                        ready.countDown()
                        check(start.await(10, SECONDS))
                        publisher().infoOnce("image:v1", ttl, "header", "message")
                    })
                }
                check(ready.await(10, SECONDS))
                start.countDown()
                results.forEach { it.get(10, SECONDS) }
            }

            verify(exactly = 1) { delegate.info(any(), any()) }
        }

        Then("publishing failures propagate and retain the marker until expiry") {
            every { delegate.info(any(), any()) } throws IllegalStateException("Publishing failed")

            shouldThrow<IllegalStateException> {
                publisher.infoOnce("image:v1", ttl, "header", "message")
            }
            publisher.infoOnce("image:v1", ttl, "header", "message")

            verify(exactly = 1) { delegate.info(any(), any()) }
        }

        Then("invalid keys TTLs and application names fail before publishing") {
            listOf("", " ").forEach { key ->
                shouldThrow<IllegalArgumentException> { publisher.infoOnce(key, ttl, "header", "message") }
            }
            listOf(Duration.ZERO, ofDays(-1)).forEach { duration ->
                shouldThrow<IllegalArgumentException> { publisher.infoOnce("key", duration, "header", "message") }
            }
            shouldThrow<IllegalArgumentException> {
                publisher(application = "").infoOnce("key", ttl, "header", "message")
            }

            verify(exactly = 0) { delegate.info(any(), any()) }
        }

        Then("Redis claims a namespaced marker atomically with the requested TTL") {
            val redis = mockk<StringRedisTemplate>()
            val values = mockk<ValueOperations<String, String>>()
            every { redis.opsForValue() } returns values
            every { values.setIfAbsent(any(), "true", ttl) } returnsMany listOf(true, false)
            val publisher = DeduplicatingMessagePublisher(delegate, MessageDeduplicator(redis), "test", "dev-gcp")

            repeat(2) { publisher.infoOnce("image:v1", ttl, "header", "message") }

            verify(exactly = 2) {
                values.setIfAbsent("notification-once:7:dev-gcp:4:test:info:image:v1", "true", ttl)
            }
            verify(exactly = 1) { delegate.info(any(), any()) }
        }

        Then("Redis failures propagate without publishing or using a local fallback") {
            val redis = mockk<StringRedisTemplate>()
            every { redis.opsForValue() } throws IllegalStateException("Redis unavailable")
            val publisher = DeduplicatingMessagePublisher(delegate, MessageDeduplicator(redis), "test", "dev-gcp")

            shouldThrow<IllegalStateException> {
                publisher.infoOnce("image:v1", ttl, "header", "message")
            }
            verify(exactly = 0) { delegate.info(any(), any()) }
        }
    }
})
