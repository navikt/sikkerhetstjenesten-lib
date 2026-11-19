package no.nav.sikkerhetstjenesten.felles.notifikasjon

import org.slf4j.Logger
import org.slf4j.LoggerFactory.getLogger
import org.springframework.context.annotation.Fallback
import org.springframework.stereotype.Component
import java.time.Duration

@FunctionalInterface
interface MessagePublisher {

    private val logger: Logger
        get() = getLogger(MessagePublisher::class.java)

    fun info(header: String, msg: String) = logger.info("$header: $msg")
    fun warn(header: String, msg: String) = logger.warn("$header: $msg")
    fun error(header: String, msg: String) = logger.error("$header: $msg")
    fun publish(header: String, msg: String, vararg emojis: String) = info(header, msg)

    fun infoOnce(key: String, ttl: Duration, header: String, msg: String) =
        publishOnce(levelKey("info", key), ttl) { info(header, msg) }

    fun warnOnce(key: String, ttl: Duration, header: String, msg: String) =
        publishOnce(levelKey("warn", key), ttl) { warn(header, msg) }

    fun errorOnce(key: String, ttl: Duration, header: String, msg: String) =
        publishOnce(levelKey("error", key), ttl) { error(header, msg) }

    fun publishOnce(key: String, ttl: Duration, publish: () -> Unit): Unit =
        throw UnsupportedOperationException("Once-only publishing requires DeduplicatingMessagePublisher")

    private fun levelKey(level: String, key: String): String {
        require(key.isNotBlank()) { "Notification key must not be blank" }
        return "$level:$key"
    }
}

@Component
@Fallback
class LoggingMessagePublisher : MessagePublisher