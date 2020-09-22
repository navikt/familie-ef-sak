package no.nav.familie.ef.sak.integration

import no.nav.familie.ef.sak.config.IntegrasjonerConfig
import no.nav.familie.ef.sak.exception.IntegrasjonException
import no.nav.familie.ef.sak.util.RessursUtils
import no.nav.familie.ef.sak.util.medContentTypeJsonUTF8
import no.nav.familie.http.client.AbstractPingableRestClient
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.oppgave.OppgaveResponse
import no.nav.familie.kontrakter.felles.oppgave.OpprettOppgaveRequest
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.RestOperations
import java.net.URI

@Component
class OppgaveClient(@Qualifier("jwtBearer") restOperations: RestOperations,
                    private val integrasjonerConfig: IntegrasjonerConfig)
    : AbstractPingableRestClient(restOperations, "oppgave") {

    override val pingUri: URI = integrasjonerConfig.pingUri
    val oppgaveUri: URI = integrasjonerConfig.oppgaveUri

    fun opprettOppgave(opprettOppgave: OpprettOppgaveRequest): String {
        val uri = URI.create("$oppgaveUri/v2")

        val respons = postForEntity<Ressurs<OppgaveResponse>>(uri, opprettOppgave, HttpHeaders().medContentTypeJsonUTF8())
        RessursUtils.assertGenerelleSuksessKriterier(respons)
        return respons.data?.oppgaveId?.toString() ?: throw IntegrasjonException("Response fra opprettOppgave mangler oppgaveId.",
                                                                                       null,
                                                                                       uri,
                                                                                       opprettOppgave.ident?.ident)

    }
}
