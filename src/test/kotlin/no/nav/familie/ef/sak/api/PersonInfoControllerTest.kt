package no.nav.familie.ef.sak.api

import com.github.tomakehurst.wiremock.client.WireMock
import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.api.dto.Person
import no.nav.familie.ef.sak.integration.dto.personopplysning.PersonIdent
import no.nav.familie.ef.sak.integration.dto.personopplysning.PersonhistorikkInfo
import no.nav.familie.ef.sak.integration.dto.personopplysning.Personinfo
import no.nav.familie.ef.sak.integration.dto.personopplysning.relasjon.SivilstandType
import no.nav.familie.ef.sak.integration.dto.personopplysning.status.PersonstatusType
import no.nav.familie.ef.sak.integration.dto.personopplysning.tilhørighet.Landkode
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.objectMapper
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.springframework.boot.test.web.client.exchange
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import java.time.LocalDate

@ActiveProfiles("integrasjonstest", "mock-oauth")
@TestPropertySource(properties = ["FAMILIE_INTEGRASJONER_API_URL=http://localhost:28085"])
@AutoConfigureWireMock(port = 28085)
class PersonInfoControllerTest : OppslagSpringRunnerTest() {

    @Before
    fun setUp() {
        headers.setBearerAuth(lokalTestToken)
    }

    @Test
    fun `skal korrekt behandle returobjekt`() {
        WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/api/v1/medlemskapsunntak"))
                                 .willReturn(WireMock.aResponse()
                                                     .withStatus(200)
                                                     .withHeader("Content-Type", "application/json")
                                                     .withBody(gyldigOppgaveResponse("medlrespons.json"))))

        val response: ResponseEntity<Ressurs<Person>> = restTemplate.exchange(localhost(GET_PERSONINFO),
                                                                                  HttpMethod.GET,
                                                                                  HttpEntity(null, headers))

        Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        Assertions.assertThat(response.body?.status).isEqualTo(Ressurs.success(null).status)
        Assertions.assertThat(response.body?.data?.personinfo?.personIdent).isEqualTo("12345678901")
    }

//    @Test
//    fun `skal kaste feil for ikke funnet`() {
//        WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/api/v1/medlemskapsunntak"))
//                                 .willReturn(WireMock.aResponse()
//                                                     .withStatus(404)
//                                                     .withHeader("Content-Type", "application/json")))
//
//        val response: ResponseEntity<Ressurs<Personinfo>> = restTemplate.exchange(localhost(GET_MEDLEMSKAP_URL),
//                                                                                  HttpMethod.GET,
//                                                                                  HttpEntity(null, headers))
//
//        Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
//    }
//
    private fun gyldigOppgaveResponse(filnavn: String): String {
        return objectMapper.writeValueAsString(person)
    }

    companion object {
        private const val GET_PERSONINFO = "/api/personinfo"
    }

    private val person = Person(Personinfo(PersonIdent("12345678901"),
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
                                           15),
                                PersonhistorikkInfo(PersonIdent("12345678901"),
                                                    emptyList(),
                                                    emptyList(),
                                                    emptyList()))

}
