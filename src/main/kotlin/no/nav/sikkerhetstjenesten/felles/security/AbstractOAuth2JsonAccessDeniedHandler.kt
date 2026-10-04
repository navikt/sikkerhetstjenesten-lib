package no.nav.sikkerhetstjenesten.felles.security

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
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
            status = HttpStatus.FORBIDDEN.value()
            contentType = MediaType.APPLICATION_PROBLEM_JSON_VALUE
            mapper.writeValue(
                writer,
                securityProblemDetail(HttpStatus.FORBIDDEN, e.message ?: "Access Denied", typeUri),
            )
        }
    }
}
