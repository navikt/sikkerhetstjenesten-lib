package no.nav.sikkerhetstjenesten.felles.cache

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import io.valkey.springframework.boot.autoconfigure.data.valkey.ValkeyProperties
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.interceptor.AbstractCacheResolver
import org.springframework.cache.interceptor.CacheInterceptor
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.MapPropertySource
import io.valkey.springframework.data.valkey.cache.ValkeyCache
import io.valkey.springframework.data.valkey.cache.ValkeyCacheManager
import io.valkey.springframework.data.valkey.connection.ValkeyConnectionFactory
import io.valkey.springframework.data.valkey.core.StringValkeyTemplate
import java.util.function.Supplier

class CacheAutoConfigurationTest {

    @Test
    fun `caching uses the managed manager with initialized JSON serialization`() {
        AnnotationConfigApplicationContext().use { context ->
            context.environment.propertySources.addFirst(
                MapPropertySource("test", mapOf("NAIS_CLUSTER_NAME" to "prod-gcp"))
            )
            context.registerBean(ValkeyConnectionFactory::class.java, Supplier { mockk<ValkeyConnectionFactory>(relaxed = true) })
            context.registerBean(MeterRegistry::class.java, Supplier { SimpleMeterRegistry() })
            context.registerBean(ValkeyProperties::class.java, Supplier { ValkeyProperties() })
            context.registerBean(StringValkeyTemplate::class.java, Supplier {
                StringValkeyTemplate(context.getBean(ValkeyConnectionFactory::class.java))
            })
            context.registerBean(CachableRestConfig::class.java, Supplier {
                object : CachableRestConfig {
                    override val navn = "pdl"
                    override val caches = setOf(CacheNøkkelConfig(navn))
                }
            })
            context.register(CachingTestConfig::class.java, CacheAutoConfiguration::class.java)
            context.addBeanFactoryPostProcessor {
                it.getBeanDefinition("valkeyMessageListenerContainer").propertyValues.add("autoStartup", false)
                it.getBeanDefinition("valkeyCacheOperations").isLazyInit = true
            }
            context.refresh()

            val manager = context.getBean(ValkeyCacheManager::class.java)
            val interceptor = context.getBean(CacheInterceptor::class.java)
            val resolver = interceptor.cacheResolver as AbstractCacheResolver
            assertSame(manager, resolver.cacheManager)
            assertEquals(setOf("pdl"), manager.cacheNames.toSet())

            val cache = manager.getCache("pdl") as ValkeyCache
            val serialization = cache.cacheConfiguration.valueSerializationPair
            val person = CachedPerson("test")
            assertEquals(person, serialization.read(serialization.write(person)))
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableCaching
    class CachingTestConfig

    data class CachedPerson(val id: String)
}
