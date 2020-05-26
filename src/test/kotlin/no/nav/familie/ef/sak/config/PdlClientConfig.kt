package no.nav.familie.ef.sak.no.nav.familie.ef.sak.config

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.integration.PdlClient
import no.nav.familie.ef.sak.integration.dto.pdl.*
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Configuration
@Profile("mock-pdl")
class PdlClientConfig {

    @Bean
    @Primary
    fun pdlClient(): PdlClient {
        val pdlCLient: PdlClient = mockk()
        every { pdlCLient.hentSøkerKort(any()) } returns
                PdlSøkerKort(adressebeskyttelse = listOf(Adressebeskyttelse(gradering = AdressebeskyttelseGradering.UGRADERT)),
                             dødsfall = emptyList(),
                             folkeregisterpersonstatus = listOf(Folkeregisterpersonstatus("bosatt",
                                                                                          "bosattEtterFolkeregisterloven")),
                             kjønn = listOf(Kjønn(KjønnType.KVINNE)),
                             navn = listOf(Navn("Fornavn", "mellomnavn", "Etternavn")))
        return pdlCLient
    }
}