package no.nav.familie.ef.sak.integration

import no.nav.familie.ef.sak.config.PdlConfig
import no.nav.familie.ef.sak.integration.dto.pdl.PdlAnnenForelder
import no.nav.familie.ef.sak.integration.dto.pdl.PdlBarn
import no.nav.familie.ef.sak.integration.dto.pdl.PdlBolkResponse
import no.nav.familie.ef.sak.integration.dto.pdl.PdlHentIdenter
import no.nav.familie.ef.sak.integration.dto.pdl.PdlIdentRequest
import no.nav.familie.ef.sak.integration.dto.pdl.PdlIdentRequestVariables
import no.nav.familie.ef.sak.integration.dto.pdl.PdlIdenter
import no.nav.familie.ef.sak.integration.dto.pdl.PdlPersonBolkRequest
import no.nav.familie.ef.sak.integration.dto.pdl.PdlPersonBolkRequestVariables
import no.nav.familie.ef.sak.integration.dto.pdl.PdlPersonKort
import no.nav.familie.ef.sak.integration.dto.pdl.PdlPersonRequest
import no.nav.familie.ef.sak.integration.dto.pdl.PdlPersonRequestVariables
import no.nav.familie.ef.sak.integration.dto.pdl.PdlResponse
import no.nav.familie.ef.sak.integration.dto.pdl.PdlSøker
import no.nav.familie.ef.sak.integration.dto.pdl.PdlSøkerData
import no.nav.familie.ef.sak.integration.dto.pdl.PdlSøkerKort
import no.nav.familie.http.client.AbstractPingableRestClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.client.RestOperations
import java.net.URI

@Service
class PdlClient(val pdlConfig: PdlConfig,
                @Qualifier("azureClientCredentials") restTemplate: RestOperations)
    : AbstractPingableRestClient(restTemplate, "pdl.personinfo") {

    override val pingUri: URI
        get() = pdlConfig.pdlUri

    override fun ping() {
        operations.optionsForAllow(pingUri)
    }

    fun hentSøkerKortBolk(personIdenter: List<String>): Map<String, PdlSøkerKort> {
        if (personIdenter.isEmpty()) return emptyMap()
        require(personIdenter.size <= 100) { "Liste med personidenter må være færre enn 100 st" }
        val pdlPersonRequest = PdlPersonBolkRequest(variables = PdlPersonBolkRequestVariables(personIdenter),
                                                    query = PdlConfig.søkerKortBolkQuery)
        val pdlResponse: PdlBolkResponse<PdlSøkerKort> = postForEntity(pdlConfig.pdlUri,
                                                                       pdlPersonRequest,
                                                                       httpHeaders())
        return feilsjekkOgReturnerData(pdlResponse)
    }

    fun hentSøker(personIdent: String): PdlSøker {
        val pdlPersonRequest = PdlPersonRequest(variables = PdlPersonRequestVariables(personIdent),
                                                query = PdlConfig.søkerQuery)
        val pdlResponse: PdlResponse<PdlSøkerData> = postForEntity(pdlConfig.pdlUri,
                                                                   pdlPersonRequest,
                                                                   httpHeaders())
        return feilsjekkOgReturnerData(personIdent, pdlResponse) { it.person }
    }

    //Brukes for å hente hele pdl dataobjektet uten serialisering
    fun hentSøkerAsMap(personIdent: String): Map<String, Any> {
        val pdlPersonRequest = PdlPersonRequest(variables = PdlPersonRequestVariables(personIdent),
                                                query = PdlConfig.søkerQuery)
        val pdlResponse: PdlResponse<Map<String, Any>> = postForEntity(pdlConfig.pdlUri,
                                                                       pdlPersonRequest,
                                                                       httpHeaders())
        return feilsjekkOgReturnerData(personIdent, pdlResponse) { it }
    }

    fun hentBarn(personIdenter: List<String>): Map<String, PdlBarn> {
        if (personIdenter.isEmpty()) return emptyMap()
        val pdlPersonRequest = PdlPersonBolkRequest(variables = PdlPersonBolkRequestVariables(personIdenter),
                                                    query = PdlConfig.barnQuery)
        val pdlResponse: PdlBolkResponse<PdlBarn> = postForEntity(pdlConfig.pdlUri,
                                                                  pdlPersonRequest,
                                                                  httpHeaders())
        return feilsjekkOgReturnerData(pdlResponse)
    }

    fun hentAndreForeldre(personIdenter: List<String>): Map<String, PdlAnnenForelder> {
        if (personIdenter.isEmpty()) return emptyMap()
        val pdlPersonRequest = PdlPersonBolkRequest(variables = PdlPersonBolkRequestVariables(personIdenter),
                                                    query = PdlConfig.annenForelderQuery)
        val pdlResponse: PdlBolkResponse<PdlAnnenForelder> = postForEntity(pdlConfig.pdlUri,
                                                                           pdlPersonRequest,
                                                                           httpHeaders())
        return feilsjekkOgReturnerData(pdlResponse)
    }

    fun hentPersonKortBolk(personIdenter: List<String>): Map<String, PdlPersonKort> {
        require(personIdenter.size <= 100) { "Liste med personidenter må være færre enn 100 st" }
        val pdlPersonRequest = PdlPersonBolkRequest(variables = PdlPersonBolkRequestVariables(personIdenter),
                                                    query = PdlConfig.personBolkKortQuery)
        val pdlResponse: PdlBolkResponse<PdlPersonKort> = postForEntity(pdlConfig.pdlUri,
                                                                        pdlPersonRequest,
                                                                        httpHeaders())
        return feilsjekkOgReturnerData(pdlResponse)
    }

    /**
     * @param ident Ident til personen, samme hvilke type (Folkeregisterident, aktørid eller npid)
     * @return liste med aktørider
     */
    fun hentAktørIder(ident: String): PdlIdenter {
        val pdlPersonRequest = PdlIdentRequest(variables = PdlIdentRequestVariables(ident, "AKTORID"),
                                               query = PdlConfig.hentIdentQuery)
        val pdlResponse: PdlResponse<PdlHentIdenter> = postForEntity(pdlConfig.pdlUri,
                                                                     pdlPersonRequest,
                                                                     httpHeaders())
        return feilsjekkOgReturnerData(ident, pdlResponse) { it.hentIdenter }
    }

    /**
     * @param ident Ident til personen, samme hvilke type (Folkeregisterident, aktørid eller npid)
     * @param historikk default false, tar med historikk hvis det er ønskelig
     * @return liste med folkeregisteridenter
     */
    fun hentPersonidenter(ident: String, historikk: Boolean = false): PdlIdenter {
        val pdlPersonRequest = PdlIdentRequest(variables = PdlIdentRequestVariables(ident, "FOLKEREGISTERIDENT", historikk),
                                               query = PdlConfig.hentIdentQuery)
        val pdlResponse: PdlResponse<PdlHentIdenter> = postForEntity(pdlConfig.pdlUri,
                                                                     pdlPersonRequest,
                                                                     httpHeaders())
        return feilsjekkOgReturnerData(ident, pdlResponse) { it.hentIdenter }
    }

    private fun httpHeaders(): HttpHeaders {

        return HttpHeaders().apply {
            add("Tema", "ENF")
        }
    }
}
