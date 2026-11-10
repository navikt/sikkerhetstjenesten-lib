package no.nav.sikkerhetstjenesten.felles.cache

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.cache.caffeine.CaffeineCacheManager

class CaffeineCacheOperationsTest {
    private val cacheManager = CaffeineCacheManager("person", "cache-sets")
    private val operations = CaffeineCacheOperations(cacheManager)
    private val cache = CacheNøkkelConfig("person")
    private val familyCache = CacheNøkkelConfig("person", "family")

    @Test
    fun `single values use the same keys as Spring caching`() {
        operations.putOne(familyCache, "id-1", "value")

        operations.getOne<String>(familyCache, "id-1") shouldBe "value"
        requireNotNull(cacheManager.getCache("person")).get("family:id-1", String::class.java) shouldBe "value"
        operations.getOne<String>(cache, "id-1") shouldBe null
    }

    @Test
    fun `bulk reads return only cached values`() {
        operations.putMany(familyCache, mapOf("id-1" to "first", "id-2" to "second"))

        operations.getMany<String>(familyCache, setOf("id-1", "id-2", "missing")) shouldBe
            mapOf("id-1" to "first", "id-2" to "second")
        operations.getMany<String>(familyCache, emptySet()) shouldBe emptyMap()
    }

    @Test
    fun `deleting a value reports whether it existed`() {
        operations.putOne(familyCache, "id-1", "value")

        operations.delete(familyCache, "id-1") shouldBe true
        operations.getOne<String>(familyCache, "id-1") shouldBe null
        operations.delete(familyCache, "id-1") shouldBe false
    }

    @Test
    fun `clearing a prefix preserves other values in the cache`() {
        operations.putOne(cache, "id-1", "plain")
        operations.putMany(familyCache, mapOf("id-1" to "first", "id-2" to "second"))
        operations.putOne(CacheNøkkelConfig("person", "other"), "id-1", "other")

        operations.size(familyCache) shouldBe 2L
        operations.clear(familyCache) shouldBe 2L

        operations.getMany<String>(familyCache, setOf("id-1", "id-2")) shouldBe emptyMap()
        operations.getOne<String>(cache, "id-1") shouldBe "plain"
        operations.getOne<String>(CacheNøkkelConfig("person", "other"), "id-1") shouldBe "other"
        operations.size(cache) shouldBe 2L
    }

    @Test
    fun `clearing an unprefixed cache removes all its entries`() {
        operations.putOne(cache, "id-1", "plain")
        operations.putOne(familyCache, "id-1", "family")

        operations.size(cache) shouldBe 2L
        operations.clear(cache) shouldBe 2L
        operations.size(cache) shouldBe 0L
    }

    @Test
    fun `sets can be stored and read`() {
        operations.getSet("missing") shouldBe emptySet()

        operations.putSet("groups", setOf("first", "second"))

        operations.getSet("groups") shouldBe setOf("first", "second")
    }
}
