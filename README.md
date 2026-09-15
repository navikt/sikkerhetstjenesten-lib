# NAV Kotlin Felles

This repository contains the reusable infrastructure classes identified as candidates for extraction from the `entraproxy` and `populasjonstilgangskontroll` projects.

Included candidates:
- `no.nav.felles.cache.CacheOperations`
- `no.nav.felles.cache.CacheNøkkel`
- `no.nav.felles.cache.CacheNøkkelConfig`
- `no.nav.felles.cache.ResilientValkeySerializer`
- `no.nav.felles.utils.cluster.ClusterConstants`
- `no.nav.felles.utils.cluster.ClusterUtils`
- `no.nav.felles.utils.extensions.TimeExtensions`
- `no.nav.felles.rest.Pingable`
- `no.nav.felles.rest.PingableHealthIndicator`
- `no.nav.felles.security.OAuth2JsonAuthenticationEntryPoint`
- `no.nav.felles.security.OAuth2JsonAccessDeniedHandler`
- `no.nav.felles.notifikasjon.LogbookPrettyPrintingFormatter`

This is intentionally framework-facing but avoids app-specific business logic or claims handling.
