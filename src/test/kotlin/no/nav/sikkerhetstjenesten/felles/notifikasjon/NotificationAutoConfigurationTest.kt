package no.nav.sikkerhetstjenesten.felles.notifikasjon

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.kotest.matchers.shouldBe
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.core.env.MapPropertySource
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import org.zalando.logbook.HeaderFilter
import org.zalando.logbook.Logbook
import org.zalando.logbook.core.HeaderFilters.replaceHeaders
import org.zalando.logbook.spring.LogbookClientHttpRequestInterceptor
import tools.jackson.databind.json.JsonMapper
import java.util.function.Supplier

class NotificationAutoConfigurationTest {

    @ParameterizedTest
    @ValueSource(ints = [0, 1, 2])
    fun `managed Logbook applies all header filter beans and preserves default masking`(filterCount: Int) {
        val mapper = JsonMapper.builder().build()
        val names = listOf("ident", "X-Identifikator")
        val identifier = "12345678901"
        val authorization = "Bearer eyJhbGciOiJIUzI1NiJ9.e30.c2lnbmF0dXJl"
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
                    .retrieve().body(String::class.java) shouldBe """{"ident":"$identifier"}"""

                server.verify()
                val messages = appender.list.map { mapper.readTree(it.formattedMessage) }
                val request = messages.single { it.path("type").asText() == "request" }
                val headers = request.path("headers")
                headers.path("Authorization").get(0).asText() shouldBe "XXX"
                names.forEachIndexed { index, name ->
                    headers.path(name).get(0).asText() shouldBe
                        if (index < filterCount) "MASKED" else identifier
                }
                val response = messages.single { it.path("type").asText() == "response" }
                response.path("body").path("ident").asText() shouldBe "1234*******"
            }
        } finally {
            logger.detachAppender(appender)
            logger.level = previousLevel
            appender.stop()
        }
    }
}
