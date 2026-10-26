package no.nav.sikkerhetstjenesten.felles.graphql

import org.slf4j.LoggerFactory.getLogger
import org.springframework.graphql.client.ClientGraphQlRequest
import org.springframework.graphql.client.SyncGraphQlClientInterceptor
import org.springframework.graphql.client.SyncGraphQlClientInterceptor.Chain

class GraphQLLoggingInterceptor : SyncGraphQlClientInterceptor {
    private val log = getLogger(javaClass)

    override fun intercept(req: ClientGraphQlRequest, chain: Chain) =
        chain.next(req).also {
            log.trace("Eksekverte {} med variabler {}", req.document, req.variables)
        }
}