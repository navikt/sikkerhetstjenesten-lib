package no.nav.sikkerhetstjenesten.felles.graphql

import no.nav.sikkerhetstjenesten.felles.NoCoverageAnalysis
import no.nav.sikkerhetstjenesten.felles.rest.IrrecoverableRestException
import no.nav.sikkerhetstjenesten.felles.rest.RestConfig
import org.slf4j.LoggerFactory.getLogger
import org.springframework.core.ParameterizedTypeReference
import org.springframework.core.ResolvableType
import org.springframework.graphql.client.GraphQlClient
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR

abstract class AbstractSyncGraphQLClientAdapter(
    protected val cfg: RestConfig,
    protected val client: GraphQlClient,
    private val errorHandler: GraphQLErrorHandler = GraphQLErrorHandler()) {

    protected val log = getLogger(javaClass)

    protected inline fun <reified T : Any> query(query: Pair<String, String>, vars: Map<String, String>): T? =
        query(query, vars, T::class.java)

    protected inline fun <reified T : Any> queryList(query: Pair<String, String>, vars: Map<String, String>): List<T>? =
        queryList(query, vars, T::class.java)

    protected inline fun <reified T : Any> queryRequired(query: Pair<String, String>, vars: Map<String, String>): T =
        query<T>(query, vars) ?: throw IrrecoverableRestException(INTERNAL_SERVER_ERROR,
            cfg.baseUri,
            "Fant ikke feltet ${query.second} i responsen")

    protected inline fun <reified T : Any> queryListRequired(query: Pair<String, String>, vars: Map<String, String>)  =
        queryList<T>(query, vars) ?: throw IrrecoverableRestException(INTERNAL_SERVER_ERROR,
            cfg.baseUri,
            "Fant ikke feltet ${query.second} i responsen")

    protected fun <T : Any> query(query: Pair<String, String>, vars: Map<String, String>, type: Class<T>): T? =
        runCatching {
            client
                .documentName(query.first)
                .variables(vars)
                .executeSync()
                .field(query.second)
                .toEntity(type)
        }.getOrElse {
            log.warn("Feil ved oppslag av {}", type.simpleName, it)
            errorHandler.handle(cfg.baseUri, it)
        }

    protected fun <T : Any> queryList(query: Pair<String, String>, vars: Map<String, String>, elementType: Class<T>) =
        runCatching {
            val listType = ResolvableType.forClassWithGenerics(List::class.java, elementType).type
            client
                .documentName(query.first)
                .variables(vars)
                .executeSync()
                .field(query.second)
                .toEntity(ParameterizedTypeReference.forType<List<T>>(listType))
        }.getOrElse {
            log.warn("Feil ved oppslag av liste av {}", elementType.simpleName, it)
            errorHandler.handle(cfg.baseUri, it)
        }

    @NoCoverageAnalysis
    override fun toString() =
        "${javaClass.simpleName} [graphQlClient=$client, cfg=$cfg]"
}
