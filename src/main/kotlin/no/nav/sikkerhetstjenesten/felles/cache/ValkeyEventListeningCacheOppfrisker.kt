package no.nav.sikkerhetstjenesten.felles.cache

import no.nav.sikkerhetstjenesten.felles.leder.LeaderAware
import org.slf4j.LoggerFactory.getLogger
import org.springframework.data.redis.annotation.RedisListener

private const val CHANNEL_EXPIRED = "__keyevent@0__:expired"
private const val CHANNEL_DELETED = "__keyevent@0__:del"

class ValkeyEventListeningCacheOppfrisker(erLeder: Boolean = true,
                                          private vararg val oppfriskere: CacheOppfrisker) : LeaderAware(erLeder) {
    private val log = getLogger(javaClass)

    init {
       log.info("ValkeyEventListeningCacheOppfrisker opprettet med ${oppfriskere.size} oppfriskere")
    }

    @RedisListener(CHANNEL_EXPIRED)
    @RedisListener(CHANNEL_DELETED)
    fun onEvent(nokkel: CacheNøkkel) {

        somLeder {
            log.info(" mottok event for nøkkel ${nokkel.cacheName}")
            oppfriskere.firstOrNull { it.cacheName == nokkel.cacheName }?.run {
                log.info("Oppfrisker for cache ${nokkel.cacheName} fant matchende oppfrisker, oppfrisker cache $nokkel")
                oppfrisk(nokkel)
            }
        }
    }

}
