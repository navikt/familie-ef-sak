package no.nav.familie.ef.sak.opplysninger.personopplysninger

import no.nav.familie.ef.sak.arbeidsfordeling.Arbeidsfordelingsenhet
import no.nav.familie.ef.sak.felles.integration.dto.Tilgang
import no.nav.familie.ef.sak.infrastruktur.config.IntegrasjonerConfig
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.arbeidsfordeling.Enhet
import no.nav.familie.kontrakter.felles.getDataOrThrow
import no.nav.familie.kontrakter.felles.navkontor.NavKontorEnhet
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.kontrakter.felles.personopplysning.Ident
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import org.springframework.web.client.body
import java.net.URI

@Component
class PersonopplysningerIntegrasjonerClient(
    @Qualifier("integrasjonerRestClient") private val restClient: RestClient,
    private val integrasjonerConfig: IntegrasjonerConfig,
) {
    fun sjekkTilgangTilPerson(personIdent: String): Tilgang =
        restClient
            .post()
            .uri(integrasjonerConfig.tilgangPersonUri)
            .header(HEADER_NAV_TEMA, HEADER_NAV_TEMA_ENF)
            .body(listOf(personIdent))
            .retrieve()
            .body<List<Tilgang>>()!!
            .single()

    fun sjekkTilgangTilPersonMedRelasjoner(personIdent: String): Tilgang =
        restClient
            .post()
            .uri(integrasjonerConfig.tilgangRelasjonerUri)
            .header(HEADER_NAV_TEMA, HEADER_NAV_TEMA_ENF)
            .body(PersonIdent(personIdent))
            .retrieve()
            .body<Tilgang>()!!

    fun hentNavEnhetForPersonMedRelasjoner(ident: String): List<Arbeidsfordelingsenhet> {
        val uri = integrasjonerConfig.arbeidsfordelingMedRelasjonerUri
        return hentArbeidsfordelingEnhet(uri, ident)
    }

    fun hentStrengesteAdressebeskyttelseForPersonMedRelasjoner(personIdent: String): ADRESSEBESKYTTELSEGRADERING =
        restClient
            .post()
            .uri(integrasjonerConfig.adressebeskyttelse)
            .header(HEADER_NAV_TEMA, HEADER_NAV_TEMA_ENF)
            .body(PersonIdent(personIdent))
            .retrieve()
            .body<Ressurs<ADRESSEBESKYTTELSEGRADERING>>()!!
            .getDataOrThrow()

    fun hentBehandlendeEnhetForOppfølging(personident: String): Enhet? {
        val response =
            restClient
                .post()
                .uri(integrasjonerConfig.arbeidsfordelingOppfølgingUri)
                .body(Ident(personident))
                .retrieve()
                .body<Ressurs<List<Enhet>>>()!!
        return response.getDataOrThrow().firstOrNull()
    }

    private fun hentArbeidsfordelingEnhet(
        uri: URI,
        ident: String,
    ): List<Arbeidsfordelingsenhet> =
        try {
            val response =
                restClient
                    .post()
                    .uri(uri)
                    .body(PersonIdent(ident))
                    .retrieve()
                    .body<Ressurs<List<Arbeidsfordelingsenhet>>>()!!
            response.data ?: throw Feil("Objektet fra integrasjonstjenesten mot arbeidsfordeling er tomt uri=$uri")
        } catch (e: RestClientException) {
            throw Feil("Kall mot integrasjon feilet ved henting av arbeidsfordelingsenhet uri=$uri", e)
        }

    fun hentNavKontor(ident: String): NavKontorEnhet? {
        val ressurs =
            restClient
                .post()
                .uri(integrasjonerConfig.navKontorUri)
                .body(PersonIdent(ident))
                .retrieve()
                .body<Ressurs<NavKontorEnhet>>()!!
        if (ressurs.status != Ressurs.Status.SUKSESS) {
            error("Henting av nav-kontor feilet status=${ressurs.status} - ${ressurs.melding}")
        }
        return ressurs.data
    }

    companion object {
        const val HEADER_NAV_TEMA = "Nav-Tema"
        const val HEADER_NAV_TEMA_ENF = "ENF"
    }
}
