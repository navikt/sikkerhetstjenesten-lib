package no.nav.sikkerhetstjenesten.felles.cache

import java.time.Duration
import kotlin.reflect.KClass

interface CacheOperations {
    fun delete(cache: no.nav.sikkerhetstjenesten.felles.cache.CacheNøkkelConfig, id: String): Boolean
    fun <T : Any> getOne(cache: no.nav.sikkerhetstjenesten.felles.cache.CacheNøkkelConfig, id: String, clazz: KClass<T>): T?
    fun putOne(cache: no.nav.sikkerhetstjenesten.felles.cache.CacheNøkkelConfig, id: String, value: Any, ttl: Duration? = null)
    fun <T : Any> getMany(cache: no.nav.sikkerhetstjenesten.felles.cache.CacheNøkkelConfig, ids: Set<String>, clazz: KClass<T>): Map<String, T?>
    fun putMany(cache: no.nav.sikkerhetstjenesten.felles.cache.CacheNøkkelConfig, innslag: Map<String, Any>, ttl: Duration? = null)
    fun clear(cache: no.nav.sikkerhetstjenesten.felles.cache.CacheNøkkelConfig): Long
    fun clear(caches: Set<no.nav.sikkerhetstjenesten.felles.cache.CacheNøkkelConfig>) = caches.sumOf { clear(it) }
    fun clearAll(): Long
    fun size(cache: no.nav.sikkerhetstjenesten.felles.cache.CacheNøkkelConfig): Long = sizes(cache).values.single()
    fun sizes(vararg caches: no.nav.sikkerhetstjenesten.felles.cache.CacheNøkkelConfig): Map<String, Long>
}

inline fun <reified T : Any> no.nav.sikkerhetstjenesten.felles.cache.CacheOperations.getOne(cfg: no.nav.sikkerhetstjenesten.felles.cache.CacheNøkkelConfig, id: String): T? =
    getOne(cfg, id, T::class)

inline fun <reified T : Any> no.nav.sikkerhetstjenesten.felles.cache.CacheOperations.getMany(cfg: no.nav.sikkerhetstjenesten.felles.cache.CacheNøkkelConfig, ids: Set<String>): Map<String, T?> =
    getMany(cfg, ids, T::class)
