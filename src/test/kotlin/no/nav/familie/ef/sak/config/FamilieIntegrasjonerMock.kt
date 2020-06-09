package no.nav.familie.ef.sak.no.nav.familie.ef.sak.config

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import no.nav.familie.ef.sak.config.IntegrasjonerConfig
import no.nav.familie.ef.sak.integration.dto.EgenAnsattResponse
import no.nav.familie.ef.sak.integration.dto.Tilgang
import no.nav.familie.ef.sak.integration.dto.personopplysning.Periode
import no.nav.familie.ef.sak.integration.dto.personopplysning.PersonIdent
import no.nav.familie.ef.sak.integration.dto.personopplysning.PersonhistorikkInfo
import no.nav.familie.ef.sak.integration.dto.personopplysning.Personinfo
import no.nav.familie.ef.sak.integration.dto.personopplysning.adresse.Adresse
import no.nav.familie.ef.sak.integration.dto.personopplysning.adresse.AdressePeriode
import no.nav.familie.ef.sak.integration.dto.personopplysning.adresse.AdresseType
import no.nav.familie.ef.sak.integration.dto.personopplysning.adresse.Adresseinfo
import no.nav.familie.ef.sak.integration.dto.personopplysning.relasjon.Familierelasjon
import no.nav.familie.ef.sak.integration.dto.personopplysning.relasjon.RelasjonsRolleType
import no.nav.familie.ef.sak.integration.dto.personopplysning.relasjon.SivilstandType
import no.nav.familie.ef.sak.integration.dto.personopplysning.status.PersonstatusPeriode
import no.nav.familie.ef.sak.integration.dto.personopplysning.status.PersonstatusType
import no.nav.familie.ef.sak.integration.dto.personopplysning.tilhørighet.Landkode
import no.nav.familie.ef.sak.integration.dto.personopplysning.tilhørighet.StatsborgerskapPeriode
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.objectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.time.LocalDate


@Component
class FamilieIntegrasjonerMock(integrasjonerConfig: IntegrasjonerConfig) {

    val responses = listOf(
            WireMock.get(WireMock.urlEqualTo(integrasjonerConfig.pingUri.path))
                    .willReturn(WireMock.aResponse().withStatus(200)),
            WireMock.post(WireMock.urlEqualTo(integrasjonerConfig.egenAnsattUri.path))
                    .willReturn(WireMock.okJson(objectMapper.writeValueAsString(egenAnsatt))),
            WireMock.get(WireMock.urlEqualTo(integrasjonerConfig.personopplysningerUri.path))
                    .willReturn(WireMock.okJson(objectMapper.writeValueAsString(person))),
            WireMock.post(WireMock.urlEqualTo(integrasjonerConfig.tilgangUri.path))
                    .withRequestBody(WireMock.matching(".*ikketilgang.*"))
                    .atPriority(1)
                    .willReturn(WireMock.okJson(objectMapper
                                                        .writeValueAsString(listOf(Tilgang(false,
                                                                                           "Mock sier: Du har ikke tilgang " +
                                                                                           "til person ikketilgang"))))),
            WireMock.get(WireMock.urlPathMatching(integrasjonerConfig.personhistorikkUri.path))
                    .atPriority(2)
                    .withQueryParam("tomDato", WireMock.equalTo(LocalDate.now().toString()))
                    .withQueryParam("fomDato", WireMock.equalTo(LocalDate.now().minusYears(5).toString()))
                    .willReturn(WireMock.okJson(objectMapper.writeValueAsString(personhistorikkInfo))),
            WireMock.post(WireMock.urlEqualTo(integrasjonerConfig.tilgangUri.path))
                    .willReturn(WireMock.okJson(objectMapper.writeValueAsString(listOf(Tilgang(true, null))))))

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
        private val egenAnsatt = Ressurs.success(EgenAnsattResponse(false))
        private val person = Ressurs.success(Personinfo(PersonIdent("12137578901"),
                                                        "Bob",
                                                        Adresseinfo(AdresseType.BOSTEDSADRESSE,
                                                                    "Bob",
                                                                    "Adresselinje 1",
                                                                    "Adresselinje 2",
                                                                    "Adresselinje 3",
                                                                    "Adresselinje 4",
                                                                    "0754",
                                                                    "Oslo",
                                                                    "Norge",
                                                                    PersonstatusType.BOSA),

                                                        "Kjønn",
                                                        LocalDate.of(1975, 12, 13),
                                                        null,
                                                        PersonstatusType.FØDR,
                                                        SivilstandType.ENKE,
                                                        setOf(Familierelasjon(PersonIdent("24120165487"),
                                                                              RelasjonsRolleType.BARN,
                                                                              LocalDate.of(2001, 12, 24),
                                                                              true),
                                                              Familierelasjon(PersonIdent("11127565498"),
                                                                              RelasjonsRolleType.EKTE,
                                                                              LocalDate.of(1975, 12, 11),
                                                                              false)),
                                                        Landkode.NORGE,
                                                        "Utenlandsadresse",
                                                        "Geografisk tilknytning",
                                                        "Diskresjonskode",
                                                        "adresseLandkode",
                                                        listOf(Adresseinfo(AdresseType.BOSTEDSADRESSE,
                                                                           "Bob",
                                                                           "Adresselinje 1",
                                                                           "Adresselinje 2",
                                                                           "Adresselinje 3",
                                                                           "Adresselinje 4",
                                                                           "0754",
                                                                           "Oslo",
                                                                           "Norge",
                                                                           PersonstatusType.BOSA),
                                                               Adresseinfo(AdresseType.POSTADRESSE,
                                                                           "Bob",
                                                                           "Adresselinje 1",
                                                                           "Adresselinje 2",
                                                                           "Adresselinje 3",
                                                                           "Adresselinje 4",
                                                                           "0754",
                                                                           "Oslo",
                                                                           "Norge",
                                                                           PersonstatusType.BOSA)),
                                                        45))
        private val personhistorikkInfo =
                Ressurs.success(PersonhistorikkInfo(PersonIdent("12137578901"),
                                                    listOf(PersonstatusPeriode(Periode(LocalDate.of(1975, 12, 13),
                                                                                       LocalDate.of(2004, 5, 21)),
                                                                               PersonstatusType.FØDR)),
                                                    listOf(StatsborgerskapPeriode(Periode(LocalDate.of(1975, 12, 13),
                                                                                          LocalDate.of(2004, 5, 21)),
                                                                                  Landkode.UDEFINERT)),
                                                    listOf(AdressePeriode(Periode(LocalDate.of(1975, 12, 13),
                                                                                  LocalDate.of(2004, 5, 21)),
                                                                          Adresse(AdresseType.BOSTEDSADRESSE,
                                                                                  "Adresselinje 1",
                                                                                  "Adresselinje 2",
                                                                                  "Adresselinje 3",
                                                                                  "Adresselinje 4",
                                                                                  "zipcode",
                                                                                  "Boston",
                                                                                  "Uniten")),
                                                           AdressePeriode(Periode(LocalDate.of(1975, 12, 13),
                                                                                  LocalDate.of(9999, 12, 31)),
                                                                          Adresse(AdresseType.BOSTEDSADRESSE,
                                                                                  "Adresselinje 1",
                                                                                  "Adresselinje 2",
                                                                                  "Adresselinje 3",
                                                                                  "Adresselinje 4",
                                                                                  "0754",
                                                                                  "Oslo",
                                                                                  "Norge")))))
    }
}