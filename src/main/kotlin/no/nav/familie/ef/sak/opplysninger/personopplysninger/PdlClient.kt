package no.nav.familie.ef.sak.opplysninger.personopplysninger

import no.nav.familie.ef.sak.infrastruktur.config.PdlConfig
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlAnnenForelder
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlBarn
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlBolkResponse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlHentIdenter
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdentRequest
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdentRequestVariables
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdenter
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlPersonBolkRequest
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlPersonBolkRequestVariables
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlPersonKort
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlPersonRequest
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlPersonRequestVariables
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlResponse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlSøker
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlSøkerData
import no.nav.familie.http.client.AbstractPingableRestClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.client.RestOperations
import java.net.URI

@Service
class PdlClient(val pdlConfig: PdlConfig,
                @Qualifier("azureClientCredential") restTemplate: RestOperations)
    : AbstractPingableRestClient(restTemplate, "pdl.personinfo") {

    override val pingUri: URI
        get() = pdlConfig.pdlUri

    override fun ping() {
        operations.optionsForAllow(pingUri)
    }

    fun hentSøker(personIdent: String): PdlSøker {
        val pdlPersonRequest = PdlPersonRequest(variables = PdlPersonRequestVariables(personIdent),
                                                query = PdlConfig.søkerQuery)
        val pdlResponse: PdlResponse<PdlSøkerData> = postForEntity(pdlConfig.pdlUri,
                                                                   pdlPersonRequest,
                                                                   httpHeaders())
        return feilsjekkOgReturnerData(personIdent, pdlResponse) { it.person }
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
