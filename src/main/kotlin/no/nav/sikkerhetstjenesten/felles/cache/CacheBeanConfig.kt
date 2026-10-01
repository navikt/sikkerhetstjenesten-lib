package no.nav.sikkerhetstjenesten.felles.cache

import io.micrometer.core.instrument.MeterRegistry
import no.nav.boot.conditionals.ConditionalOnGCP
import no.nav.sikkerhetstjenesten.entraproxy.felles.cache.ResilientValkeySerializer
import no.nav.sikkerhetstjenesten.entraproxy.felles.cache.ValkeyCacheOperations
import no.nav.sikkerhetstjenesten.felles.rest.PingableHealthIndicator
import org.slf4j.LoggerFactory.getLogger
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.data.redis.autoconfigure.DataRedisProperties
import org.springframework.cache.Cache
import org.springframework.cache.annotation.CachingConfigurer
import org.springframework.cache.interceptor.CacheErrorHandler
import org.springframework.context.annotation.Bean
import org.springframework.data.redis.cache.RedisCacheConfiguration.defaultCacheConfig
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.cache.RedisCacheWriter.nonLockingRedisCacheWriter
import org.springframework.data.redis.config.RedisListenerConfigurer
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.listener.RedisMessageListenerContainer
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer
import org.springframework.data.redis.serializer.RedisMessageConverters
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair.fromSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer
import tools.jackson.core.StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule.Builder

@AutoConfiguration
@ConditionalOnGCP
class CacheBeanConfig(private val cf: RedisConnectionFactory,
                      private val registry: MeterRegistry,
                      private vararg val cfgs: CachableRestConfig) : CachingConfigurer, RedisListenerConfigurer {

    private val log = getLogger(javaClass)

    override fun errorHandler(): CacheErrorHandler =
        CacheMeteredErrorHandler(registry)

    override fun configureMessageConverters(builder: RedisMessageConverters.Builder) {
        builder.addCustomConverter(CacheNøkkelMessageConverter())
    }

    @Bean
    override fun cacheManager() =
        RedisCacheManager.builder(nonLockingRedisCacheWriter(cf))
            .withInitialCacheConfigurations(cfgs.associate {
                it.navn to cacheConfig(it)
            }).enableStatistics()
            .build()


    @Bean
    fun cachePingable(valkey: StringRedisTemplate, properties: DataRedisProperties) =
        CachePingable(valkey, properties)

    @Bean
    fun cacheHealthIndicator(pingable: CachePingable) =
        PingableHealthIndicator(pingable)

    @Bean
    fun valkeyCacheOperations(valkey: StringRedisTemplate) =
        ValkeyCacheOperations(valkey, *cfgs)


    private fun cacheConfig(cfg: CachableRestConfig) =
        defaultCacheConfig()
            .entryTtl(cfg.varighet)
            .serializeKeysWith(fromSerializer(StringRedisSerializer()))
            .serializeValuesWith(fromSerializer(
                ResilientValkeySerializer(GenericJacksonJsonRedisSerializer(VALKEY_MAPPER)))).apply {
                if (!cfg.cacheNulls) disableCachingNullValues()
            }

    @Bean(name = ["redisMessageListenerContainer"])
    fun redisMessageListenerContainer(cf: RedisConnectionFactory) =
        RedisMessageListenerContainer().apply {
            setConnectionFactory(cf)
            setRecoveryInterval(5_000) // retry subscription every 5s
            maxSubscriptionRegistrationWaitingTime = 30_000

            setErrorHandler {
                log.warn("Valkey listener feilet, retry om 5s", it)
            }
        }

    companion object {
        val VALKEY_MAPPER = JsonMapper.builder().polymorphicTypeValidator(NavPolymorphicTypeValidator()).apply {
            enable(INCLUDE_SOURCE_IN_LOCATION)
            addModules(Builder().build(), JacksonTypeInfoAddingValkeyModule())
        }.build()
    }
}

private class CacheMeteredErrorHandler(private val registry: MeterRegistry) : CacheErrorHandler {
    private val log = getLogger(javaClass)

    override fun handleCacheGetError(e: RuntimeException, cache: Cache, key: Any) =
        record("get", cache, e)

    override fun handleCachePutError(e: RuntimeException, cache: Cache, key: Any, value: Any?) =
        record("put", cache, e)

    override fun handleCacheEvictError(e: RuntimeException, cache: Cache, key: Any) =
        record("evict", cache, e).also {
            throw e
        }

    override fun handleCacheClearError(e: RuntimeException, cache: Cache) =
        record("clear", cache, e).also {
            throw e
        }

    private fun record(op: String, cache: Cache, e: RuntimeException) {
        registry.counter("cache.operation.failed", "op", op, "cache", cache.name,
            "exception", e.javaClass.simpleName).increment()
        log.warn("Cache $op feilet for ${cache.name}: ${e.message}")
    }
}


