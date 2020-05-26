package no.nav.familie.ef.sak.config

import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Configuration
class PdlConfig(@Value("\${PDL_URL}") pdlUrl: URI) {


    val pdlUri: URI = UriComponentsBuilder.fromUri(pdlUrl).path(PATH_GRAPHQL).build().toUri()

    val søkerKortQuery get() = this::class.java.getResource("/pdl/søker_kort.graphql").readText().graphqlCompatible()

    val søkerQuery get() = this::class.java.getResource("/pdl/søker.graphql").readText().graphqlCompatible()

    val barnQuery get() = this::class.java.getResource("/pdl/barn.graphql").readText().graphqlCompatible()

    val annenForelderQuery get() = this::class.java.getResource("/pdl/annenForelder.graphql").readText().graphqlCompatible()

    companion object {
        private const val PATH_GRAPHQL = "graphql"
    }

    fun String.graphqlCompatible(): String {
        return StringUtils.normalizeSpace(this.replace("\n", ""))
    }

}
