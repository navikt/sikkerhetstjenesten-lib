package no.nav.sikkerhetstjenesten.felles.rest.health

import io.micrometer.core.aop.TimedAspect
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags
import no.nav.sikkerhetstjenesten.felles.security.AuthContext
import no.nav.sikkerhetstjenesten.felles.utils.cluster.ClusterUtils
import org.springframework.beans.factory.ListableBeanFactory
import org.springframework.boot.actuate.endpoint.SanitizableData.SANITIZED_VALUE
import org.springframework.boot.actuate.endpoint.SanitizingFunction
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.security.config.observation.SecurityObservationSettings
import java.util.function.Function

private val SENSITIVE_KEYS = setOf("password", "secret", "token", "key", "credentials", "jwk", "private_key")

@AutoConfiguration
@ConditionalOnClass(MeterRegistry::class)
@EnableAspectJAutoProxy
class ObservabilityAutoConfiguration {


    @Bean
    fun securityObservationSettings()  =
        SecurityObservationSettings.withDefaults().shouldObserveRequests(false)
            .build()

    @Bean
    fun sanitizingFunction() = SanitizingFunction { data ->
        if (SENSITIVE_KEYS.any { data.key.contains(it, ignoreCase = true) }) data.withValue(SANITIZED_VALUE) else data
    }

    @Bean
    fun clusterAddingTimedAspect(meterRegistry: MeterRegistry, authContext: AuthContext) =
        TimedAspect(
            meterRegistry,
            Function { pjp ->
                Tags.of(
                    "cluster",
                    ClusterUtils.current.clusterName,
                    "class",
                    pjp.target.javaClass.simpleName,
                    "method",
                    pjp.signature.name,
                    "client",
                    authContext.systemNavn
                )
            }
        )

    @Bean
    fun httpClientPoolMetrics(meterRegistry: MeterRegistry) = HttpClientPoolMetrics(meterRegistry)

    @Bean
    fun httpClientPoolMetricsBinder(metrics: HttpClientPoolMetrics, beanFactory: ListableBeanFactory) =
        HttpClientPoolMetricsBinder(metrics, beanFactory)

}
