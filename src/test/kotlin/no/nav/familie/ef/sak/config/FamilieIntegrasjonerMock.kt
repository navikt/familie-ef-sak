package no.nav.familie.ef.sak.no.nav.familie.ef.sak.config

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import no.nav.familie.ef.sak.config.IntegrasjonerConfig
import no.nav.familie.ef.sak.integration.dto.personopplysning.PersonIdent
import no.nav.familie.ef.sak.integration.dto.personopplysning.PersonhistorikkInfo
import no.nav.familie.ef.sak.integration.dto.personopplysning.Personinfo
import no.nav.familie.ef.sak.integration.dto.personopplysning.relasjon.SivilstandType
import no.nav.familie.ef.sak.integration.dto.personopplysning.status.PersonstatusType
import no.nav.familie.ef.sak.integration.dto.personopplysning.tilhørighet.Landkode
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.objectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.time.LocalDate


@Component
class FamilieIntegrasjonerMock(integrasjonerConfig: IntegrasjonerConfig) {

    val responses = listOf(WireMock.get(WireMock.urlEqualTo(integrasjonerConfig.pingUri.path))
                                   .willReturn(WireMock.aResponse().withStatus(200)),
                           WireMock.get(WireMock.urlEqualTo(integrasjonerConfig.personopplysningerUri.path))
                                   .willReturn(WireMock.aResponse()
                                                       .withStatus(200)
                                                       .withHeader("Content-Type", "application/json")
                                                       .withBody(objectMapper.writeValueAsString(person))),
                           WireMock.get(WireMock.urlEqualTo(integrasjonerConfig.personhistorikkUri.path))
                                   .willReturn(WireMock.aResponse()
                                                       .withStatus(200)
                                                       .withHeader("Content-Type", "application/json")
                                                       .withBody(objectMapper.writeValueAsString(personhistorikkInfo))))

    @Bean("mock-integrasjoner")
    @Profile("mock-integrasjoner")
    fun integrationMockServer(): WireMockServer {
        val mockServer = WireMockServer(8085)
        responses.forEach {
            mockServer.stubFor(it)
        }
        mockServer.start()
        return mockServer
    }

    companion object {
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
}