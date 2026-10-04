package no.nav.sikkerhetstjenesten.felles.security

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import no.nav.sikkerhetstjenesten.felles.security.AuthContext.Companion.AZP_NAME
import no.nav.sikkerhetstjenesten.felles.security.AuthContext.Companion.CLIENT_CREDENTIALS
import no.nav.sikkerhetstjenesten.felles.security.AuthContext.Companion.NAVIDENT
import no.nav.sikkerhetstjenesten.felles.security.AuthContext.Companion.OID
import no.nav.sikkerhetstjenesten.felles.security.AuthContext.Companion.ROLES
import no.nav.sikkerhetstjenesten.felles.security.TokenType.CCF
import no.nav.sikkerhetstjenesten.felles.security.TokenType.OBO
import no.nav.sikkerhetstjenesten.felles.security.TokenType.UNAUTHENTICATED
import no.nav.sikkerhetstjenesten.felles.utils.extensions.DomainExtensions.UTILGJENGELIG
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import java.time.Instant
import java.util.UUID

class AuthContextTest : BehaviorSpec({
    val authContext = AuthContext()
    val oid = UUID.randomUUID()

    fun setClaims(vararg claims: Pair<String, Any>) {
        val now = Instant.now()
        val jwt = Jwt.withTokenValue("token")
            .header("alg", "none")
            .claims { it.putAll(claims.toMap()) }
            .issuedAt(now)
            .expiresAt(now.plusSeconds(3600))
            .build()
        SecurityContextHolder.getContext().authentication = JwtAuthenticationToken(jwt)
    }

    fun clearContext() {
        SecurityContextHolder.clearContext()
    }

    beforeEach { clearContext() }
    afterSpec { clearContext() }

    Given("type er CCF") {
        When("idtyp er app") {
            Then("returnerer CCF") {
                setClaims(ROLES to listOf(CLIENT_CREDENTIALS))
                authContext.type shouldBe CCF
            }
        }
    }

    Given("type er OBO") {
        When("oid finnes og idtyp ikke er app") {
            Then("returnerer OBO") {
                setClaims(OID to "$oid")
                authContext.type shouldBe OBO
            }
        }
    }

    Given("ingen auth i security context") {
        Then("returnerer UNAUTHENTICATED") {
            authContext.type shouldBe UNAUTHENTICATED
        }
    }

    Given("navIdent") {
        When("NAVident finnes") {
            Then("returnerer navIdent") {
                setClaims(NAVIDENT to "Z999999")
                authContext.navIdent shouldBe "Z999999"
            }
        }

        When("NAVident mangler") {
            Then("returnerer null") {
                authContext.navIdent shouldBe null
            }
        }
    }

    Given("system-claims") {
        When("azp_name finnes") {
            Then("brukes for system, cluster, systemNavn og systemAndNs") {
                setClaims(AZP_NAME to "dev-gcp:team:app")
                authContext.system shouldBe "dev-gcp:team:app"
                authContext.systemNavn shouldBe "app"
                authContext.systemAndNs shouldBe "team:app"
                authContext.clusterAndSystem shouldBe "app:dev-gcp"
            }
        }

        When("azp_name mangler") {
            Then("returnerer utilgjengelig defaults") {
                authContext.system shouldBe UTILGJENGELIG
                authContext.systemNavn shouldBe UTILGJENGELIG
                authContext.systemAndNs shouldBe ""
                authContext.clusterAndSystem shouldBe UTILGJENGELIG
            }
        }
    }

    Given("globaleGruppeIds") {
        When("groups inneholder gyldige UUIDs") {
            Then("returnerer set av UUIDs") {
                val gruppeId = UUID.randomUUID()
                setClaims("groups" to listOf(gruppeId.toString()))
                authContext.globaleGruppeIds shouldBe setOf(gruppeId)
            }
        }

        When("groups mangler eller er ugyldige") {
            Then("returnerer tomt sett") {
                authContext.globaleGruppeIds.shouldBeEmpty()
                setClaims("groups" to listOf("ikke-en-uuid"))
                authContext.globaleGruppeIds.shouldBeEmpty()
            }
        }
    }
})
