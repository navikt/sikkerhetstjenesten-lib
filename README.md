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

## Reusable GitHub Actions workflows

Shared CI/CD workflows used by `entra-proxy` and `populasjonstilgangskontroll`. Reference them pinned to a commit SHA:

- `.github/workflows/reusable-gradle-build.yml` – Gradle build, detekt, tests, SBOM and Docker image push (`team` required; optional Node/Playwright and test report upload). Outputs `image` and `digest`.
- `.github/workflows/reusable-nais-deploy.yml` – deploys `image` to the clusters in `clusters` (JSON list) using the environment `<cluster>:<team>`.
- `.github/workflows/reusable-zizmor.yml` – zizmor workflow security scan (`advanced-security: false` for private repos without GHAS).

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
