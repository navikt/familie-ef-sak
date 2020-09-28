package no.nav.familie.ef.sak.integration

import no.nav.familie.ef.sak.api.Feil
import no.nav.familie.ef.sak.config.IntegrasjonerConfig
import no.nav.familie.ef.sak.integration.dto.familie.Arbeidsfordelingsenhet
import no.nav.familie.ef.sak.integration.dto.familie.EgenAnsattRequest
import no.nav.familie.ef.sak.integration.dto.familie.EgenAnsattResponse
import no.nav.familie.ef.sak.integration.dto.familie.Tilgang
import no.nav.familie.http.client.AbstractPingableRestClient
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.kodeverk.KodeverkDto
import no.nav.familie.kontrakter.felles.medlemskap.Medlemskapsinfo
import no.nav.familie.kontrakter.felles.personopplysning.Ident
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestOperations
import java.net.URI

@Component
class FamilieIntegrasjonerClient(@Qualifier("azure") restOperations: RestOperations,
                                 private val integrasjonerConfig: IntegrasjonerConfig)
    : AbstractPingableRestClient(restOperations, "familie.integrasjoner") {

    override val pingUri: URI = integrasjonerConfig.pingUri

    fun sjekkTilgangTilPersoner(identer: List<String>): List<Tilgang> {
        return postForEntity(integrasjonerConfig.tilgangUri, identer)
    }

    fun hentMedlemskapsinfo(ident: String): Medlemskapsinfo {
        return postForEntity<Ressurs<Medlemskapsinfo>>(integrasjonerConfig.personopplysningerUri, Ident(ident)).data!!
    }

    fun hentKodeverkLandkoder(): KodeverkDto {
        return getForEntity<Ressurs<KodeverkDto>>(integrasjonerConfig.kodeverkLandkoderUri).data!!
    }

    fun hentKodeverkPoststed(): KodeverkDto {
        return getForEntity<Ressurs<KodeverkDto>>(integrasjonerConfig.kodeverkPoststedUri).data!!
    }

    fun hentNavEnhet(ident: String): List<Arbeidsfordelingsenhet> {
        val uri = integrasjonerConfig.arbeidsfordelingUri
        return try {
            val response = postForEntity<Ressurs<List<Arbeidsfordelingsenhet>>>(uri, PersonIdent(ident))
            response.data ?: throw Feil("Objektet fra integrasjonstjenesten mot arbeidsfordeling er tomt uri=$uri")
        } catch (e: RestClientException) {
            throw Feil("Kall mot integrasjon feilet ved henting av arbeidsfordelingsenhet uri=$uri", e)
        }
    }

    fun egenAnsatt(ident: String): Boolean {
        return postForEntity<Ressurs<EgenAnsattResponse>>(integrasjonerConfig.egenAnsattUri,
                                                          EgenAnsattRequest(ident)).data!!.erEgenAnsatt
    }
}
