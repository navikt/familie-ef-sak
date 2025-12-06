package no.nav.familie.ef.sak.felles.integration

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.anyUrl
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.put
import com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.matching.EqualToPattern
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import no.nav.familie.ef.sak.infrastruktur.config.IntegrasjonerConfig
import no.nav.familie.ef.sak.infrastruktur.exception.ApiFeil
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.Toggle
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.journalføring.JournalpostClient
import no.nav.familie.ef.sak.journalføring.dto.DokumentVariantformat
import no.nav.familie.kontrakter.ef.søknad.Testsøknad
import no.nav.familie.kontrakter.felles.BrukerIdType
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.dokarkiv.ArkiverDokumentResponse
import no.nav.familie.kontrakter.felles.dokarkiv.BulkOppdaterLogiskVedleggRequest
import no.nav.familie.kontrakter.felles.dokarkiv.v2.ArkiverDokumentRequest
import no.nav.familie.kontrakter.felles.journalpost.Bruker
import no.nav.familie.kontrakter.felles.journalpost.JournalposterForBrukerRequest
import no.nav.familie.kontrakter.felles.journalpost.Journalposttype
import no.nav.familie.kontrakter.felles.objectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.boot.restclient.RestTemplateBuilder
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestOperations
import java.net.URI

internal class JournalpostClientTest {
    companion object {
        private val featureToggleService = mockk<FeatureToggleService>(relaxed = true)
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
            journalpostClient =
                JournalpostClient(restOperations, integrasjonerConfig, featureToggleService)
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
        unmockkObject(SikkerhetContext)
    }

    @BeforeEach
    fun setup() {
        every {
            featureToggleService.isEnabled(any())
        } answers {
            firstArg<Toggle>() != Toggle.UTVIKLER_MED_VEILEDERRROLLE
        }
        mockkObject(SikkerhetContext)
    }

    @Test
    fun `skal hente ut vedlegg for en journalpost`() {
        val vedlegg =
            "255044462D312E0D747261696C65723C3C2F526F6F743C3C2F50616765733C3C2F".toByteArray()
        wiremockServerItem.stubFor(
            get(urlPathMatching("${integrasjonerConfig.journalPostUri.path}/hentdokument/([0-9]*)/([0-9]*)"))
                .withQueryParam("variantFormat", equalTo("ARKIV"))
                .willReturn(okJson(objectMapper.writeValueAsString(Ressurs.success(vedlegg)))),
        )
        val response = journalpostClient.hentDokument("123", "123", DokumentVariantformat.ARKIV)

        assertThat(response).isNotNull
        assertThat(response).isEqualTo(vedlegg)
    }

    @Test
    fun `skal hente ut og parse søknad om overgangsstønad`() {
        val vedlegg =
            objectMapper.writeValueAsString(Ressurs.success(objectMapper.writeValueAsBytes(Testsøknad.søknadOvergangsstønad)))
        wiremockServerItem.stubFor(
            get(urlPathMatching("${integrasjonerConfig.journalPostUri.path}/hentdokument/([0-9]*)/([0-9]*)"))
                .withQueryParam("variantFormat", equalTo("ORIGINAL"))
                .willReturn(okJson(vedlegg)),
        )
        val response = journalpostClient.hentOvergangsstønadSøknad("123", "123")

        assertThat(response).isNotNull
        assertThat(response.personalia.verdi.fødselsnummer)
            .isEqualTo(Testsøknad.søknadOvergangsstønad.personalia.verdi.fødselsnummer)
    }

    @Test
    fun `skal hente ut og parse søknad om barnetilsyn`() {
        val vedlegg =
            objectMapper.writeValueAsString(Ressurs.success(objectMapper.writeValueAsBytes(Testsøknad.søknadBarnetilsyn)))
        wiremockServerItem.stubFor(
            get(urlPathMatching("${integrasjonerConfig.journalPostUri.path}/hentdokument/([0-9]*)/([0-9]*)"))
                .withQueryParam("variantFormat", equalTo("ORIGINAL"))
                .willReturn(okJson(vedlegg)),
        )
        val response = journalpostClient.hentBarnetilsynSøknad("123", "123")

        assertThat(response).isNotNull
        assertThat(response.personalia.verdi.fødselsnummer).isEqualTo(Testsøknad.søknadBarnetilsyn.personalia.verdi.fødselsnummer)
    }

    @Test
    fun `skal hente ut og parse søknad om skolepenger`() {
        val vedlegg =
            objectMapper.writeValueAsString(Ressurs.success(objectMapper.writeValueAsBytes(Testsøknad.søknadSkolepenger)))
        wiremockServerItem.stubFor(
            get(urlPathMatching("${integrasjonerConfig.journalPostUri.path}/hentdokument/([0-9]*)/([0-9]*)"))
                .withQueryParam("variantFormat", equalTo("ORIGINAL"))
                .willReturn(okJson(vedlegg)),
        )
        val response = journalpostClient.hentSkolepengerSøknad("123", "123")

        assertThat(response).isNotNull
        assertThat(response.personalia.verdi.fødselsnummer).isEqualTo(Testsøknad.søknadSkolepenger.personalia.verdi.fødselsnummer)
    }

    @Test
    fun `skal feile dersom et vedlegg ikke finnes for på en journalpost`() {
        val vedlegg =
            "255044462D312E0D747261696C65723C3C2F526F6F743C3C2F50616765733C3C2F4B6964735B3C".toByteArray()
        wiremockServerItem.stubFor(
            get(urlPathMatching("${integrasjonerConfig.journalPostUri.path}/hentdokument/([0-9]*)/([0-9]*)"))
                .withQueryParam("variantFormat", equalTo("ARKIV"))
                .willReturn(okJson(objectMapper.writeValueAsString(Ressurs.success(vedlegg)))),
        )
        assertThrows<HttpClientErrorException> {
            journalpostClient.hentDokument("123", "abc", DokumentVariantformat.ARKIV)
        }
    }

    @Test
    internal fun `skal sende med saksbehandlerIdent`() {
        val saksbehandler = "k123123"
        val response = Ressurs.success(ArkiverDokumentResponse("1", true))
        wiremockServerItem.stubFor(
            post(anyUrl())
                .willReturn(okJson(objectMapper.writeValueAsString(response))),
        )
        journalpostClient.arkiverDokument(ArkiverDokumentRequest("123", true, emptyList(), emptyList()), saksbehandler)

        wiremockServerItem.verify(1, postRequestedFor(anyUrl()).withHeader("Nav-User-Id", EqualToPattern(saksbehandler)))
    }

    @Test
    internal fun `skal kaste feil hvis innlogget bruker er utvikler med veillederrolle`() {
        val journalposterForBrukerRequest =
            JournalposterForBrukerRequest(
                brukerId =
                    Bruker(
                        id = "1234",
                        type = BrukerIdType.FNR,
                    ),
                antall = 100,
                tema = listOf(Tema.ENF),
                journalposttype = listOf(Journalposttype.N),
            )

        every { featureToggleService.isEnabled(any()) } returns true

        val feilJournalposter = assertThrows<ApiFeil> { journalpostClient.finnJournalposter(journalposterForBrukerRequest) }
        assertThat(feilJournalposter.httpStatus).isEqualTo(HttpStatus.FORBIDDEN)
        val feilJournalpost = assertThrows<ApiFeil> { journalpostClient.hentJournalpost("1234") }
        assertThat(feilJournalpost.httpStatus).isEqualTo(HttpStatus.FORBIDDEN)
    }

    @Test
    internal fun `skal bulk oppdatere logiske vedlegg for et dokument`() {
        val dokumentInfoId = "123"
        val request = BulkOppdaterLogiskVedleggRequest(titler = listOf("Logisk vedlegg 1", "Logisk vedlegg 2"))

        wiremockServerItem.stubFor(
            put("${integrasjonerConfig.dokarkivUri.path}/dokument/$dokumentInfoId/logiskVedlegg")
                .willReturn(okJson(objectMapper.writeValueAsString(Ressurs.success(dokumentInfoId)))),
        )

        val response = journalpostClient.oppdaterLogiskeVedlegg(dokumentInfoId, request)

        assertThat(response).isNotNull()
        assertThat(response).isEqualTo(dokumentInfoId)
    }
}
