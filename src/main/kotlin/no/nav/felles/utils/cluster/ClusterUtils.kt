package no.nav.felles.utils.cluster

import java.lang.System.getenv
import java.lang.System.setProperty

enum class ClusterUtils(val clusterName: String) {
    TEST_CLUSTER(ClusterConstants.TEST),
    LOCAL_CLUSTER(ClusterConstants.LOCAL),
    DEV_GCP_CLUSTER(ClusterConstants.DEV_GCP),
    PROD_GCP_CLUSTER(ClusterConstants.PROD_GCP);

    companion object {
        val current = (getenv(ClusterConstants.NAIS_CLUSTER_NAME) ?: ClusterConstants.LOCAL)
            .let { env -> entries.first { it.clusterName == env } }

        val isProd = current == PROD_GCP_CLUSTER
        val isDev = current == DEV_GCP_CLUSTER
        val isLocalOrTest = !isDev && !isProd
        val profiler = when (current) {
            TEST_CLUSTER, LOCAL_CLUSTER ->
                arrayOf(current.clusterName).also {
                    setProperty(ClusterConstants.NAIS_CLUSTER_NAME, current.clusterName)
                }

            DEV_GCP_CLUSTER -> arrayOf(ClusterConstants.DEV, ClusterConstants.DEV_GCP, ClusterConstants.GCP)
            PROD_GCP_CLUSTER -> arrayOf(ClusterConstants.PROD, ClusterConstants.PROD_GCP, ClusterConstants.GCP)
        }
    }
}
