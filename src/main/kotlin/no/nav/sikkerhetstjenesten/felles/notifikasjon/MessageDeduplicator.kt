package no.nav.sikkerhetstjenesten.felles.notifikasjon

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.Expiry
import org.springframework.data.redis.core.StringRedisTemplate
import java.time.Duration

class MessageDeduplicator(
    private val valkey: StringRedisTemplate? = null,
    ticker: () -> Long = System::nanoTime,
) {
    private val local = Caffeine.newBuilder()
        .ticker { ticker() }
        .expireAfter(Expiry.creating<String, Duration> { _, ttl -> ttl })
        .build<String, Duration>()

    fun claim(key: String, ttl: Duration): Boolean =
        if (valkey != null) {
            checkNotNull(valkey.opsForValue().setIfAbsent(key, "true", ttl)) {
                "Redis did not return a result for notification deduplication"
            }
        } else {
            local.asMap().putIfAbsent(key, ttl) == null
        }
}