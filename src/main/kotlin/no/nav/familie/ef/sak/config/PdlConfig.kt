package no.nav.familie.ef.sak.config

import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.spring.ClientConfigurationProperties
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Configuration
class PdlConfig(val oAuth2AccessTokenService: OAuth2AccessTokenService,
                val clientConfigurationProperties: ClientConfigurationProperties,
                @Value("\${PDL_URL}") pdlUrl: URI) {


    val systemToken: String
        get() = oAuth2AccessTokenService.getAccessToken(clientConfigurationProperties.registration["pdl"]).accessToken


    val pdlUri = UriComponentsBuilder.fromUri(pdlUrl).path(PATH_GRAPHQL).build().toUri()

    val søkerQuery = this::class.java.getResource("/pdl/søker.graphql").readText().graphqlCompatible()

//    val barnQuery = this::class.java.getResource("/pdl/barn.graphql").readText().graphqlCompatible()

//    private val annenForelderQuery =
//            this::class.java.getResource("/pdl/annenForelder.graphql").readText().graphqlCompatible()

    companion object {
        private const val PATH_GRAPHQL = "graphql"
    }

    fun String.graphqlCompatible(): String {
        return StringUtils.normalizeSpace(this.replace("\n", ""))
    }

}