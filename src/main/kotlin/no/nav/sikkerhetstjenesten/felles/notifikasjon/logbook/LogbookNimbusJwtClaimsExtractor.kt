package no.nav.sikkerhetstjenesten.felles.notifikasjon.logbook

import com.nimbusds.jwt.SignedJWT.parse
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.security.oauth2.core.OAuth2AccessToken.TokenType.BEARER
import org.zalando.logbook.HttpRequest
import org.zalando.logbook.attributes.AttributeExtractor
import org.zalando.logbook.attributes.HttpAttributes
import org.zalando.logbook.attributes.HttpAttributes.EMPTY

class LogbookNimbusJwtClaimsExtractor : AttributeExtractor {

    override fun extract(request: HttpRequest): HttpAttributes {
        val auth = request.headers.getFirst(AUTHORIZATION) ?: return EMPTY
        return HttpAttributes(parse(auth.removePrefix(BEARER.value) + " ").jwtClaimsSet.claims.withTimestampsInCurrentTimezone())
    }
}