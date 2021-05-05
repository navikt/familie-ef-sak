package no.nav.familie.ef.sak.integration

import no.nav.familie.ef.sak.config.PdlConfig
import no.nav.familie.ef.sak.integration.dto.pdl.Paging
import no.nav.familie.ef.sak.integration.dto.pdl.PdlPersonSøkRequest
import no.nav.familie.ef.sak.integration.dto.pdl.PdlPersonSøkRequestVariables
import no.nav.familie.ef.sak.integration.dto.pdl.PdlResponse
import no.nav.familie.ef.sak.integration.dto.pdl.PersonSøk
import no.nav.familie.ef.sak.integration.dto.pdl.PersonSøkResultat
import no.nav.familie.ef.sak.integration.dto.pdl.SøkeKriterier
import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.http.sts.StsRestClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.client.RestOperations

/**
 * Denne klienten sender med azuretokenet til saksbehandler slik att PDL kan sjekke tilgang på dataen som returneres
 */
@Service
class PdlSaksbehandlerClient(val pdlConfig: PdlConfig,
                             @Qualifier("azureMedApiKey") restTemplate: RestOperations,
                             val stsRestClient: StsRestClient)
    : AbstractRestClient(restTemplate, "pdl.personinfo.saksbehandler") {

    fun søkPersonerMedSammeAdresse(søkeKriterier: List<SøkeKriterier>): PersonSøkResultat {
        val pdlPersonSøkRequest = PdlPersonSøkRequest(variables = PdlPersonSøkRequestVariables(paging = Paging(1, 30),
                                                                                               criteria = søkeKriterier),
                                                      query = PdlConfig.søkPersonQuery)
        val pdlResponse: PdlResponse<PersonSøk> = postForEntity(pdlConfig.pdlUri,
                                                                pdlPersonSøkRequest,
                                                                httpHeaders())
        return feilsjekkOgReturnerData(null, pdlResponse) { it.sokPerson }
    }

    private fun httpHeaders(): HttpHeaders {

        return HttpHeaders().apply {
            add("Tema", "ENF")
        }
    }
}
