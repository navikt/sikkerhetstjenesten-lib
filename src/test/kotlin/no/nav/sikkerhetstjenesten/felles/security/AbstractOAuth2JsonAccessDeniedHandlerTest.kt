package no.nav.sikkerhetstjenesten.felles.security

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.access.AccessDeniedException
import tools.jackson.databind.json.JsonMapper
import java.net.URI

class AbstractOAuth2JsonAccessDeniedHandlerTest : FunSpec({
    test("writes a forbidden problem detail and invokes preHandle") {
        val mapper = JsonMapper.builder().build()
        val typeUri = URI.create("https://example.test/access-denied")
        val handler = TestAccessDeniedHandler(mapper, AuthContext(), typeUri)
        val response = MockHttpServletResponse()

        handler.handle(
            MockHttpServletRequest(),
            response,
            AccessDeniedException("Not authorized"),
        )

        val problemDetail = mapper.readTree(response.contentAsString)
        response.status shouldBe 403
        response.contentType shouldBe "application/problem+json"
        problemDetail["status"].asInt() shouldBe 403
        problemDetail["detail"].asString() shouldBe "Not authorized"
        problemDetail["type"].asString() shouldBe typeUri.toString()
        handler.preHandleCalled shouldBe true
    }
}) {
    private class TestAccessDeniedHandler(
        mapper: JsonMapper,
        authContext: AuthContext,
        typeUri: URI,
    ) : AbstractOAuth2JsonAccessDeniedHandler(mapper, authContext, typeUri) {
        var preHandleCalled = false

        override fun preHandle(req: jakarta.servlet.http.HttpServletRequest, res: jakarta.servlet.http.HttpServletResponse) {
            preHandleCalled = true
        }
    }
}
