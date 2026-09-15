package no.nav.felles.security

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE
import org.springframework.http.ProblemDetail.forStatusAndDetail
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.web.access.AccessDeniedHandler
import java.net.URI

class OAuth2JsonAccessDeniedHandler(
    private val mapper: ObjectMapper,
    private val typeUri: URI = URI.create("https://example.com/problems/forbidden"),
) : AccessDeniedHandler {

    override fun handle(req: HttpServletRequest, res: HttpServletResponse, e: AccessDeniedException) {
        res.status = HttpStatus.FORBIDDEN.value()
        res.contentType = APPLICATION_PROBLEM_JSON_VALUE
        mapper.writeValue(res.writer, problemDetail(HttpStatus.FORBIDDEN, e.message ?: "Access denied"))
    }

    private fun problemDetail(status: HttpStatus, detail: String) =
        forStatusAndDetail(status, detail).apply {
            type = typeUri
            title = status.reasonPhrase
        }
}
