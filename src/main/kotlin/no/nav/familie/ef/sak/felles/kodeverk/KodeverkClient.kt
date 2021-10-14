package no.nav.familie.ef.sak.felles.kodeverk

import no.nav.familie.ef.sak.infrastruktur.config.IntegrasjonerConfig
import no.nav.familie.http.client.AbstractPingableRestClient
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.kodeverk.KodeverkDto
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import java.net.URI

@Component
class KodeverkClient(@Qualifier("azure") restOperations: RestOperations,
                     private val integrasjonerConfig: IntegrasjonerConfig)
    : AbstractPingableRestClient(restOperations, "kodeverk") {

    override val pingUri: URI = integrasjonerConfig.pingUri
    val logger: Logger = LoggerFactory.getLogger(this::class.java)


    fun hentKodeverkLandkoder(): KodeverkDto {
        return getForEntity<Ressurs<KodeverkDto>>(integrasjonerConfig.kodeverkLandkoderUri).data!!
    }

    fun hentKodeverkPoststed(): KodeverkDto {
        return getForEntity<Ressurs<KodeverkDto>>(integrasjonerConfig.kodeverkPoststedUri).data!!
    }
}
