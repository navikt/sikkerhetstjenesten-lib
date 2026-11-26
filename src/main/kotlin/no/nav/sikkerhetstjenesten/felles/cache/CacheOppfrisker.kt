package no.nav.sikkerhetstjenesten.felles.cache

import no.nav.sikkerhetstjenesten.felles.rest.ConsumerAwareHandlerInterceptor.Companion.USER_ID
import org.slf4j.LoggerFactory.getLogger
import org.slf4j.MDC
import kotlin.system.measureTimeMillis


abstract class AbstractCacheOppfrisker(private val cfg: CachableRestConfig) : CacheOppfrisker {
    protected val log = getLogger(javaClass)

    override val cacheName = cfg.caches.map { it.name }.distinct()
        .also { require(it.size == 1) { "Alle cacher må ha nøyaktig ett felles name, men fikk $it" } }
        .single()

    protected abstract fun doOppfrisk(nøkkelElementer: CacheNøkkel)

    override fun oppfrisk(nøkkelElementer: CacheNøkkel) {
        val duration = measureTimeMillis {
            runCatching {
                MDC.put(USER_ID, nøkkelElementer.id)
                doOppfrisk(nøkkelElementer)
                log.info("Oppfrisking av $nøkkelElementer OK")
            }.getOrElse {
                log.warn("Oppfrisking av $nøkkelElementer feilet", it)
            }
        }
        log.trace("Oppfrisking tok {}ms for {}", duration, nøkkelElementer)
    }

}

interface CacheOppfrisker {
    val cacheName: String
    fun oppfrisk(nøkkelElementer: CacheNøkkel)
}