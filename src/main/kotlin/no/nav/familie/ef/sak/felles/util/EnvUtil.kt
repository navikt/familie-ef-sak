package no.nav.familie.ef.sak.felles.util

object EnvUtil {

    fun erIDev() = System.getenv("NAIS_CLUSTER_NAME") == "dev-gcp"
}