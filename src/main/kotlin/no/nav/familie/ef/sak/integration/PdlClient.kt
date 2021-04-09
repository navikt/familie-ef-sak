package no.nav.familie.ef.sak.integration

import no.nav.familie.ef.sak.config.PdlConfig
import no.nav.familie.ef.sak.exception.PdlNotFoundException
import no.nav.familie.ef.sak.exception.PdlRequestException
import no.nav.familie.ef.sak.integration.dto.pdl.*
import no.nav.familie.http.client.AbstractPingableRestClient
import no.nav.familie.http.sts.StsRestClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.client.RestOperations
import java.net.URI

@Service
class PdlClient(val pdlConfig: PdlConfig,
                @Qualifier("stsMedApiKey") restTemplate: RestOperations,
                val stsRestClient: StsRestClient)
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

    fun sokPersoner(bostedsadresse: Bostedsadresse): PersonSøkResultat {
        val pdlPersonSøkRequest = PdlPersonSøkRequest(variables = PdlPersonSøkRequestVariables(paging = Paging(1, 30),
                                                                                               criteria = PdlPersonSøkHjelper.lagPdlPersonSøkCriteria(
                                                                                                       bostedsadresse)),
                                                      query = PdlConfig.søkPersonQuery)
        val pdlResponse: PdlResponse<PersonSøk> = postForEntity(pdlConfig.pdlUri,
                                                                        pdlPersonSøkRequest,
                                                                        httpHeaders())
        //todo feilhåndtering
        return pdlResponse.data.sokPerson
    }

    private inline fun <reified DATA : Any, reified T : Any> feilsjekkOgReturnerData(ident: String,
                                                                                     pdlResponse: PdlResponse<DATA>,
                                                                                     dataMapper: (DATA) -> T?): T {

        if (pdlResponse.harFeil()) {
            if (pdlResponse.errors?.any { it.extensions?.notFound() == true } == true) {
                throw PdlNotFoundException()
            }
            secureLogger.error("Feil ved henting av ${T::class} fra PDL: ${pdlResponse.errorMessages()}")
            throw PdlRequestException("Feil ved henting av ${T::class} fra PDL. Se secure logg for detaljer.")
        }

        val data = dataMapper.invoke(pdlResponse.data)
        if (data == null) {
            secureLogger.error("Feil ved oppslag på ident $ident. " +
                               "PDL rapporterte ingen feil men returnerte tomt datafelt")
            throw PdlRequestException("Manglende ${T::class} ved feilfri respons fra PDL. Se secure logg for detaljer.")
        }
        return data
    }

    private inline fun <reified T : Any> feilsjekkOgReturnerData(pdlResponse: PdlBolkResponse<T>): Map<String, T> {
        if (pdlResponse.data == null) {
            secureLogger.error("Data fra pdl er null ved bolkoppslag av ${T::class} fra PDL: ${pdlResponse.errorMessages()}")
            throw PdlRequestException("Data er null fra PDL -  ${T::class}. Se secure logg for detaljer.")
        }

        val feil = pdlResponse.data.personBolk.filter { it.code != "ok" }.map { it.ident to it.code }.toMap()
        if (feil.isNotEmpty()) {
            secureLogger.error("Feil ved henting av ${T::class} fra PDL: $feil")
            throw PdlRequestException("Feil ved henting av ${T::class} fra PDL. Se secure logg for detaljer.")
        }
        return pdlResponse.data.personBolk.associateBy({ it.ident }, { it.person!! })
    }

    private fun httpHeaders(): HttpHeaders {

        return HttpHeaders().apply {
            add("Nav-Consumer-Token", "Bearer ${stsRestClient.systemOIDCToken}")
            add("Tema", "ENF")
        }
    }
}
