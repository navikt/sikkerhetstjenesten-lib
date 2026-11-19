package no.nav.sikkerhetstjenesten.felles.notifikasjon

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.core.env.MapPropertySource
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import org.zalando.logbook.HeaderFilter
import org.zalando.logbook.Logbook
import org.zalando.logbook.core.HeaderFilters.replaceHeaders
import org.zalando.logbook.spring.LogbookClientHttpRequestInterceptor
import tools.jackson.databind.json.JsonMapper
import java.time.Duration.ofDays
import java.util.function.Supplier

class NotificationAutoConfigurationTest : BehaviorSpec({

    Given("a local publisher without Redis") {
        Then("once-only notifications are supported") {
            AnnotationConfigApplicationContext().use { context ->
                context.environment.propertySources.addFirst(
                    MapPropertySource("test", mapOf("NAIS_CLUSTER_NAME" to "local", "spring.application.name" to "test"))
                )
                context.register(NotificationAutoConfiguration::class.java)
                context.refresh()

                val publisher = context.getBean(MessagePublisher::class.java)
                publisher.infoOnce("startup", ofDays(30), "header", "message")
                publisher.infoOnce("startup", ofDays(30), "header", "message")
                publisher.javaClass shouldBe DeduplicatingMessagePublisher::class.java
                var attempts = 0
                repeat(2) { publisher.publishOnce("attempt", ofDays(30)) { attempts++ } }
                attempts shouldBe 1
            }
        }
    }

    listOf(false, true).forEach { slack ->
        Given("a GCP publisher with slack=$slack") {
            Then("Redis is used instead of the local fallback") {
                val redis = mockk<StringRedisTemplate>(relaxed = true)
                val values = mockk<ValueOperations<String, String>>()
                every { redis.opsForValue() } returns values
                every { values.setIfAbsent(any(), "true", ofDays(30)) } returnsMany listOf(true, false)

                AnnotationConfigApplicationContext().use { context ->
                    val properties = mutableMapOf<String, Any>(
                        "NAIS_CLUSTER_NAME" to "dev-gcp",
                        "nais.cluster.name" to "dev-gcp",
                        "spring.application.name" to "test",
                    )
                    if (slack) properties["slack.webhook"] = ""
                    context.environment.propertySources.addFirst(MapPropertySource("test", properties))
                    context.registerBean(JsonMapper::class.java, Supplier { JsonMapper.builder().build() })
                    context.registerBean(StringRedisTemplate::class.java, Supplier { redis })
                    context.register(NotificationAutoConfiguration::class.java)
                    context.refresh()

                    val publisher = context.getBean(MessagePublisher::class.java)
                    var attempts = 0
                    repeat(2) { publisher.publishOnce("startup", ofDays(30)) { attempts++ } }

                    attempts shouldBe 1
                    verify(exactly = 2) {
                        values.setIfAbsent("notification-once:7:dev-gcp:4:test:startup", "true", ofDays(30))
                    }
                }
            }
        }
    }

    listOf(0, 1, 2).forEach { filterCount ->
        Given("managed Logbook with $filterCount header filter beans") {
            Then("all header filters are applied and default masking is preserved") {
                val mapper = JsonMapper.builder().build()
                val names = listOf("ident", "X-Identifikator")
                val identifier = "12345678901"
                val authorization = "******"
                val logger = LoggerFactory.getLogger(Logbook::class.java) as Logger
                val previousLevel = logger.level
                val appender = ListAppender<ILoggingEvent>().apply { start() }
                logger.addAppender(appender)
                logger.level = Level.TRACE

                try {
                    AnnotationConfigApplicationContext().use { context ->
                        context.environment.propertySources.addFirst(
                            MapPropertySource("test", mapOf("NAIS_CLUSTER_NAME" to "prod-gcp"))
                        )
                        context.registerBean(JsonMapper::class.java, Supplier { mapper })
                        context.registerBean(StringRedisTemplate::class.java, Supplier { mockk(relaxed = true) })
                        names.take(filterCount).forEachIndexed { index, name ->
                            context.registerBean("headerFilter$index", HeaderFilter::class.java, Supplier {
                                replaceHeaders(name, "MASKED")
                            })
                        }
                        context.register(NotificationAutoConfiguration::class.java)
                        context.refresh()

                        val builder = RestClient.builder().requestInterceptor(
                            LogbookClientHttpRequestInterceptor(context.getBean(Logbook::class.java))
                        )
                        val server = MockRestServiceServer.bindTo(builder).build()
                        server.expect(requestTo("https://example.test/person"))
                            .andExpect(header("Authorization", authorization))
                            .andExpect(header("ident", identifier))
                            .andExpect(header("X-Identifikator", identifier))
                            .andRespond(withSuccess("""{"ident":"$identifier"}""", APPLICATION_JSON))

                        builder.build().get().uri("https://example.test/person")
                            .header("Authorization", authorization)
                            .header("ident", identifier)
                            .header("X-Identifikator", identifier)
                            .retrieve().body<String>() shouldBe """{"ident":"$identifier"}"""

                        server.verify()
                        val messages = appender.list.map { mapper.readTree(it.formattedMessage) }
                        val request = messages.single { it.path("type").asString() == "request" }
                        val headers = request.path("headers")
                        headers.path("Authorization").get(0).asString() shouldBe "XXX"
                        names.forEachIndexed { index, name ->
                            headers.path(name).get(0).asString() shouldBe
                                if (index < filterCount) "MASKED" else identifier
                        }
                        val response = messages.single { it.path("type").asString() == "response" }
                        response.path("body").path("ident").asString() shouldBe "1234*******"
                    }
                } finally {
                    logger.detachAppender(appender)
                    logger.level = previousLevel
                    appender.stop()
                }
            }
        }
    }
})
