package no.nav.sikkerhetstjenesten.felles.notifikasjon.logbook

import no.nav.sikkerhetstjenesten.felles.utils.extensions.TimeExtensions.OSLO
import org.zalando.logbook.HttpRequest
import java.util.Date

fun Map<String, Any>.withTimestampsInCurrentTimezone() =
    mapValues {
        (_, value) -> (value as? Date)?.toInstant()?.atZone(OSLO) ?: value
    }

fun String.shouldIgnoreGraphQlIntrospectionQuery() =
    GRAPHQL_INTROSPECTION_QUERY_BODY.matches(this)

fun HttpRequest.shouldIgnoreGraphQlIntrospectionQuery() =
    bodyAsString.shouldIgnoreGraphQlIntrospectionQuery()

private val GRAPHQL_INTROSPECTION_QUERY_BODY =
    Regex("""(?s)^\s*\{\s*"query"\s*:\s*"\{\s*__typename\s*}"\s*}\s*$""")
