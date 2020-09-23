package no.nav.familie.ef.sak.integration

import no.nav.familie.ef.sak.config.IntegrasjonerConfig
import no.nav.familie.ef.sak.exception.IntegrasjonException
import no.nav.familie.ef.sak.util.RessursUtils.assertGenerelleSuksessKriterier
import no.nav.familie.ef.sak.util.medContentTypeJsonUTF8
import no.nav.familie.http.client.AbstractPingableRestClient
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.oppgave.*
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class OppgaveClient(@Qualifier("jwtBearer") restOperations: RestOperations,
                    private val integrasjonerConfig: IntegrasjonerConfig)
    : AbstractPingableRestClient(restOperations, "oppgave") {

    override val pingUri: URI = integrasjonerConfig.pingUri
    private val oppgaveUri: URI = integrasjonerConfig.oppgaveUri

    fun opprettOppgave(opprettOppgave: OpprettOppgaveRequest): Long {
        val uri = URI.create("$oppgaveUri/v2")

        val respons = postForEntity<Ressurs<OppgaveResponse>>(uri, opprettOppgave, HttpHeaders().medContentTypeJsonUTF8())
        assertGenerelleSuksessKriterier(respons)
        return respons.data?.oppgaveId ?: throw IntegrasjonException("Respons fra opprettOppgave mangler oppgaveId.",
                                                                                 null,
                                                                                 uri,
                                                                                 opprettOppgave.ident?.ident)
    }

    fun finnOppgaveMedId(oppgaveId: Long): Oppgave {
        val uri = URI.create("$oppgaveUri/$oppgaveId")

        val respons = getForEntity<Ressurs<Oppgave>>(uri)
        assertGenerelleSuksessKriterier(respons)
        return respons.data ?: throw IntegrasjonException("Respons fra finnOppgaveMedId mangler data.",
                                                          null,
                                                          uri,
                                                          null)
    }

    fun hentOppgaver(finnOppgaveRequest: FinnOppgaveRequest): FinnOppgaveResponseDto {
        val uri = URI.create("$oppgaveUri/v4")

        val respons =
                postForEntity<Ressurs<FinnOppgaveResponseDto>>(uri, finnOppgaveRequest, HttpHeaders().medContentTypeJsonUTF8())
        assertGenerelleSuksessKriterier(respons)
        return respons.data ?: throw IntegrasjonException("Respons fra hentOppgaver mangler data.", null, uri, null)
    }

    fun fordelOppgave(oppgaveId: Long, saksbehandler: String?): String {
        val baseUri = URI.create("$oppgaveUri/$oppgaveId/fordel")
        val uri = if (saksbehandler == null)
            baseUri
        else
            UriComponentsBuilder.fromUri(baseUri).queryParam("saksbehandler", saksbehandler).build().toUri()

        return Result.runCatching {
            postForEntity<Ressurs<OppgaveResponse>>(uri, HttpHeaders().medContentTypeJsonUTF8())
        }.fold(
                onSuccess = {
                    assertGenerelleSuksessKriterier(it)

                    it.data?.oppgaveId?.toString() ?: throw IntegrasjonException("Response fra oppgave mangler oppgaveId.",
                                                                                 null,
                                                                                 uri)
                },
                onFailure = {
                    val message = if (it is RestClientResponseException) it.responseBodyAsString else ""
                    throw IntegrasjonException("Kall mot integrasjon feilet ved fordel oppgave. response=$message",
                                               it,
                                               uri
                    )
                }
        )
    }

    fun ferdigstillOppgave(oppgaveId: Long) {
        val uri = URI.create("$oppgaveUri/$oppgaveId/ferdigstill")

        Result.runCatching {
            val response = patchForEntity<Ressurs<OppgaveResponse>>(uri, "")
            assertGenerelleSuksessKriterier(response)
        }.onFailure {
            throw IntegrasjonException("Kan ikke ferdigstille $oppgaveId. response=${responseBody(it)}", it, uri)
        }
    }

    private fun responseBody(it: Throwable): String? {
        return if (it is RestClientResponseException) it.responseBodyAsString else ""
    }

}
