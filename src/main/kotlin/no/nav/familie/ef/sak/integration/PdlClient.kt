package no.nav.familie.ef.sak.integration

import no.nav.familie.ef.sak.exception.PdlRequestException
import no.nav.familie.ef.sak.config.PdlConfig
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


    fun hentSøker(personIdent: String): PdlSøker {
        val pdlPersonRequest = PdlPersonRequest(variables = PdlPersonRequestVariables(personIdent),
                                                query = pdlConfig.søkerQuery)
        return hentFraPdl<PdlSøkerData>(pdlPersonRequest).person
    }

    fun hentBarn(personIdent: String): PdlBarn {
        val pdlPersonRequest = PdlPersonRequest(variables = PdlPersonRequestVariables(personIdent),
                                                query = pdlConfig.barnQuery)
        return hentFraPdl<PdlBarnData>(pdlPersonRequest).person
    }

    fun hentForelder2(personIdent: String): PdlAnnenForelder {
        val pdlPersonRequest = PdlPersonRequest(variables = PdlPersonRequestVariables(personIdent),
                                                query = pdlConfig.annenForelderQuery)
        return hentFraPdl<PdlAnnenForelderData>(pdlPersonRequest).person
    }

    private inline fun <reified T : Any> hentFraPdl(pdlPersonRequest: PdlPersonRequest): T {
        val pdlRespone: PdlResponse<T> = postForEntity(pdlConfig.pdlUri,
                                                       pdlPersonRequest,
                                                       httpHeaders())!!

        if (pdlRespone.harFeil()) {
            secureLogger.error("Feil ved henting av ${T::class} fra PDL: ${pdlRespone.errorMessages()}")
            throw PdlRequestException("Feil ved henting av ${T::class} fra PDL. Se secure logg for detaljer.")
        }

        if (pdlRespone.data == null) {
            secureLogger.error("Feil ved oppslag på ident ${pdlPersonRequest.variables.ident}. " +
                               "PDL rapporterte ingen feil men returnerte tomt datafelt")
            throw PdlRequestException("Manglende ${T::class} ved feilfri respons fra PDL. Se secure logg for detaljer.")
        }
        return pdlRespone.data
    }

    private fun httpHeaders(): HttpHeaders {

        return HttpHeaders().apply {
            add("Nav-Consumer-Token", "Bearer ${stsRestClient.systemOIDCToken}")
            add("Tema", "ENF")
        }
    }
}
