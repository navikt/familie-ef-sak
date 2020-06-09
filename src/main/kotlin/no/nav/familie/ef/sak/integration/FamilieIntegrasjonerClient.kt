package no.nav.familie.ef.sak.integration

import no.nav.familie.ef.sak.config.IntegrasjonerConfig
import no.nav.familie.ef.sak.integration.dto.EgenAnsattRequest
import no.nav.familie.ef.sak.integration.dto.EgenAnsattResponse
import no.nav.familie.ef.sak.integration.dto.Tilgang
import no.nav.familie.ef.sak.integration.dto.personopplysning.PersonhistorikkInfo
import no.nav.familie.ef.sak.integration.dto.personopplysning.Personinfo
import no.nav.familie.http.client.AbstractPingableRestClient
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.medlemskap.Medlemskapsinfo
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import java.net.URI
import java.time.LocalDate

@Component
class FamilieIntegrasjonerClient(@Qualifier("azure") restOperations: RestOperations,
                                 private val integrasjonerConfig: IntegrasjonerConfig)
    : AbstractPingableRestClient(restOperations, "familie.integrasjoner") {

    override val pingUri: URI = integrasjonerConfig.pingUri

    fun sjekkTilgangTilPersoner(identer: List<String>): List<Tilgang> {
        return postForEntity(integrasjonerConfig.tilgangUri, identer)
    }

    fun hentMedlemskapsinfo(ident: String): Medlemskapsinfo {
        return getForEntity<Ressurs<Medlemskapsinfo>>(integrasjonerConfig.personopplysningerUri, personIdentHeader(ident)).data!!
    }

    @Deprecated("bruk Pdl-løsning")
    fun hentPersonopplysninger(ident: String): Personinfo {
        return getForEntity<Ressurs<Personinfo>>(integrasjonerConfig.personopplysningerUri, personIdentHeader(ident)).data!!
    }

    @Deprecated("bruk Pdl-løsning")
    fun hentPersonhistorikk(ident: String): PersonhistorikkInfo {

        val uri = integrasjonerConfig.personhistorikkUriBuilder
                .queryParam("fomDato", LocalDate.now().minusYears(5))
                .queryParam("tomDato", LocalDate.now()).build().toUri()

        return getForEntity<Ressurs<PersonhistorikkInfo>>(uri,
                                                          personIdentHeader(ident)).data!!
    }

    fun egenAnsatt(ident: String): Boolean {
        return postForEntity<Ressurs<EgenAnsattResponse>>(integrasjonerConfig.egenAnsattUri,
                                                          EgenAnsattRequest(ident)).data!!.erEgenAnsatt
    }


    private fun personIdentHeader(ident: String) = HttpHeaders().apply { add("Nav-Personident", ident) }

}
