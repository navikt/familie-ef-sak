package no.nav.familie.ef.sak.integration

import no.nav.familie.ef.sak.config.PdlConfig
import no.nav.familie.ef.sak.integration.dto.pdl.PdlHentPersonResponse
import no.nav.familie.ef.sak.integration.dto.pdl.PdlPersonRequest
import no.nav.familie.ef.sak.integration.dto.pdl.PdlPersonRequestVariables
import no.nav.familie.http.client.AbstractRestClient
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenResponse
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.spring.ClientConfigurationProperties
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.client.RestOperations

@Service
class PdlClient(private val pdlConfig: PdlConfig,
                restTemplate: RestOperations,
//                val stsRestClient: StsRestClient
                private val oAuth2AccessTokenService: OAuth2AccessTokenService,
                private val configurationProperties: ClientConfigurationProperties
) : AbstractRestClient(restTemplate, "pdl.personinfo") {


    fun hentSøker(personIdent: String): PdlHentPersonResponse {
        val pdlPersonRequest = PdlPersonRequest(variables = PdlPersonRequestVariables(personIdent, true),
                                                query = pdlConfig.søkerQuery)
        return postForEntity(pdlConfig.pdlUri,
                             pdlPersonRequest,
                             httpHeaders())!!
    }

    private fun httpHeaders(): HttpHeaders {
        val response: OAuth2AccessTokenResponse = oAuth2AccessTokenService.getAccessToken(configurationProperties.registration["pdlSystem"])

        val systemOIDCToken =
                return HttpHeaders().apply {
//            add("Nav-Consumer-Token", "Bearer ${stsRestClient.systemOIDCToken}")
                    add("Nav-Consumer-Token", "Bearer ${response.accessToken}")
                    add("Tema", "ENF")
                }
    }

}
