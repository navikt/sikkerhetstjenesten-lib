package no.nav.sikkerhetstjenesten.felles.security

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.web.access.AccessDeniedHandler
import tools.jackson.databind.json.JsonMapper
import java.net.URI

abstract class AbstractOAuth2JsonAccessDeniedHandler(
    private val mapper: JsonMapper,
    protected val authContext: AuthContext,
    private val typeUri: URI,
) : AccessDeniedHandler {
    protected abstract fun preHandle(req: HttpServletRequest, res: HttpServletResponse)

    override fun handle(req: HttpServletRequest, res: HttpServletResponse, e: AccessDeniedException) {
        preHandle(req, res)
        with(res) {
            status = FORBIDDEN.value()
            contentType = APPLICATION_PROBLEM_JSON_VALUE
            mapper.writeValue(
                writer,
                securityProblemDetail(FORBIDDEN, e.message ?: "Access Denied", typeUri),
            )
        }
    }
}
