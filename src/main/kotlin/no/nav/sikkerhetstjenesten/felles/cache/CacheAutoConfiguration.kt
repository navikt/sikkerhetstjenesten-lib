package no.nav.sikkerhetstjenesten.felles.cache

import io.micrometer.core.instrument.MeterRegistry
import no.nav.boot.conditionals.ConditionalOnGCP
import no.nav.sikkerhetstjenesten.felles.rest.PingableHealthIndicator
import org.slf4j.LoggerFactory.getLogger
import org.springframework.boot.autoconfigure.AutoConfiguration
import io.valkey.springframework.boot.autoconfigure.data.valkey.ValkeyProperties
import org.springframework.cache.annotation.CachingConfigurer
import org.springframework.cache.interceptor.CacheErrorHandler
import org.springframework.context.annotation.Bean
import io.valkey.springframework.data.valkey.cache.ValkeyCacheConfiguration.defaultCacheConfig
import io.valkey.springframework.data.valkey.cache.ValkeyCacheManager
import io.valkey.springframework.data.valkey.cache.ValkeyCacheWriter.nonLockingValkeyCacheWriter
import io.valkey.springframework.data.valkey.config.ValkeyListenerConfigurer
import io.valkey.springframework.data.valkey.connection.ValkeyConnectionFactory
import io.valkey.springframework.data.valkey.core.StringValkeyTemplate
import io.valkey.springframework.data.valkey.listener.ValkeyMessageListenerContainer
import io.valkey.springframework.data.valkey.serializer.GenericJacksonJsonValkeySerializer
import io.valkey.springframework.data.valkey.serializer.ValkeyMessageConverters
import io.valkey.springframework.data.valkey.serializer.ValkeySerializationContext.SerializationPair.fromSerializer
import io.valkey.springframework.data.valkey.serializer.StringValkeySerializer
import tools.jackson.core.StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule.Builder

@AutoConfiguration
@ConditionalOnGCP
class CacheAutoConfiguration(private val cf: ValkeyConnectionFactory,
                      private val registry: MeterRegistry,
                      private vararg val cfgs: CachableRestConfig) : CachingConfigurer, ValkeyListenerConfigurer {

    private val log = getLogger(javaClass)

    override fun errorHandler()  =
        CacheMeteredErrorHandler(registry)

    override fun configureMessageConverters(builder: ValkeyMessageConverters.Builder) {
        builder.addCustomConverter(CacheNøkkelMessageConverter())
    }

    @Bean(name = ["cacheManager"])
    fun valkeyCacheManager() =
        ValkeyCacheManager.builder(nonLockingValkeyCacheWriter(cf))
            .withInitialCacheConfigurations(cfgs.associate {
                it.navn to cacheConfig(it)
            }).enableStatistics()
            .build()


    @Bean
    fun cachePingable(valkey: StringValkeyTemplate, properties: ValkeyProperties) =
        CachePingable(valkey, properties)

    @Bean
    fun cacheHealthIndicator(pingable: CachePingable) =
        PingableHealthIndicator(pingable)

    @Bean
    fun valkeyCacheOperations(valkey: StringValkeyTemplate) =
        ValkeyCacheOperations(valkey, *cfgs)

    @Bean
    fun valkeyEventListeningCacheOppfrisker(oppfriskere: List<CacheOppfrisker>) =
        ValkeyEventListeningCacheOppfrisker(oppfriskere = oppfriskere.toTypedArray())




    @Bean(name = ["valkeyMessageListenerContainer"])
    fun valkeyMessageListenerContainer(cf: ValkeyConnectionFactory) =
        ValkeyMessageListenerContainer().apply {
            setConnectionFactory(cf)
            setRecoveryInterval(5_000) // retry subscription every 5s
            maxSubscriptionRegistrationWaitingTime = 30_000

            setErrorHandler {
                log.warn("Valkey listener feilet, retry om 5s", it)
            }
        }

    private fun cacheConfig(cfg: CachableRestConfig) =
        defaultCacheConfig()
            .entryTtl(cfg.varighet)
            .serializeKeysWith(fromSerializer(StringValkeySerializer()))
            .serializeValuesWith(fromSerializer(
                ResilientValkeySerializer(GenericJacksonJsonValkeySerializer(VALKEY_MAPPER)))).apply {
                if (!cfg.cacheNulls) disableCachingNullValues()
            }

    companion object {
        val VALKEY_MAPPER = JsonMapper.builder().polymorphicTypeValidator(NavPolymorphicTypeValidator()).apply {
            enable(INCLUDE_SOURCE_IN_LOCATION)
            addModules(Builder().build(), JacksonTypeInfoAddingValkeyModule())
        }.build()
    }
}

