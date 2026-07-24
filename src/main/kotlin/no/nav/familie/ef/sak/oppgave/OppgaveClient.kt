package no.nav.familie.ef.sak.oppgave

import no.nav.familie.ef.sak.felles.util.medContentTypeJsonUTF8
import no.nav.familie.ef.sak.infrastruktur.config.IntegrasjonerConfig
import no.nav.familie.ef.sak.infrastruktur.exception.ApiFeil
import no.nav.familie.ef.sak.infrastruktur.exception.IntegrasjonException
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.Toggle
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.getDataOrThrow
import no.nav.familie.kontrakter.felles.oppgave.FinnMappeResponseDto
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveRequest
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveResponseDto
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.OppgaveResponse
import no.nav.familie.kontrakter.felles.oppgave.OpprettOppgaveRequest
import no.nav.familie.kontrakter.felles.saksbehandler.Saksbehandler
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class OppgaveClient(
    @Qualifier("integrasjonerRestClient") private val restClient: RestClient,
    integrasjonerConfig: IntegrasjonerConfig,
    private val featureToggleService: FeatureToggleService,
) {
    private val oppgaveUri: URI = integrasjonerConfig.oppgaveUri
    private val saksbehandlerUri: URI = integrasjonerConfig.saksbehandlerUri

    fun opprettOppgave(opprettOppgave: OpprettOppgaveRequest): Long {
        val uri = URI.create("$oppgaveUri/opprett")

        val respons =
            restClient
                .post()
                .uri(uri)
                .headers { it.addAll(HttpHeaders().medContentTypeJsonUTF8()) }
                .body(opprettOppgave)
                .retrieve()
                .body<Ressurs<OppgaveResponse>>()!!
        return pakkUtRespons(respons, uri, "opprettOppgave").oppgaveId
    }

    fun finnOppgaveMedId(oppgaveId: Long): Oppgave {
        kastApiFeilDersomUtviklerMedVeilederrolle()
        val uri = URI.create("$oppgaveUri/$oppgaveId")

        val respons =
            restClient
                .get()
                .uri(uri)
                .retrieve()
                .body<Ressurs<Oppgave>>()!!
        return pakkUtRespons(respons, uri, "finnOppgaveMedId")
    }

    fun hentOppgaver(finnOppgaveRequest: FinnOppgaveRequest): FinnOppgaveResponseDto {
        kastApiFeilDersomUtviklerMedVeilederrolle()
        val uri = URI.create("$oppgaveUri/v4")

        val respons =
            restClient
                .post()
                .uri(uri)
                .headers { it.addAll(HttpHeaders().medContentTypeJsonUTF8()) }
                .body(finnOppgaveRequest)
                .retrieve()
                .body<Ressurs<FinnOppgaveResponseDto>>()!!
        return pakkUtRespons(respons, uri, "hentOppgaver")
    }

    fun hentSaksbehandlerInfo(navIdent: String): Saksbehandler {
        val uri = URI.create("$saksbehandlerUri/$navIdent")

        val respons =
            restClient
                .get()
                .uri(uri)
                .retrieve()
                .body<Ressurs<Saksbehandler>>()!!
        return pakkUtRespons(respons, uri, "hentSaksbehandlerInfo")
    }

    fun fordelOppgave(
        oppgaveId: Long,
        saksbehandler: String?,
        versjon: Int? = null,
        endretAvSaksbehandler: String? = null,
    ): Long {
        var uri = URI.create("$oppgaveUri/$oppgaveId/fordel")

        if (saksbehandler != null) {
            uri =
                UriComponentsBuilder
                    .fromUri(uri)
                    .queryParam("saksbehandler", saksbehandler)
                    .build()
                    .toUri()
        }

        if (versjon != null) {
            uri =
                UriComponentsBuilder
                    .fromUri(uri)
                    .queryParam("versjon", versjon)
                    .build()
                    .toUri()
        }

        if (endretAvSaksbehandler != null) {
            uri =
                UriComponentsBuilder
                    .fromUri(uri)
                    .queryParam("endretAvSaksbehandler", endretAvSaksbehandler)
                    .build()
                    .toUri()
        }

        try {
            val respons =
                restClient
                    .post()
                    .uri(uri)
                    .headers { it.addAll(HttpHeaders().medContentTypeJsonUTF8()) }
                    .retrieve()
                    .body<Ressurs<OppgaveResponse>>()!!
            return pakkUtRespons(respons, uri, "fordelOppgave").oppgaveId
        } catch (e: HttpClientErrorException) {
            if (e.responseBodyAsString.contains("allerede er ferdigstilt")) {
                throw ApiFeil(
                    "Oppgaven med id=$oppgaveId er allerede ferdigstilt. Prøv å hente oppgaver på nytt.",
                    HttpStatus.BAD_REQUEST,
                )
            } else if (e.statusCode == HttpStatus.CONFLICT) {
                throw ApiFeil(
                    "Oppgaven har endret seg siden du sist hentet oppgaver. For å kunne gjøre endringer må du hente oppgaver på nytt.",
                    HttpStatus.CONFLICT,
                )
            }
            throw e
        }
    }

    fun ferdigstillOppgave(oppgaveId: Long) {
        val uri = URI.create("$oppgaveUri/$oppgaveId/ferdigstill")
        val respons =
            restClient
                .patch()
                .uri(uri)
                .body("")
                .retrieve()
                .body<Ressurs<OppgaveResponse>>()!!
        pakkUtRespons(respons, uri, "ferdigstillOppgave")
    }

    fun oppdaterOppgave(oppgave: Oppgave): Long {
        try {
            val oppgaveId = oppgave.id ?: error("Oppgave mangler id")
            val response =
                restClient
                    .patch()
                    .uri(URI.create("$oppgaveUri/$oppgaveId/oppdater"))
                    .headers { it.addAll(HttpHeaders().medContentTypeJsonUTF8()) }
                    .body(oppgave)
                    .retrieve()
                    .body<Ressurs<OppgaveResponse>>()!!
            return response.getDataOrThrow().oppgaveId
        } catch (e: HttpClientErrorException) {
            if (e.statusCode == HttpStatus.CONFLICT) {
                throw ApiFeil(
                    "Oppgaven har endret seg siden du sist hentet oppgaver. For å kunne gjøre endringer må du laste inn siden på nytt",
                    HttpStatus.CONFLICT,
                )
            }
            throw e
        }
    }

    fun finnMapper(
        enhetsnummer: String,
        limit: Int,
    ): FinnMappeResponseDto {
        val uri =
            UriComponentsBuilder
                .fromUri(oppgaveUri)
                .pathSegment("mappe", "sok")
                .queryParam("enhetsnr", enhetsnummer)
                .queryParam("limit", limit)
                .build()
                .toUri()
        val respons =
            restClient
                .get()
                .uri(uri)
                .retrieve()
                .body<Ressurs<FinnMappeResponseDto>>()!!
        return pakkUtRespons(respons, uri, "finnMappe")
    }

    private fun kastApiFeilDersomUtviklerMedVeilederrolle() {
        if (featureToggleService.isEnabled(Toggle.UTVIKLER_MED_VEILEDERRROLLE)) {
            throw ApiFeil(
                "Kan ikke hente ut oppgaver som utvikler med veilederrolle. Kontakt teamet dersom du har saksbehandlerrolle.",
                HttpStatus.FORBIDDEN,
            )
        }
    }

    private fun <T> pakkUtRespons(
        respons: Ressurs<T>,
        uri: URI?,
        metode: String,
    ): T {
        val data = respons.data
        if (respons.status == Ressurs.Status.SUKSESS && data != null) {
            return data
        } else if (respons.status == Ressurs.Status.SUKSESS) {
            throw IntegrasjonException("Ressurs har status suksess, men mangler data")
        } else {
            throw IntegrasjonException(
                "Respons fra $metode feilet med status=${respons.status} melding=${respons.melding}",
                null,
                uri,
                data,
            )
        }
    }
}
