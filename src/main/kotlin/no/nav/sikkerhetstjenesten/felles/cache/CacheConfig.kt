package no.nav.sikkerhetstjenesten.felles.cache


import glide.api.models.configuration.GlideClientConfiguration
import glide.api.models.configuration.NodeAddress
import glide.api.models.configuration.ServerCredentials
import no.nav.sikkerhetstjenesten.felles.cache.CacheConfig.Companion.VALKEY
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(VALKEY)
data class CacheConfig(val username: String, val password: String, val host: String, val port: String) {
    val cacheConfiguration: GlideClientConfiguration = GlideClientConfiguration.builder()
        .address(NodeAddress.builder().host(host).port(port.toInt()).build())
        .useTLS(true)
        .credentials(ServerCredentials.builder().username(username).password(password).build())
        .build()

    companion object {
        const val VALKEY = "valkey"
    }
}

