package no.nav.sikkerhetstjenesten.felles.leder

import io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import reactor.netty.http.client.HttpClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Bean
import org.springframework.web.reactive.function.client.WebClient
import java.net.URI

@AutoConfiguration
@ConditionalOnProperty("elector.sse.url")
class LederBeanConfiguration {

    @Bean
    fun sseLederUtvelger(client: WebClient, @Value($$"${elector.sse.url}") uri: URI) =
        SSELederUtvelger(client, uri)

    @Bean
    fun pollendeLederUtvelger(client: WebClient, @Value($$"${elector.get.url}") uri: URI) =
        PollendeLederUtvelger(client, uri)

    @Bean
    fun lederVarsler(publisher: ApplicationEventPublisher) = LederVarsler(publisher)

    @Bean
    fun lederUtvelger(sseUtvelger: SSELederUtvelger, pollendeUtvelger: PollendeLederUtvelger, varsler: LederVarsler) =
        LederUtvelger(sseUtvelger, pollendeUtvelger, varsler)

    @Bean
    fun electorWebClient(builder: WebClient.Builder): WebClient =
        builder
            .clientConnector(ReactorClientHttpConnector(
                HttpClient.create().option(CONNECT_TIMEOUT_MILLIS, 3000)
            ))
            .build()
}