package no.nav.sikkerhetstjenesten.felles.notifikasjon

import java.time.Duration

class DeduplicatingMessagePublisher(
    private val delegate: MessagePublisher,
    private val deduplicator: MessageDeduplicator,
    private val applicationName: String,
    private val clusterName: String,
) : MessagePublisher {
    override fun info(header: String, msg: String) = delegate.info(header, msg)
    override fun warn(header: String, msg: String) = delegate.warn(header, msg)
    override fun error(header: String, msg: String) = delegate.error(header, msg)
    override fun publish(header: String, msg: String, vararg emojis: String) =
        delegate.publish(header, msg, *emojis)

    override fun publishOnce(key: String, ttl: Duration, publish: () -> Unit) {
        require(applicationName.isNotBlank()) { "spring.application.name is required for once-only publishing" }
        require(key.isNotBlank()) { "Notification key must not be blank" }
        require(!ttl.isNegative && !ttl.isZero) { "Notification TTL must be positive" }
        val cacheKey = "notification-once:${clusterName.length}:$clusterName:${applicationName.length}:$applicationName:$key"
        if (deduplicator.claim(cacheKey, ttl)) {
            publish()
        }
    }
}

