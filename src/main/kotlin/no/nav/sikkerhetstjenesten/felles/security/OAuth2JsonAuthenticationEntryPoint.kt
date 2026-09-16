package no.nav.sikkerhetstjenesten.felles.security

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE
import org.springframework.http.ProblemDetail.forStatusAndDetail
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import java.net.URI

class OAuth2JsonAuthenticationEntryPoint(
    private val mapper: ObjectMapper,
    private val defaultDetail: String = "Bruker er ikke logget inn. Mangler bearer-token i Authorization-header.",
    private val typeUri: URI = URI.create("https://example.com/problems/unauthorized"),
) : AuthenticationEntryPoint {

    override fun commence(req: HttpServletRequest, res: HttpServletResponse, e: AuthenticationException) {
        res.status = HttpStatus.UNAUTHORIZED.value()
        res.contentType = APPLICATION_PROBLEM_JSON_VALUE
        mapper.writeValue(res.writer, problemDetail(HttpStatus.UNAUTHORIZED, defaultDetail))
    }

    private fun problemDetail(status: HttpStatus, detail: String) =
        forStatusAndDetail(status, detail).apply {
            type = typeUri
            title = status.reasonPhrase
        }
}
