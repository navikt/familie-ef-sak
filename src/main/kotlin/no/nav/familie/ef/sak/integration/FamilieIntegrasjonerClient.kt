package no.nav.familie.ef.sak.integration

import no.nav.familie.ef.sak.config.IntegrasjonerConfig
import no.nav.familie.ef.sak.integration.dto.Tilgang
import no.nav.familie.ef.sak.integration.dto.personopplysning.PersonhistorikkInfo
import no.nav.familie.ef.sak.integration.dto.personopplysning.Personinfo
import no.nav.familie.http.client.AbstractPingableRestClient
import no.nav.familie.kontrakter.felles.Ressurs
import org.springframework.http.HttpHeaders
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

    fun hentPersonopplysninger(ident: String): Personinfo {
        return getForEntity<Ressurs<Personinfo>>(integrasjonerConfig.personopplysningerUri, personIdentHeader(ident)).data!!
    }

    fun hentPersonhistorikk(ident: String): PersonhistorikkInfo {
        return getForEntity<Ressurs<PersonhistorikkInfo>>(integrasjonerConfig.personopplysningerUri,
                                                          personIdentHeader(ident)).data!!
    }


    private fun personIdentHeader(ident: String) = HttpHeaders().apply { add("Nav-Personident", ident) }

}
