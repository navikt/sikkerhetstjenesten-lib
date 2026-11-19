package no.nav.sikkerhetstjenesten.felles.notifikasjon

import no.nav.boot.conditionals.ConditionalOnGCP
import no.nav.sikkerhetstjenesten.felles.utils.extensions.DomainExtensions.maskFnr
import no.nav.sikkerhetstjenesten.felles.notifikasjon.logbook.LogbookNimbusJwtClaimsExtractor
import no.nav.sikkerhetstjenesten.felles.notifikasjon.logbook.LogbookPrettyPrintingFormatter
import no.nav.sikkerhetstjenesten.felles.notifikasjon.logbook.LogbookStatusAtLeastExcluding
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Fallback
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.http.HttpStatus.CONTINUE
import org.zalando.logbook.HeaderFilter
import org.zalando.logbook.Logbook
import org.zalando.logbook.attributes.AttributeExtractor
import org.zalando.logbook.core.Conditions.exclude
import org.zalando.logbook.core.Conditions.requestTo
import org.zalando.logbook.core.DefaultHttpLogWriter
import org.zalando.logbook.core.DefaultSink
import org.zalando.logbook.core.HeaderFilters.defaultValue
import tools.jackson.databind.json.JsonMapper

private val BRUKER_ID_REGEX = Regex("""(?<!\d)\d{11}(?!\d)""")

@AutoConfiguration
class NotificationAutoConfiguration {

    @Bean
    @Fallback
    fun localAuditor(): Auditor = LocalAuditor()

    @Bean
    @ConditionalOnGCP
    fun secureAuditor(): Auditor = SecureAuditor()

    @Bean
    @Fallback
    fun localMessageDeduplicator() = MessageDeduplicator()

    @Bean
    @ConditionalOnGCP
    fun redisMessageDeduplicator(valkey: StringRedisTemplate) = MessageDeduplicator(valkey)

    @Bean
    @Fallback
    fun loggingMessagePublisher(
        deduplicator: MessageDeduplicator,
        @Value($$"${spring.application.name:}") applicationName: String,
        @Value($$"${nais.cluster.name:local}") clusterName: String,
    ): MessagePublisher =
        DeduplicatingMessagePublisher(LoggingMessagePublisher(), deduplicator, applicationName, clusterName)

    @Bean
    @ConditionalOnGCP
    @ConditionalOnProperty(prefix = "slack", name = ["webhook"])
    fun slackMessagePublisher(
        @Value($$"${slack.webhook}") url: String,
        deduplicator: MessageDeduplicator,
        @Value($$"${spring.application.name:}") applicationName: String,
        @Value($$"${nais.cluster.name:local}") clusterName: String,
    ): MessagePublisher =
        DeduplicatingMessagePublisher(SlackMessagePublisher(url), deduplicator, applicationName, clusterName)

    @Bean
    fun restRetryLogger() = RestRetryLogger()

    @Bean
    @ConditionalOnGCP
    fun logbookJwtClaimsExtractor(): AttributeExtractor = LogbookNimbusJwtClaimsExtractor()

    @Bean
    @ConditionalOnGCP
    fun logbookPrettyPrintingFormatter(mapper: JsonMapper) =
        LogbookPrettyPrintingFormatter(mapper)

    @Bean
    @ConditionalOnGCP
    fun logbook(
        formatter: LogbookPrettyPrintingFormatter,
        jwtClaimsExtractor: AttributeExtractor,
        headerFilters: List<HeaderFilter>,
    ) =
        Logbook.builder()
            .headerFilter(defaultValue())
            .headerFilters(headerFilters)
            .strategy(LogbookStatusAtLeastExcluding(CONTINUE))
            .bodyFilter { _, body ->
                BRUKER_ID_REGEX.replace(body) { m -> m.value.maskFnr() }
            }
            .condition(
                exclude(
                    requestTo("**/internal/**"),
                    requestTo("**/monitoring/**"),
                    requestTo("**/actuator/**"),
                    requestTo("https://graph.microsoft.com/v1.0/organization"),
                ),
            )
            .attributeExtractor(jwtClaimsExtractor)
            .sink(DefaultSink(formatter, DefaultHttpLogWriter()))
            .build()
}
