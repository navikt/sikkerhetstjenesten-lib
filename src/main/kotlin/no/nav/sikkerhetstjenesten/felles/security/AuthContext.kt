package no.nav.sikkerhetstjenesten.felles.security

import no.nav.sikkerhetstjenesten.felles.domain.AnsattId
import no.nav.sikkerhetstjenesten.felles.security.TokenType.CCF
import no.nav.sikkerhetstjenesten.felles.security.TokenType.OBO
import no.nav.sikkerhetstjenesten.felles.security.TokenType.UNAUTHENTICATED
import org.springframework.security.core.context.SecurityContextHolder.getContext
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.AbstractOAuth2TokenAuthenticationToken
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class AuthContext {
    val globaleGruppeIds: Set<UUID>
        get() =
            listClaim(GROUPS)
                .mapNotNullTo(mutableSetOf()) { value ->
                    runCatching { UUID.fromString(value.toString()) }.getOrNull()
                }

    val system get() = stringClaim(AZP_NAME) ?: UTILGJENGELIG
    val oid get() = stringClaim(OID)?.let { runCatching { UUID.fromString(it) }.getOrNull() }
    val ansattId get() = stringClaim(NAVIDENT)?.let(::AnsattId)
    val clusterAndSystem
        get() = system.split(":").let { parts ->
            if (parts.size == 3) "${parts[2]}:${parts[0]}" else system
        }
    val systemNavn get() = system.split(":").last()
    val systemAndNs get() = system.split(":").drop(1).joinToString(separator = ":")
    val type
        get() = when {
            erObo -> OBO
            erCC -> CCF
            else -> UNAUTHENTICATED
        }

    private val erCC get() = listClaim(ROLES).contains(CLIENT_CREDENTIALS)
    private val erObo get() = !erCC && oid != null

    private fun stringClaim(name: String) = jwt()?.claims?.get(name)?.toString()
    private fun listClaim(name: String) = (jwt()?.claims?.get(name) as? List<*>) ?: emptyList<Any>()
    private fun jwt() = getContext().authentication?.let { authentication ->
        when (val principal = authentication.principal) {
            is Jwt -> principal
            else -> (authentication as? AbstractOAuth2TokenAuthenticationToken<*>)?.token as? Jwt
        }
    }

    companion object {
        const val CLIENT_CREDENTIALS = "access_as_application"
        const val GROUPS = "groups"
        const val ROLES = "roles"
        const val OID = "oid"
        const val AZP_NAME = "azp_name"
        const val NAVIDENT = "NAVident"
    }
}

enum class TokenType {
    OBO, CCF, UNAUTHENTICATED
}
