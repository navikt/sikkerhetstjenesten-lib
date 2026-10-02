package no.nav.sikkerhetstjenesten.felles.notifikasjon

import no.nav.boot.conditionals.ConditionalOnGCP
import no.nav.boot.conditionals.ConditionalOnNotProd
import no.nav.sikkerhetstjenesten.felles.domain.DomainExtensions.maskFnr
import no.nav.sikkerhetstjenesten.felles.notifikasjon.logbook.LogbookNimbusJwtClaimsExtractor
import no.nav.sikkerhetstjenesten.felles.notifikasjon.logbook.LogbookPrettyPrintingFormatter
import no.nav.sikkerhetstjenesten.felles.notifikasjon.logbook.LogbookStatusAtLeastExcluding
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Fallback
import org.springframework.http.HttpStatus.NOT_FOUND
import org.zalando.logbook.Logbook
import org.zalando.logbook.attributes.AttributeExtractor
import org.zalando.logbook.core.Conditions.exclude
import org.zalando.logbook.core.Conditions.requestTo
import org.zalando.logbook.core.DefaultHttpLogWriter
import org.zalando.logbook.core.DefaultSink
import tools.jackson.databind.json.JsonMapper

private val BRUKER_ID_REGEX = Regex("""(?<!\d)\d{11}(?!\d)""")

@AutoConfiguration
class NotificationBeanConfig {

    @Bean
    @Fallback
    fun localAuditor(): Auditor = LocalAuditor()

    @Bean
    @ConditionalOnGCP
    fun secureAuditor(): Auditor = SecureAuditor()

    @Bean
    @Fallback
    fun loggingMessagePublisher(): MessagePublisher = LoggingMessagePublisher()

    @Bean
    @ConditionalOnGCP
    fun slackMessagePublisher(@Value("\${slack.webhook:}") url: String): MessagePublisher =
        SlackMessagePublisher(url)

    @Bean
    fun restRetryLogger() = RestRetryLogger()

    @Bean
    @ConditionalOnNotProd
    fun logbookJwtClaimsExtractor(): AttributeExtractor = LogbookNimbusJwtClaimsExtractor()

    @Bean
    @ConditionalOnNotProd
    fun logbookPrettyPrintingFormatter(mapper: JsonMapper) =
        LogbookPrettyPrintingFormatter(mapper)

    @Bean
    @ConditionalOnNotProd
    fun logbook(formatter: LogbookPrettyPrintingFormatter, jwtClaimsExtractor: AttributeExtractor) =
        Logbook.builder()
            .strategy(LogbookStatusAtLeastExcluding(NOT_FOUND))
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
