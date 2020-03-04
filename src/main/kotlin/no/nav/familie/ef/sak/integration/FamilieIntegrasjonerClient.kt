package no.nav.familie.ef.sak.integration

import no.nav.familie.ef.sak.config.IntegrasjonerConfig
import no.nav.familie.ef.sak.integration.dto.Tilgang
import no.nav.familie.http.client.AbstractPingableRestClient
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import java.net.URI

@Component
class FamilieIntegrasjonerClient(restOperations: RestOperations,
                                 private val integrasjonerConfig: IntegrasjonerConfig)
    : AbstractPingableRestClient(restOperations, "familie.integrasjoner") {

    override val pingUri: URI = integrasjonerConfig.pingUri

    fun sjekkTilgangTilPersoner(identer: List<String>): List<Tilgang> {
        return postForEntity(integrasjonerConfig.tilgangUri, identer)!!

    }
}
