package no.nav.felles.utils.cluster

object ClusterConstants {
    const val LOCAL = "local"
    const val GCP = "gcp"
    const val TEST = "test"
    const val DEV = "dev"
    const val PROD = "prod"
    const val DEV_GCP = "$DEV-$GCP"
    const val PROD_GCP = "$PROD-$GCP"
    const val NAIS_CLUSTER_NAME = "NAIS_CLUSTER_NAME"
}
