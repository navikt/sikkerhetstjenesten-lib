package no.nav.sikkerhetstjenesten.felles.utils.cluster

import no.nav.sikkerhetstjenesten.felles.utils.cluster.ClusterConstants.DEV
import no.nav.sikkerhetstjenesten.felles.utils.cluster.ClusterConstants.DEV_GCP
import no.nav.sikkerhetstjenesten.felles.utils.cluster.ClusterConstants.GCP
import no.nav.sikkerhetstjenesten.felles.utils.cluster.ClusterConstants.LOCAL
import no.nav.sikkerhetstjenesten.felles.utils.cluster.ClusterConstants.NAIS_CLUSTER_NAME
import no.nav.sikkerhetstjenesten.felles.utils.cluster.ClusterConstants.PROD
import no.nav.sikkerhetstjenesten.felles.utils.cluster.ClusterConstants.PROD_GCP
import no.nav.sikkerhetstjenesten.felles.utils.cluster.ClusterConstants.TEST
import java.lang.System.getenv
import java.lang.System.setProperty

enum class ClusterUtils(val clusterName: String) {
    TEST_CLUSTER(TEST),
    LOCAL_CLUSTER(LOCAL),
    DEV_GCP_CLUSTER(DEV_GCP),
    PROD_GCP_CLUSTER(PROD_GCP);

    companion object {
        val current = (getenv(NAIS_CLUSTER_NAME) ?: LOCAL)
            .let { env -> entries.first { it.clusterName == env } }

        val isProd = current == PROD_GCP_CLUSTER
        val isDev = current == DEV_GCP_CLUSTER
        val isLocalOrTest = !isDev && !isProd
        val profiler = when (current) {
            TEST_CLUSTER, LOCAL_CLUSTER ->
                arrayOf(current.clusterName).also {
                    setProperty(NAIS_CLUSTER_NAME, current.clusterName)
                }

            DEV_GCP_CLUSTER -> arrayOf(DEV, DEV_GCP, GCP)
            PROD_GCP_CLUSTER -> arrayOf(PROD, PROD_GCP, GCP)
        }
    }

    internal fun profilerFor(cluster: ClusterUtils) =
        when (cluster) {
            TEST_CLUSTER,
            LOCAL_CLUSTER ->
                arrayOf(cluster.clusterName).also {
                    setProperty(NAIS_CLUSTER_NAME, cluster.clusterName)
                }
            DEV_GCP_CLUSTER -> arrayOf(DEV, DEV_GCP, GCP)
            PROD_GCP_CLUSTER -> arrayOf(PROD, PROD_GCP, GCP)
        }
}
