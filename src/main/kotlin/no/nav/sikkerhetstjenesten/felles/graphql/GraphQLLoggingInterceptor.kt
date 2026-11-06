package no.nav.sikkerhetstjenesten.felles.graphql

import no.nav.sikkerhetstjenesten.felles.graphql.AbstractSyncGraphQLClientAdapter.Companion.PING_QUERY
import org.slf4j.LoggerFactory.getLogger
import org.springframework.graphql.client.ClientGraphQlRequest
import org.springframework.graphql.client.SyncGraphQlClientInterceptor
import org.springframework.graphql.client.SyncGraphQlClientInterceptor.Chain

class GraphQLLoggingInterceptor : SyncGraphQlClientInterceptor {
    private val log = getLogger(javaClass)

    override fun intercept(req: ClientGraphQlRequest, chain: Chain) =
        chain.next(req).also {
            if (req.document != PING_QUERY) {
               log.trace("Eksekverte {} med variabler {}", req.document, req.variables)
            }
        }
}