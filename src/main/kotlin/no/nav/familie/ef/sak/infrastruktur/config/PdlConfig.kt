package no.nav.familie.ef.sak.infrastruktur.config

import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Configuration
class PdlConfig(
    @Value("\${PDL_URL}") pdlUrl: URI,
) {
    val pdlUri: URI =
        UriComponentsBuilder
            .fromUri(pdlUrl)
            .pathSegment(PATH_GRAPHQL)
            .build()
            .toUri()

    companion object {
        const val PATH_GRAPHQL = "graphql"

        val personBolkKortQuery = graphqlQuery("/pdl/person_kort_bolk.graphql")

        val søkerQuery = graphqlQuery("/pdl/søker.graphql")

        val forelderBarnQuery = graphqlQuery("/pdl/forelder_barn.graphql")

        val annenForelderQuery = graphqlQuery("/pdl/andreForeldre.graphql")

        val hentIdentQuery = graphqlQuery("/pdl/hent_ident.graphql")

        val hentIdenterBolkQuery = graphqlQuery("/pdl/hent_ident_bolk.graphql")

        val søkPersonQuery = graphqlQuery("/pdl/søk_person.graphql")

        private fun graphqlQuery(path: String) =
            PdlConfig::class.java
                .getResource(path)
                .readText()
                .graphqlCompatible()

        private fun String.graphqlCompatible(): String = StringUtils.normalizeSpace(this.replace("\n", ""))
    }
}
