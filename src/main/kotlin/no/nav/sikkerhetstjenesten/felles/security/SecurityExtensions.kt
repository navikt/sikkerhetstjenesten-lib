package no.nav.sikkerhetstjenesten.felles.security

import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy.STATELESS
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal

object SecurityExtensions {

     fun HttpSecurity.stateless() =
        requestCache { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(STATELESS) }
            .csrf { it.disable() }
            .formLogin { it.disable() }
            .httpBasic { it.disable() }
            .logout { it.disable() }

    fun <R> OAuth2AuthenticatedPrincipal.requiredAttribute(attributeName: String, mapper: (String) -> R): R =
        mapper(requireNotNull(getAttribute(attributeName)) { "Mangler $attributeName i token" })
}