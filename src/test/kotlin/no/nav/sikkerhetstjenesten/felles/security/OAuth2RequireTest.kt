package no.nav.sikkerhetstjenesten.felles.security

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.context.SecurityContextHolder.getContext
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import java.time.Instant

class OAuth2RequireTest : BehaviorSpec({
    lateinit var context: AnnotationConfigApplicationContext

    fun autentiser(vararg autoriteter: String) {
        val now = Instant.now()
        val jwt = Jwt.withTokenValue("token")
            .header("alg", "none")
            .subject("bruker")
            .issuedAt(now)
            .expiresAt(now.plusSeconds(3600))
            .build()
        getContext().authentication = JwtAuthenticationToken(jwt, autoriteter.map(::SimpleGrantedAuthority))
    }

    beforeEach {
        SecurityContextHolder.clearContext()
        context = AnnotationConfigApplicationContext(MethodSecurityTestConfig::class.java)
    }

    afterEach {
        SecurityContextHolder.clearContext()
        context.close()
    }

    val targets = listOf(
        SecuredTarget("OBO på metodenivå", OBO_AUTHORITY, CCF_AUTHORITY) {
            it.getBean(MethodSecuredService::class.java).obo()
        },
        SecuredTarget("CCF på metodenivå", CCF_AUTHORITY, OBO_AUTHORITY) {
            it.getBean(MethodSecuredService::class.java).ccf()
        },
        SecuredTarget("OBO på klassenivå", OBO_AUTHORITY, CCF_AUTHORITY) {
            it.getBean(OboSecuredService::class.java).call()
        },
        SecuredTarget("CCF på klassenivå", CCF_AUTHORITY, OBO_AUTHORITY) {
            it.getBean(CcfSecuredService::class.java).call()
        },
    )

    targets.forEach { target ->
        Given(target.name) {
            Then("tilgang gis med riktig autoritet") {
                autentiser(target.requiredAuthority)

                target.invoke(context) shouldBe "allowed"
            }

            Then("tilgang nektes med autoriteten til den andre tokentypen") {
                autentiser(target.otherAuthority)

                shouldThrow<AccessDeniedException> { target.invoke(context) }
            }

            Then("tilgang nektes for en autentisert bruker uten autoriteter") {
                autentiser()

                shouldThrow<AccessDeniedException> { target.invoke(context) }
            }

            Then("tilgang nektes med en uvedkommende autoritet") {
                autentiser("ROLE_USER")

                shouldThrow<AccessDeniedException> { target.invoke(context) }
            }

            Then("tilgang nektes for en anonym bruker") {
                getContext().authentication =
                    AnonymousAuthenticationToken(
                        "test", "anonymousUser", listOf(SimpleGrantedAuthority("ROLE_ANONYMOUS"))
                    )

                shouldThrow<AccessDeniedException> { target.invoke(context) }
            }

            Then("manglende autentisering avvises") {
                shouldThrow<AuthenticationCredentialsNotFoundException> { target.invoke(context) }
            }
        }
    }
}) {
    private data class SecuredTarget(
        val name: String,
        val requiredAuthority: String,
        val otherAuthority: String,
        val invoke: (AnnotationConfigApplicationContext) -> String,
    )

    @Configuration(proxyBeanMethods = false)
    @EnableMethodSecurity
    class MethodSecurityTestConfig {
        @Bean
        fun methodSecuredService() = MethodSecuredService()

        @Bean
        fun oboSecuredService() = OboSecuredService()

        @Bean
        fun ccfSecuredService() = CcfSecuredService()
    }

    open class MethodSecuredService {
        @OAuth2RequireOBO
        open fun obo() = "allowed"

        @OAuth2RequireCCF
        open fun ccf() = "allowed"
    }

    @OAuth2RequireOBO
    open class OboSecuredService {
        open fun call() = "allowed"
    }

    @OAuth2RequireCCF
    open class CcfSecuredService {
        open fun call() = "allowed"
    }
}
