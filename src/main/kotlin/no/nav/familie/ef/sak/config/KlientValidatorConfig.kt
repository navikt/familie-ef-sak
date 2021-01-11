package no.nav.familie.ef.sak.config

import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

/**
 * Config må legges inn med <issuer>/<system_id>, eks sts/srvArena eller azuread/<system_id>
 */
@ConstructorBinding
@ConfigurationProperties(prefix = "tilgang")
class KlientValidatorConfig(val klienter: Map<Klient, String>) {

    init {
        val logger = LoggerFactory.getLogger(KlientValidatorConfig::class.java)
        if (klienter.keys.size != Klient.values().size) {
            logger.error("Savner oppsett av klienter: {}",
                         Klient.values().toMutableSet().also { it.removeAll(klienter.keys) })
        }
    }

    enum class Klient {
        ARENA
    }
}
