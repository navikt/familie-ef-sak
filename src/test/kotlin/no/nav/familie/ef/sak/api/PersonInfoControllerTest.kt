package no.nav.familie.ef.sak.api

import com.github.tomakehurst.wiremock.client.WireMock
import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.api.dto.PersonIdentDto
import no.nav.familie.ef.sak.api.gui.PersonInfoController
import no.nav.familie.ef.sak.api.gui.dto.Person
import no.nav.familie.ef.sak.config.PdlConfig
import no.nav.familie.ef.sak.integration.dto.familie.Tilgang
import no.nav.familie.ef.sak.integration.dto.personopplysning.PersonIdent
import no.nav.familie.ef.sak.integration.dto.personopplysning.PersonhistorikkInfo
import no.nav.familie.ef.sak.integration.dto.personopplysning.Personinfo
import no.nav.familie.ef.sak.integration.dto.personopplysning.relasjon.SivilstandType
import no.nav.familie.ef.sak.integration.dto.personopplysning.status.PersonstatusType
import no.nav.familie.ef.sak.integration.dto.personopplysning.tilhørighet.Landkode
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.objectMapper
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.exchange
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import java.time.LocalDate

@ActiveProfiles("local", "mock-pdl", "mock-oauth", "integrasjonstest")
@TestPropertySource(properties = ["FAMILIE_INTEGRASJONER_URL=http://localhost:28085"])
@AutoConfigureWireMock(port = 28085)
class PersonInfoControllerTest : OppslagSpringRunnerTest() {

    @Autowired
    lateinit var personInfoController: PersonInfoController

    @BeforeEach
    fun setUp() {
        headers.setBearerAuth(lokalTestToken)
        WireMock.stubFor(WireMock.post(WireMock.urlEqualTo("/api/tilgang/personer"))
                                 .willReturn(WireMock.aResponse()
                                                     .withStatus(200)
                                                     .withHeader("Content-Type", "application/json")
                                                     .withBody(objectMapper.writeValueAsString(listOf(Tilgang(true, null))))))

        WireMock.stubFor(WireMock.post(WireMock.urlEqualTo("/${PdlConfig.PATH_GRAPHQL}"))
                                 .willReturn(WireMock.aResponse()
                                                     .withStatus(200)
                                                     .withHeader("Content-Type", "application/json")
                                                     .withBody("{}")))

    }

    @Test
    fun `skal korrekt behandle returobjekt`() {
        WireMock.stubFor(WireMock.post(WireMock.urlEqualTo("/api/personopplysning/v2/info"))
                                 .willReturn(WireMock.aResponse()
                                                     .withStatus(200)
                                                     .withHeader("Content-Type", "application/json")
                                                     .withBody(objectMapper.writeValueAsString(person))))
        WireMock.stubFor(WireMock.post(WireMock.urlPathEqualTo("/api/personopplysning/v2/historikk"))
                                 .willReturn(WireMock.aResponse()
                                                     .withStatus(200)
                                                     .withHeader("Content-Type", "application/json")
                                                     .withBody(objectMapper.writeValueAsString(personhistorikkInfo))))

        val response =
                restTemplate.exchange<Ressurs<Person>>(localhost(PERSONINFO_PATH),
                                                       HttpMethod.POST,
                                                       HttpEntity(PersonIdentDto(PERSONIDENT), headers))

        Assertions.assertThat(response).isNotNull

        Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        Assertions.assertThat(response.body?.data?.personhistorikkInfo?.personIdent?.id).isEqualTo(person.data?.personIdent?.id)
        Assertions.assertThat(response.body?.data?.personinfo?.personIdent?.id).isEqualTo(person.data?.personIdent?.id)
    }

    @Test
    fun `skal kaste feil hvis personinfo ikke funnet`() {
        WireMock.stubFor(WireMock.post(WireMock.urlEqualTo("/api/personopplysning/v2/info"))
                                 .willReturn(WireMock.aResponse()
                                                     .withStatus(404)))
        WireMock.stubFor(WireMock.post(WireMock.urlEqualTo("/api/personopplysning/v2/historikk"))
                                 .willReturn(WireMock.aResponse()
                                                     .withStatus(200)
                                                     .withHeader("Content-Type", "application/json")
                                                     .withBody(objectMapper.writeValueAsString(personhistorikkInfo))))

        val response: ResponseEntity<String> = restTemplate.exchange(localhost(PERSONINFO_PATH),
                                                                     HttpMethod.POST,
                                                                     HttpEntity(PersonIdentDto(PERSONIDENT), headers))

        Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        Assertions.assertThat(response.body).isEqualTo("Feil mot personopplysning. Message=404 Not Found: [no body]")

    }

    @Test
    fun `skal kaste feil hvis personhistorikk ikke funnet`() {
        WireMock.stubFor(WireMock.post(WireMock.urlEqualTo("/api/personopplysning/v2/info"))
                                 .willReturn(WireMock.aResponse()
                                                     .withStatus(200)
                                                     .withHeader("Content-Type", "application/json")
                                                     .withBody(objectMapper.writeValueAsString(person))))
        WireMock.stubFor(WireMock.post(WireMock.urlPathEqualTo("/api/personopplysning/v2/historikk"))
                                 .willReturn(WireMock.aResponse()
                                                     .withStatus(404)))

        val response: ResponseEntity<String> = restTemplate.exchange(localhost(PERSONINFO_PATH),
                                                                     HttpMethod.POST,
                                                                     HttpEntity(PersonIdentDto(PERSONIDENT), headers))

        Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        Assertions.assertThat(response.body).isEqualTo("Feil mot personopplysning. Message=404 Not Found: [no body]")
    }

    companion object {
        private const val PERSONINFO_PATH = "/api/personinfo"
        private const val PERSONIDENT = "12345678901"
    }

    private val person = Ressurs.success(Personinfo(PersonIdent("12345678901"),
                                                    "Bob",
                                                    null,
                                                    null,
                                                    LocalDate.now(),
                                                    null,
                                                    PersonstatusType.FØDR,
                                                    SivilstandType.ENKE,
                                                    emptySet(),
                                                    Landkode.NORGE,
                                                    null,
                                                    null,
                                                    null,
                                                    null,
                                                    emptyList(),
                                                    15))
    private val personhistorikkInfo = Ressurs.success(PersonhistorikkInfo(PersonIdent("12345678901"),
                                                                          emptyList(),
                                                                          emptyList(),
                                                                          emptyList()))

}
