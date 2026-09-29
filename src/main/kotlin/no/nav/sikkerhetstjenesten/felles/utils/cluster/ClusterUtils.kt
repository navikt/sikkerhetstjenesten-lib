package no.nav.sikkerhetstjenesten.felles.utils.cluster

import no.nav.sikkerhetstjenesten.felles.utils.cluster.ClusterConstants.DEV_GCP
import no.nav.sikkerhetstjenesten.felles.utils.cluster.ClusterConstants.LOCAL
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
        val current = (getenv(ClusterConstants.NAIS_CLUSTER_NAME) ?: LOCAL)
            .let { env -> entries.first { it.clusterName == env } }

        val isProd = current == PROD_GCP_CLUSTER
        val isDev = current == DEV_GCP_CLUSTER
        val isLocalOrTest = !isDev && !isProd
        val profiler = when (current) {
            TEST_CLUSTER, LOCAL_CLUSTER ->
                arrayOf(current.clusterName).also {
                    setProperty(ClusterConstants.NAIS_CLUSTER_NAME, current.clusterName)
                }

            DEV_GCP_CLUSTER -> arrayOf(ClusterConstants.DEV, DEV_GCP, ClusterConstants.GCP)
            PROD_GCP_CLUSTER -> arrayOf(ClusterConstants.PROD, PROD_GCP, ClusterConstants.GCP)
        }
    }
}
