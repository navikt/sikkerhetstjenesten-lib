package no.nav.sikkerhetstjenesten.felles.security

import no.nav.sikkerhetstjenesten.felles.domain.DomainExtensions.UTILGJENGELIG
import no.nav.sikkerhetstjenesten.felles.rest.DownstreamURIContext.currentUri

import org.slf4j.LoggerFactory.getLogger
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.client.ClientAuthorizationException
import org.springframework.security.oauth2.client.OAuth2AuthorizationFailureHandler
import org.springframework.security.oauth2.core.OAuth2AuthorizationException

class OAuth2LoggingAuthorizationFailureHandler(
    private val delegate: OAuth2AuthorizationFailureHandler) : OAuth2AuthorizationFailureHandler {
    private val log = getLogger(javaClass)

    override fun onAuthorizationFailure(e: OAuth2AuthorizationException, principal: Authentication, attr: Map<String, Any>) {
        val registrationId = (e as? ClientAuthorizationException)?.clientRegistrationId ?: UTILGJENGELIG
        val uri = currentUri ?:UTILGJENGELIG
        log.debug("OAuth2 authorization feilet for id={}, errorCode={}, uri={}", registrationId, e.error.errorCode, uri)
        delegate.onAuthorizationFailure(e, principal, attr)
    }
}
