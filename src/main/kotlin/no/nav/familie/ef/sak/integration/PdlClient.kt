package no.nav.familie.ef.sak.integration

import no.nav.familie.ef.sak.config.PdlConfig
import no.nav.familie.ef.sak.exception.PdlRequestException
import no.nav.familie.ef.sak.integration.dto.pdl.*
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

    fun hentSøkerKort(personIdent: String): PdlSøkerKort {
        val pdlPersonRequest = PdlPersonRequest(variables = PdlPersonRequestVariables(personIdent),
                                                query = PdlConfig.søkerKortQuery)
        val pdlResponse: PdlResponse<PdlSøkerKortData> = postForEntity(pdlConfig.pdlUri,
                                                                       pdlPersonRequest,
                                                                       httpHeaders())
        return feilsjekkOgReturnerData(personIdent, pdlResponse).person
    }

    fun hentSøker(personIdent: String): PdlSøker {
        val pdlPersonRequest = PdlPersonRequest(variables = PdlPersonRequestVariables(personIdent),
                                                query = PdlConfig.søkerQuery)
        val pdlResponse: PdlResponse<PdlSøkerData> = postForEntity(pdlConfig.pdlUri,
                                                                   pdlPersonRequest,
                                                                   httpHeaders())
        return feilsjekkOgReturnerData(personIdent, pdlResponse).person
    }

    //Brukes for å hente hele pdl dataobjektet uten serialisering
    fun hentSøkerAsMap(personIdent: String): Map<String, Any> {
        val pdlPersonRequest = PdlPersonRequest(variables = PdlPersonRequestVariables(personIdent),
                                                query = PdlConfig.søkerQuery)
        val pdlResponse: PdlResponse<Map<String, Any>> = postForEntity(pdlConfig.pdlUri,
                                                                       pdlPersonRequest,
                                                                       httpHeaders())
        return feilsjekkOgReturnerData(personIdent, pdlResponse)
    }

    fun hentBarn(personIdent: List<String>): Map<String, PdlBarn> {
        val pdlPersonRequest = PdlPersonBolkRequest(variables = PdlPersonBolkRequestVariables(personIdent),
                                                    query = PdlConfig.barnQuery)
        val pdlResponse: PdlBolkResponse<PdlBarn> = postForEntity(pdlConfig.pdlUri,
                                                                  pdlPersonRequest,
                                                                  httpHeaders())
        return feilsjekkOgReturnerData(pdlResponse)
    }

    fun hentAndreForeldre(personIdent: List<String>): Map<String, PdlAnnenForelder> {
        val pdlPersonRequest = PdlPersonBolkRequest(variables = PdlPersonBolkRequestVariables(personIdent),
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

    private inline fun <reified T : Any> feilsjekkOgReturnerData(ident: String,
                                                                 pdlResponse: PdlResponse<T>): T {

        if (pdlResponse.harFeil()) {
            secureLogger.error("Feil ved henting av ${T::class} fra PDL: ${pdlResponse.errorMessages()}")
            throw PdlRequestException("Feil ved henting av ${T::class} fra PDL. Se secure logg for detaljer.")
        }

        if (pdlResponse.data == null) {
            secureLogger.error("Feil ved oppslag på ident $ident. " +
                               "PDL rapporterte ingen feil men returnerte tomt datafelt")
            throw PdlRequestException("Manglende ${T::class} ved feilfri respons fra PDL. Se secure logg for detaljer.")
        }
        return pdlResponse.data
    }

    private inline fun <reified T : Any> feilsjekkOgReturnerData(pdlResponse: PdlBolkResponse<T>): Map<String, T> {

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
