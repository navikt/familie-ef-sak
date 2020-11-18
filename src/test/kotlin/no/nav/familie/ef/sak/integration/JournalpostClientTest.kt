package no.nav.familie.ef.sak.integration

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.config.IntegrasjonerConfig
import no.nav.familie.ef.sak.domene.DokumentVariantformat
import no.nav.familie.http.sts.StsRestClient
import no.nav.familie.kontrakter.ef.søknad.Testsøknad
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.objectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestOperations
import java.net.URI

internal class JournalpostClientTest {

    companion object {

        private val restOperations: RestOperations = RestTemplateBuilder().build()
        lateinit var journalpostClient: JournalpostClient
        lateinit var wiremockServerItem: WireMockServer
        lateinit var integrasjonerConfig: IntegrasjonerConfig

        @BeforeAll
        @JvmStatic
        fun initClass() {
            wiremockServerItem = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())
            wiremockServerItem.start()
            integrasjonerConfig = IntegrasjonerConfig(URI.create(wiremockServerItem.baseUrl()))
            val stsRestClient = mockk<StsRestClient>()
            every { stsRestClient.systemOIDCToken } returns "token"
            journalpostClient = JournalpostClient(restOperations, integrasjonerConfig)

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
    fun `skal hente ut overgangsstønad-json for en journalpost`() {
        val søknadOvergangsstønad = Testsøknad.søknadOvergangsstønad
        wiremockServerItem.stubFor(
                WireMock.get(WireMock.urlPathMatching("${integrasjonerConfig.journalPostUri.path}/hentdokument/([0-9]*)/([0-9]*)"))
                        .withQueryParam("variantFormat", WireMock.equalTo("ORIGINAL"))
                        .willReturn(WireMock.okJson(objectMapper.writeValueAsString(Ressurs.success(søknadOvergangsstønad)))))
        val response = journalpostClient.hentOvergangsstønadSøknad("123", "123")


        assertThat(response).isNotNull
        assertThat(response.personalia).isEqualTo(søknadOvergangsstønad.personalia)
        assertThat(response.bosituasjon).isEqualTo(søknadOvergangsstønad.bosituasjon)
        assertThat(response.innsendingsdetaljer).isEqualTo(søknadOvergangsstønad.innsendingsdetaljer)
        assertThat(response.medlemskapsdetaljer).isEqualTo(søknadOvergangsstønad.medlemskapsdetaljer)
    }

    @Test
    fun `skal hente ut vedlegg for en journalpost`() {
        val vedlegg =
                "255044462D312E0D747261696C65723C3C2F526F6F743C3C2F50616765733C3C2F4B6964735B3C3C2F4D65646961426F785B302030203320335D3E3E5D3E3E3E3E3E3E".toByteArray()
        wiremockServerItem.stubFor(
                WireMock.get(WireMock.urlPathMatching("${integrasjonerConfig.journalPostUri.path}/hentdokument/([0-9]*)/([0-9]*)"))
                        .withQueryParam("variantFormat", WireMock.equalTo("ARKIV"))
                        .willReturn(WireMock.okJson(objectMapper.writeValueAsString(Ressurs.success(vedlegg)))))
        val response = journalpostClient.hentDokument("123", "123", DokumentVariantformat.ARKIV)


        assertThat(response).isNotNull
        assertThat(response).isEqualTo(vedlegg)
    }

    @Test
    fun `skal feile dersom et vedlegg ikke finnes for på en journalpost`() {
        val vedlegg =
                "255044462D312E0D747261696C65723C3C2F526F6F743C3C2F50616765733C3C2F4B6964735B3C3C2F4D65646961426F785B302030203320335D3E3E5D3E3E3E3E3E3E".toByteArray()
        wiremockServerItem.stubFor(
                WireMock.get(WireMock.urlPathMatching("${integrasjonerConfig.journalPostUri.path}/hentdokument/([0-9]*)/([0-9]*)"))
                        .withQueryParam("variantFormat", WireMock.equalTo("ARKIV"))
                        .willReturn(WireMock.okJson(objectMapper.writeValueAsString(Ressurs.success(vedlegg)))))
        assertThrows<HttpClientErrorException> {
            journalpostClient.hentDokument("123", "abc", DokumentVariantformat.ARKIV)
        }

    }

}