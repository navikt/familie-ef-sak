package no.nav.familie.ef.sak.integration

import no.nav.familie.ef.sak.config.PdlConfig
import no.nav.familie.ef.sak.integration.dto.pdl.PdlHentPersonResponse
import no.nav.familie.ef.sak.integration.dto.pdl.PdlPersonRequest
import no.nav.familie.ef.sak.integration.dto.pdl.PdlPersonRequestVariables
import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.http.sts.StsRestClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.client.RestOperations

@Service
class PdlClient(val pdlConfig: PdlConfig,
                @Qualifier("sts") restTemplate: RestOperations,
                val stsRestClient: StsRestClient)
    : AbstractRestClient(restTemplate, "pdl.personinfo") {


    fun hentSøker(personIdent: String): PdlHentPersonResponse {
        val pdlPersonRequest = PdlPersonRequest(variables = PdlPersonRequestVariables(personIdent, true),
                                                query = pdlConfig.søkerQuery)
        return postForEntity(pdlConfig.pdlUri,
                             pdlPersonRequest,
                             httpHeaders())!!
    }

    fun hentBarn(personIdent: String): PdlHentPersonResponse {
        val pdlPersonRequest = PdlPersonRequest(variables = PdlPersonRequestVariables(personIdent, true),
                                                query = pdlConfig.barnQuery)
        return postForEntity(pdlConfig.pdlUri,
                             pdlPersonRequest,
                             httpHeaders())!!


    }

    fun hentForelder2(personIdent: String): PdlHentPersonResponse {
        val pdlPersonRequest = PdlPersonRequest(variables = PdlPersonRequestVariables(personIdent, true),
                                                query = pdlConfig.annenForelderQuery)
        return postForEntity(pdlConfig.pdlUri,
                             pdlPersonRequest,
                             httpHeaders())!!
    }

    private fun httpHeaders(): HttpHeaders {

        return HttpHeaders().apply {
            add("Nav-Consumer-Token", "Bearer ${stsRestClient.systemOIDCToken}")
            add("Tema", "ENF")
        }
    }

}
