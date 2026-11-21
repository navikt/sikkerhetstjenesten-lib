
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
- `no.nav.sikkerhetstjenesten.felles.rest.RestHeaderAddingRequestInterceptor`
- `no.nav.felles.security.OAuth2JsonAuthenticationEntryPoint`
- `no.nav.felles.security.OAuth2JsonAccessDeniedHandler`
- `no.nav.felles.notifikasjon.LogbookPrettyPrintingFormatter`

This is intentionally framework-facing but avoids app-specific business logic or claims handling.

## Once-only notifications

Auto-configured `MessagePublisher` beans support `infoOnce`, `warnOnce`, and `errorOnce`:

```kotlin
publisher.infoOnce(
    key = "gruppevalidering:$image",
    ttl = Duration.ofDays(30),
    header = ":rocket:",
    msg = "Validert grupper ved oppstart",
)
```

Set `spring.application.name`. Markers are scoped by application, cluster, notification
level, and caller-supplied key. In GCP they are claimed atomically in Redis with the
requested TTL; no cache registration is needed. Outside GCP they use an expiring
in-memory cache shared by the auto-configured publishers in that application context.
Normal `info`, `warn`, `error`, and `publish` calls are unchanged.

This guarantees at most one publishing attempt per marker lifetime, not delivery.
Failures retain the marker until expiry, and cache eviction or clearing can allow
another attempt sooner. Manually created or custom publishers must be wrapped in
`DeduplicatingMessagePublisher` to support the once-only methods.

## Reusable GitHub Actions workflows

Shared CI/CD workflows used by `entra-proxy` and `populasjonstilgangskontroll`. Reference them pinned to a commit SHA:

- `.github/workflows/reusable-gradle-build.yml` – Gradle build, detekt, tests, SBOM and Docker image push (`team` required; optional Node/Playwright and test report upload). Outputs `image` and `digest`.
- `.github/workflows/reusable-nais-deploy.yml` – deploys `image` to the clusters in `clusters` (JSON list) using the environment `<cluster>:<team>`.
- `.github/workflows/reusable-zizmor.yml` – zizmor workflow security scan (`advanced-security: false` for private repos without GHAS).
- `.github/workflows/reusable-pages.yml` – publishes to GitHub Pages: optional Jekyll site, detekt report, JaCoCo/test reports and MkDocs site, using artifacts from `reusable-gradle-build.yml` (enable `upload-test-reports` there for test reports).

```yaml
jobs:
  build:
    permissions:
      checks: write
      contents: read
      id-token: write
    uses: navikt/sikkerhetstjenesten-lib/.github/workflows/reusable-gradle-build.yml@<sha>
    with:
      team: my-team
  deploy:
    needs: build
    permissions:
      contents: read
      id-token: write
    uses: navikt/sikkerhetstjenesten-lib/.github/workflows/reusable-nais-deploy.yml@<sha>
    with:
      team: my-team
      clusters: '["dev-gcp"]'
      image: ${{ needs.build.outputs.image }}
```

Changes to `reusable-*.yml` do not trigger a library release.
