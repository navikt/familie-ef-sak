package no.nav.familie.ef.sak.opplysninger.personopplysninger

import no.nav.familie.ef.sak.infrastruktur.config.PdlConfig
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Paging
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlPersonSøkRequest
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlPersonSøkRequestVariables
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlResponse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PersonSøk
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PersonSøkResultat
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.SøkeKriterier
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.restklient.client.AbstractRestClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.client.RestOperations

/**
 * Denne klienten sender med azuretokenet til saksbehandler slik att PDL kan sjekke tilgang på dataen som returneres
 */
@Service
class PdlSaksbehandlerClient(
    val pdlConfig: PdlConfig,
    @Qualifier("azureOnBehalfOf") restTemplate: RestOperations,
) : AbstractRestClient(restTemplate, "pdl.personinfo.saksbehandler") {
    fun søkPersonerMedSammeAdresse(søkeKriterier: List<SøkeKriterier>): PersonSøkResultat {
        val pdlPersonSøkRequest =
            PdlPersonSøkRequest(
                variables =
                    PdlPersonSøkRequestVariables(
                        paging = Paging(1, 30),
                        criteria = søkeKriterier,
                    ),
                query = PdlConfig.søkPersonQuery,
            )
        val pdlResponse: PdlResponse<PersonSøk> =
            postForEntity(
                pdlConfig.pdlUri,
                pdlPersonSøkRequest,
                httpHeaders(),
            )
        return feilsjekkOgReturnerData(null, pdlResponse) { it.sokPerson }
    }

    private fun httpHeaders(): HttpHeaders =
        HttpHeaders().apply {
            add("Tema", "ENF")
            add("behandlingsnummer", Tema.ENF.behandlingsnummer)
        }
}
