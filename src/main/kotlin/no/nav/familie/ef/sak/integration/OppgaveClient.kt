package no.nav.familie.ef.sak.integration

import no.nav.familie.ef.sak.api.Feil
import no.nav.familie.ef.sak.config.IntegrasjonerConfig
import no.nav.familie.ef.sak.integration.dto.familie.Arbeidsfordelingsenhet
import no.nav.familie.ef.sak.integration.dto.familie.EgenAnsattRequest
import no.nav.familie.ef.sak.integration.dto.familie.EgenAnsattResponse
import no.nav.familie.ef.sak.integration.dto.familie.Tilgang
import no.nav.familie.ef.sak.integration.dto.personopplysning.PersonhistorikkInfo
import no.nav.familie.ef.sak.integration.dto.personopplysning.Personinfo
import no.nav.familie.http.client.AbstractPingableRestClient
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.kodeverk.KodeverkDto
import no.nav.familie.kontrakter.felles.medlemskap.Medlemskapsinfo
import no.nav.familie.kontrakter.felles.personopplysning.Ident
import no.nav.familie.log.NavHttpHeaders
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestOperations
import java.net.URI
import java.time.LocalDate

@Component
class OppgaveClient(@Qualifier("jwtBearer") restOperations: RestOperations,
                    private val integrasjonerConfig: IntegrasjonerConfig)
    : AbstractPingableRestClient(restOperations, "oppgave") {

    override val pingUri: URI = integrasjonerConfig.pingUri


    private fun HttpHeaders.medPersonident(personident: String): HttpHeaders {
        this.add(NavHttpHeaders.NAV_PERSONIDENT.asString(), personident)
        return this
    }
}
