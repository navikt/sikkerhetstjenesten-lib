package no.nav.sikkerhetstjenesten.felles.cache

import org.slf4j.LoggerFactory.getLogger
import io.valkey.springframework.data.valkey.serializer.ValkeySerializer


class ResilientValkeySerializer(private val delegate: ValkeySerializer<Any>) : ValkeySerializer<Any> {

    private val log = getLogger(javaClass)

    override fun serialize(t: Any?): ByteArray =
        delegate.serialize(t)

    override fun deserialize(bytes: ByteArray?) =
        runCatching {
            delegate.deserialize(bytes)
        }.onFailure {
            log.info("Kunne ikke deserialisere cache-entry, behandler som miss", it)
        }.getOrNull()
}