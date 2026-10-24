package no.nav.sikkerhetstjenesten.felles.cache

import org.slf4j.LoggerFactory.getLogger
import kotlin.system.measureTimeMillis


abstract class AbstractCacheOppfrisker(private vararg val caches: CacheNøkkelConfig) : CacheOppfrisker {
    protected val log = getLogger(javaClass)

    override val cacheName = caches.map { it.name }.distinct()
        .also { require(it.size == 1) { "Alle cacher må ha nøyaktig ett felles name, men fikk $it" } }
        .single()

    protected abstract fun doOppfrisk(nøkkelElementer: CacheNøkkel)

    override fun oppfrisk(nøkkelElementer: CacheNøkkel) {
        val duration = measureTimeMillis {
            runCatching {
                doOppfrisk(nøkkelElementer)
                log.info("Oppfrisking av $nøkkelElementer OK")
            }.getOrElse {
                loggOppfriskingFeilet(nøkkelElementer, it)
            }
        }
        log.info("Oppfrisking tok ${duration}ms for $nøkkelElementer")
    }
    protected fun loggOppfriskingFeilet(elementer: CacheNøkkel, feil: Throwable) {
        log.warn("Oppfrisking av $elementer feilet", feil)
    }
}

interface CacheOppfrisker {
    val cacheName: String
    fun oppfrisk(nøkkelElementer: CacheNøkkel)
}