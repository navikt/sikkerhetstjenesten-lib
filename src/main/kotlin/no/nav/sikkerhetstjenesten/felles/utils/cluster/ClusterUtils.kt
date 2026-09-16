package no.nav.sikkerhetstjenesten.felles.utils.cluster

import java.lang.System.getenv
import java.lang.System.setProperty

enum class ClusterUtils(val clusterName: String) {
    TEST_CLUSTER(_root_ide_package_.no.nav.sikkerhetstjenesten.felles.utils.cluster.ClusterConstants.TEST),
    LOCAL_CLUSTER(_root_ide_package_.no.nav.sikkerhetstjenesten.felles.utils.cluster.ClusterConstants.LOCAL),
    DEV_GCP_CLUSTER(_root_ide_package_.no.nav.sikkerhetstjenesten.felles.utils.cluster.ClusterConstants.DEV_GCP),
    PROD_GCP_CLUSTER(_root_ide_package_.no.nav.sikkerhetstjenesten.felles.utils.cluster.ClusterConstants.PROD_GCP);

    companion object {
        val current = (getenv(_root_ide_package_.no.nav.sikkerhetstjenesten.felles.utils.cluster.ClusterConstants.NAIS_CLUSTER_NAME) ?: _root_ide_package_.no.nav.sikkerhetstjenesten.felles.utils.cluster.ClusterConstants.LOCAL)
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
