package no.nav.familie.ef.sak.infotrygd

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import efterlatte.prosessering.TaskId
import efterlatte.prosessering.TaskType
import efterlatte.prosessering.spring.TaskService
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ef.sak.infotrygd.skygge.SKYGGEKJØR_INFOTRYGD_TASK_TYPE
import no.nav.familie.ef.sak.infotrygd.skygge.SkyggeInfotrygdOperasjon
import no.nav.familie.ef.sak.infotrygd.skygge.SkyggeInfotrygdPayload
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.Toggle
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPeriodeRequest
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPeriodeResponse
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.kontrakter.felles.jsonMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.restclient.RestTemplateBuilder
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.web.client.RestOperations
import java.net.URI

/**
 * Verifiserer at [InfotrygdReplikaClient.skyggekjør] lagrer skyggetasken riktig via den nye
 * [TaskService] (efterlatte-prosessering), se pilot-notatene i InfotrygdReplikaClient.
 */
internal class InfotrygdReplikaClientTest {
    companion object {
        private val taskService = mockk<TaskService>()
        private val featureToggleService = mockk<FeatureToggleService>()
        private lateinit var wiremockServer: WireMockServer
        private lateinit var infotrygdReplikaClient: InfotrygdReplikaClient

        @BeforeAll
        @JvmStatic
        fun initClass() {
            wiremockServer = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())
            wiremockServer.start()
            val restOperations: RestOperations =
                RestTemplateBuilder()
                    .additionalMessageConverters(JacksonJsonHttpMessageConverter(jsonMapper))
                    .build()
            infotrygdReplikaClient =
                InfotrygdReplikaClient(
                    infotrygdReplikaUri = URI.create(wiremockServer.baseUrl()),
                    restOperations = restOperations,
                    taskService = taskService,
                    featureToggleService = featureToggleService,
                )
        }

        @AfterAll
        @JvmStatic
        fun tearDown() {
            wiremockServer.stop()
        }
    }

    private val request = InfotrygdPeriodeRequest(setOf("12345678910"), setOf(StønadType.OVERGANGSSTØNAD))
    private val respons = InfotrygdPeriodeResponse(overgangsstønad = emptyList(), barnetilsyn = emptyList(), skolepenger = emptyList())

    @BeforeEach
    fun setup() {
        clearMocks(taskService)
        wiremockServer.resetAll()
        wiremockServer.stubFor(
            post(urlPathEqualTo("/api/perioder"))
                .willReturn(okJson(jsonMapper.writeValueAsString(respons))),
        )
    }

    @AfterEach
    fun tearDownEachTest() {
        wiremockServer.resetAll()
    }

    @Test
    internal fun `skal lagre skyggetask med riktig type og payload når toggle er på`() {
        every { featureToggleService.isEnabled(Toggle.SKYGGEKJØR_INFOTRYGD) } returns true
        val payloadSlot = slot<SkyggeInfotrygdPayload>()
        every {
            taskService.opprettIEgenTransaksjon(any<TaskType<SkyggeInfotrygdPayload>>(), capture(payloadSlot), any())
        } returns TaskId(1L)

        infotrygdReplikaClient.hentPerioder(request)

        verify(exactly = 1) {
            taskService.opprettIEgenTransaksjon(SKYGGEKJØR_INFOTRYGD_TASK_TYPE, any<SkyggeInfotrygdPayload>(), null)
        }
        val payload = payloadSlot.captured
        assertThat(payload.operasjon).isEqualTo(SkyggeInfotrygdOperasjon.HENT_PERIODER)
        assertThat(payload.personIdenter).isEqualTo(request.personIdenter)
        assertThat(payload.request).isEqualTo(jsonMapper.writeValueAsString(request))
        assertThat(payload.forventetRespons).isEqualTo(jsonMapper.writeValueAsString(respons))
    }

    @Test
    internal fun `skal ikke lagre skyggetask når toggle er av`() {
        every { featureToggleService.isEnabled(Toggle.SKYGGEKJØR_INFOTRYGD) } returns false

        infotrygdReplikaClient.hentPerioder(request)

        verify(exactly = 0) {
            taskService.opprettIEgenTransaksjon(any<TaskType<SkyggeInfotrygdPayload>>(), any(), any())
        }
    }

    @Test
    internal fun `feil ved lagring av skyggetask skal ikke påvirke det ordinære svaret`() {
        every { featureToggleService.isEnabled(Toggle.SKYGGEKJØR_INFOTRYGD) } returns true
        every {
            taskService.opprettIEgenTransaksjon(any<TaskType<SkyggeInfotrygdPayload>>(), any(), any())
        } throws RuntimeException("Klarte ikke å lagre task")

        val actuellRespons = infotrygdReplikaClient.hentPerioder(request)

        assertThat(actuellRespons).isEqualTo(respons)
    }
}
