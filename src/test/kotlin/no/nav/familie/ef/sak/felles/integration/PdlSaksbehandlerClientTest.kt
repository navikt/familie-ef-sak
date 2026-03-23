package no.nav.familie.ef.sak.felles.integration

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import no.nav.familie.ef.sak.infrastruktur.config.PdlConfig
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PdlPersonSøkHjelper
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PdlSaksbehandlerClient
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Bostedsadresse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Metadata
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Vegadresse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.boot.restclient.RestTemplateBuilder
import org.springframework.web.client.RestOperations
import java.net.URI

internal class PdlSaksbehandlerClientTest {
    companion object {
        private val restOperations: RestOperations = RestTemplateBuilder().build()
        lateinit var pdlClient: PdlSaksbehandlerClient
        lateinit var wiremockServerItem: WireMockServer

        @BeforeAll
        @JvmStatic
        fun initClass() {
            wiremockServerItem = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())
            wiremockServerItem.start()
            pdlClient =
                PdlSaksbehandlerClient(
                    PdlConfig(URI.create(wiremockServerItem.baseUrl())),
                    restOperations,
                )
        }

        @AfterAll
        @JvmStatic
        fun tearDown() {
            wiremockServerItem.stop()
        }
    }

    @AfterEach
    fun tearDownEachTest() {
        wiremockServerItem.resetAll()
    }

    @Test
    fun `pdlClient håndterer response for person søk gitt bostedsadresse`() {
        wiremockServerItem.stubFor(
            WireMock
                .post(WireMock.urlEqualTo("/${PdlConfig.PATH_GRAPHQL}"))
                .willReturn(WireMock.okJson(readFile("person_søk.json"))),
        )
        val bostedsadresse =
            Bostedsadresse(
                vegadresse =
                    Vegadresse(
                        husnummer = "1",
                        adressenavn = "KLINGAVEGEN",
                        postnummer = "0358",
                        bruksenhetsnummer = null,
                        matrikkelId = 1L,
                        husbokstav = null,
                        kommunenummer = null,
                        tilleggsnavn = null,
                        koordinater = null,
                    ),
                matrikkeladresse = null,
                angittFlyttedato = null,
                gyldigFraOgMed = null,
                gyldigTilOgMed = null,
                coAdressenavn = null,
                utenlandskAdresse = null,
                ukjentBosted = null,
                metadata = Metadata(false),
            )
        val response =
            pdlClient.søkPersonerMedSammeAdresse(PdlPersonSøkHjelper.lagPdlPersonSøkKriterier(bostedsadresse))
        assertThat(response.totalHits).isEqualTo(1)
        assertThat(
            response.hits
                .first()
                .person.navn
                .first()
                .fornavn,
        ).isEqualTo("BRÅKETE")
        assertThat(
            response.hits
                .first()
                .person.folkeregisteridentifikator
                .first()
                .identifikasjonsnummer,
        ).isEqualTo("15078817191")
    }

    private fun readFile(filnavn: String): String = this::class.java.getResource("/json/$filnavn").readText()
}
