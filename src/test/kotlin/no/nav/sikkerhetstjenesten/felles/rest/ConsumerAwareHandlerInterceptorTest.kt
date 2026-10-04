package no.nav.sikkerhetstjenesten.felles.rest

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.mockk
import no.nav.sikkerhetstjenesten.felles.rest.ConsumerAwareHandlerInterceptor.Companion.CONSUMER_ID
import no.nav.sikkerhetstjenesten.felles.rest.ConsumerAwareHandlerInterceptor.Companion.USER_ID
import no.nav.sikkerhetstjenesten.felles.security.AuthContext
import org.slf4j.MDC
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

class ConsumerAwareHandlerInterceptorTest : BehaviorSpec({

    val authContext = mockk<AuthContext>()
    lateinit var registry: SimpleMeterRegistry
    lateinit var interceptor: ConsumerAwareHandlerInterceptor

    beforeEach {
        registry = SimpleMeterRegistry()
        interceptor = ConsumerAwareHandlerInterceptor(authContext, registry)
        every { authContext.systemAndNs } returns "tilgangsmaskin:my-app"
        every { authContext.systemNavn } returns "my-app"
        every { authContext.navIdent } returns null
        MDC.clear()
    }

    Given("preHandle") {
        When("request mottas") {
            Then("legges consumerId i MDC") {
                interceptor.preHandle(MockHttpServletRequest(), MockHttpServletResponse(), Any())
                MDC.get(CONSUMER_ID) shouldBe "tilgangsmaskin:my-app"
            }
            Then("inkrementeres http_requests_by_remote_system med riktig remote_system-tagg") {
                interceptor.preHandle(MockHttpServletRequest(), MockHttpServletResponse(), Any())
                registry.get("http_requests_by_remote_system").tag("remote_system", "my-app").counter().count() shouldBe 1.0
            }
            Then("returneres true") {
                interceptor.preHandle(MockHttpServletRequest(), MockHttpServletResponse(), Any()) shouldBe true
            }
        }

        When("token har ansattId (OBO)") {
            Then("settes userId fra tokenet") {
                every { authContext.navIdent } returns "Z999999"
                interceptor.preHandle(MockHttpServletRequest(), MockHttpServletResponse(), Any())
                MDC.get(USER_ID) shouldBe "Z999999"
            }
        }

        When("token mangler ansattId (CCF)") {
            Then("settes ikke userId i MDC av interceptoren — controlleren gjør det nedstrøms") {
                interceptor.preHandle(MockHttpServletRequest(), MockHttpServletResponse(), Any())
                MDC.get(USER_ID).shouldBeNull()
            }
        }
    }

    Given("afterCompletion") {
        When("request er ferdig") {
            Then("ryddes både consumerId og userId fra MDC") {
                every { authContext.navIdent } returns "Z999999"
                val request = MockHttpServletRequest()
                val response = MockHttpServletResponse()
                interceptor.preHandle(request, response, Any())
                MDC.get(CONSUMER_ID) shouldBe "tilgangsmaskin:my-app"
                MDC.get(USER_ID) shouldBe "Z999999"

                interceptor.afterCompletion(request, response, Any(), null)

                MDC.get(CONSUMER_ID).shouldBeNull()
                MDC.get(USER_ID).shouldBeNull()
            }
        }
    }
})
