package no.nav.familie.ef.sak.no.nav.familie.ef.sak.config

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import no.nav.familie.ef.sak.config.IntegrasjonerConfig
import no.nav.familie.ef.sak.integration.dto.familie.Arbeidsfordelingsenhet
import no.nav.familie.ef.sak.integration.dto.familie.EgenAnsattResponse
import no.nav.familie.ef.sak.integration.dto.familie.Tilgang
import no.nav.familie.kontrakter.ef.sak.DokumentBrevkode
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.dokarkiv.OppdaterJournalpostResponse
import no.nav.familie.kontrakter.felles.journalpost.*
import no.nav.familie.kontrakter.felles.kodeverk.BeskrivelseDto
import no.nav.familie.kontrakter.felles.kodeverk.BetydningDto
import no.nav.familie.kontrakter.felles.kodeverk.KodeverkDto
import no.nav.familie.kontrakter.felles.objectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.time.LocalDate


@Component
class FamilieIntegrasjonerMock(integrasjonerConfig: IntegrasjonerConfig) {

    val responses =
            listOf(WireMock.get(WireMock.urlEqualTo(integrasjonerConfig.pingUri.path))
                           .willReturn(WireMock.aResponse().withStatus(200)),
                   WireMock.post(WireMock.urlEqualTo(integrasjonerConfig.egenAnsattUri.path))
                           .willReturn(WireMock.okJson(objectMapper.writeValueAsString(egenAnsatt))),
                   WireMock.post(WireMock.urlEqualTo(integrasjonerConfig.tilgangUri.path))
                           .withRequestBody(WireMock.matching(".*ikkeTilgang.*"))
                           .atPriority(1)
                           .willReturn(WireMock.okJson(objectMapper
                                                               .writeValueAsString(listOf(Tilgang(false,
                                                                                                  "Mock sier: Du har " +
                                                                                                  "ikke tilgang " +
                                                                                                  "til person ikkeTilgang"))))),
                   WireMock.post(WireMock.urlEqualTo(integrasjonerConfig.tilgangUri.path))
                           .willReturn(WireMock.okJson(objectMapper.writeValueAsString(listOf(Tilgang(true, null))))),
                   WireMock.get(WireMock.urlEqualTo(integrasjonerConfig.kodeverkPoststedUri.path))
                           .willReturn(WireMock.okJson(objectMapper.writeValueAsString(kodeverkPoststed))),
                   WireMock.get(WireMock.urlEqualTo(integrasjonerConfig.kodeverkLandkoderUri.path))
                           .willReturn(WireMock.okJson(objectMapper.writeValueAsString(kodeverkLand))),
                   WireMock.post(WireMock.urlEqualTo(integrasjonerConfig.arbeidsfordelingUri.path))
                           .willReturn(WireMock.okJson(objectMapper.writeValueAsString(arbeidsfordeling))),

                   WireMock.get(WireMock.urlPathEqualTo(integrasjonerConfig.journalPostUri.path))
                           .withQueryParam("journalpostId", equalTo("1234"))
                           .willReturn(WireMock.okJson(objectMapper.writeValueAsString(journalpost))),
                   WireMock.put(WireMock.urlMatching("${integrasjonerConfig.dokarkivUri.path}.*"))
                           .willReturn(WireMock.okJson(objectMapper.writeValueAsString(oppdatertJournalpostResponse)))

            )

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
        private val poststed =
                KodeverkDto(mapOf("0575" to listOf(BetydningDto(LocalDate.MIN,
                                                                LocalDate.MAX,
                                                                mapOf("nb" to BeskrivelseDto("OSLO",
                                                                                             "OSLO"))))))
        private val land = KodeverkDto(mapOf("NOR" to listOf(BetydningDto(LocalDate.MIN,
                                                                          LocalDate.MAX,
                                                                          mapOf("nb" to BeskrivelseDto("NORGE",
                                                                                                       "NORGE"))))))
        private val kodeverkPoststed = Ressurs.success(poststed)
        private val kodeverkLand = Ressurs.success(land)

        private val arbeidsfordeling =
                Ressurs.success(listOf(Arbeidsfordelingsenhet("1234", "nerd-enhet")))

        private val oppdatertJournalpostResponse =
                Ressurs.success(OppdaterJournalpostResponse(journalpostId = "1234"))
        private val journalpost =
                Ressurs.success(Journalpost(journalpostId = "1234",
                                            journalposttype = Journalposttype.I,
                                            journalstatus = Journalstatus.MOTTATT,
                                            tema = "ENF",
                                            behandlingstema = "ab0071",
                                            tittel = "abrakadabra",
                                            bruker = Bruker(type = BrukerIdType.FNR, id = "23097825289"),
                                            journalforendeEnhet = "4817",
                                            kanal = "SKAN_IM",
                                            dokumenter =
                                            listOf(DokumentInfo(dokumentInfoId = "12345",
                                                                tittel = "Søknad om overgangsstønad - dokument 1",
                                                                brevkode = DokumentBrevkode.OVERGANGSSTØNAD.verdi,
                                                                dokumentvarianter =
                                                                listOf(Dokumentvariant(variantformat = "ARKIV"),
                                                                       Dokumentvariant(variantformat = "ORIGINAL"))))))
    }
}