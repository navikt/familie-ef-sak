package no.nav.familie.ef.sak.integration

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.matching.EqualToPattern
import no.nav.familie.ef.sak.infrastruktur.config.IntegrasjonerConfig
import no.nav.familie.ef.sak.journalføring.dto.DokumentVariantformat
import no.nav.familie.ef.sak.journalføring.JournalpostClient
import no.nav.familie.kontrakter.ef.søknad.Testsøknad
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.dokarkiv.ArkiverDokumentResponse
import no.nav.familie.kontrakter.felles.dokarkiv.v2.ArkiverDokumentRequest
import no.nav.familie.kontrakter.felles.objectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
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
    fun `skal hente ut og parse søknad om overgangsstønad`() {
        val vedlegg =
                objectMapper.writeValueAsString(Ressurs.success(objectMapper.writeValueAsBytes(Testsøknad.søknadOvergangsstønad)))
        wiremockServerItem.stubFor(
                WireMock.get(WireMock.urlPathMatching("${integrasjonerConfig.journalPostUri.path}/hentdokument/([0-9]*)/([0-9]*)"))
                        .withQueryParam("variantFormat", WireMock.equalTo("ORIGINAL"))
                        .willReturn(WireMock.okJson(vedlegg)))
        val response = journalpostClient.hentOvergangsstønadSøknad("123", "123")


        assertThat(response).isNotNull
        assertThat(response.personalia.verdi.fødselsnummer).isEqualTo(Testsøknad.søknadOvergangsstønad.personalia.verdi.fødselsnummer)
    }


    @Test
    fun `skal hente ut og parse søknad om barnetilsyn`() {
        val vedlegg =
                objectMapper.writeValueAsString(Ressurs.success(objectMapper.writeValueAsBytes(Testsøknad.søknadBarnetilsyn)))
        wiremockServerItem.stubFor(
                WireMock.get(WireMock.urlPathMatching("${integrasjonerConfig.journalPostUri.path}/hentdokument/([0-9]*)/([0-9]*)"))
                        .withQueryParam("variantFormat", WireMock.equalTo("ORIGINAL"))
                        .willReturn(WireMock.okJson(vedlegg)))
        val response = journalpostClient.hentBarnetilsynSøknad("123", "123")

        assertThat(response).isNotNull
        assertThat(response.personalia.verdi.fødselsnummer).isEqualTo(Testsøknad.søknadBarnetilsyn.personalia.verdi.fødselsnummer)
    }

    @Test
    fun `skal hente ut og parse søknad om skolepenger`() {
        val vedlegg =
                objectMapper.writeValueAsString(Ressurs.success(objectMapper.writeValueAsBytes(Testsøknad.søknadSkolepenger)))
        wiremockServerItem.stubFor(
                WireMock.get(WireMock.urlPathMatching("${integrasjonerConfig.journalPostUri.path}/hentdokument/([0-9]*)/([0-9]*)"))
                        .withQueryParam("variantFormat", WireMock.equalTo("ORIGINAL"))
                        .willReturn(WireMock.okJson(vedlegg)))
        val response = journalpostClient.hentSkolepengerSøknad("123", "123")


        assertThat(response).isNotNull
        assertThat(response.personalia.verdi.fødselsnummer).isEqualTo(Testsøknad.søknadSkolepenger.personalia.verdi.fødselsnummer)
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

    @Test
    internal fun `skal sende med saksbehandlerIdent`() {
        val saksbehandler = "k123123"
        val response = Ressurs.success(ArkiverDokumentResponse("1", true))
        wiremockServerItem.stubFor(WireMock.post(WireMock.anyUrl())
                                           .willReturn(okJson(objectMapper.writeValueAsString(response))))
        journalpostClient.arkiverDokument(ArkiverDokumentRequest("123", true, emptyList(), emptyList()), saksbehandler)

        wiremockServerItem.verify(1, postRequestedFor(WireMock.anyUrl()).withHeader("Nav-User-Id", EqualToPattern(saksbehandler)))
    }
}