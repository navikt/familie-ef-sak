package no.nav.familie.ef.sak.config

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import no.nav.familie.ef.sak.arbeidsfordeling.Arbeidsfordelingsenhet
import no.nav.familie.ef.sak.felles.integration.dto.EgenAnsattResponse
import no.nav.familie.ef.sak.felles.integration.dto.Tilgang
import no.nav.familie.ef.sak.infrastruktur.config.IntegrasjonerConfig
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.infrastruktur.config.pdfAsBase64String
import no.nav.familie.kontrakter.ef.sak.DokumentBrevkode
import no.nav.familie.kontrakter.ef.søknad.Testsøknad
import no.nav.familie.kontrakter.felles.BrukerIdType
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.dokarkiv.ArkiverDokumentResponse
import no.nav.familie.kontrakter.felles.dokarkiv.OppdaterJournalpostResponse
import no.nav.familie.kontrakter.felles.ef.PerioderOvergangsstønadResponse
import no.nav.familie.kontrakter.felles.journalpost.Bruker
import no.nav.familie.kontrakter.felles.journalpost.DokumentInfo
import no.nav.familie.kontrakter.felles.journalpost.Dokumentvariant
import no.nav.familie.kontrakter.felles.journalpost.Dokumentvariantformat
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.familie.kontrakter.felles.journalpost.Journalposttype
import no.nav.familie.kontrakter.felles.journalpost.Journalstatus
import no.nav.familie.kontrakter.felles.journalpost.RelevantDato
import no.nav.familie.kontrakter.felles.kodeverk.BeskrivelseDto
import no.nav.familie.kontrakter.felles.kodeverk.BetydningDto
import no.nav.familie.kontrakter.felles.kodeverk.KodeverkDto
import no.nav.familie.kontrakter.felles.medlemskap.Medlemskapsinfo
import no.nav.familie.kontrakter.felles.navkontor.NavKontorEnhet
import no.nav.familie.kontrakter.felles.objectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalDateTime


@Component
class FamilieIntegrasjonerMock(integrasjonerConfig: IntegrasjonerConfig) {

    val responses =
            listOf(
                    WireMock.get(WireMock.urlEqualTo(integrasjonerConfig.pingUri.path))
                            .willReturn(WireMock.aResponse().withStatus(200)),
                    WireMock.post(WireMock.urlEqualTo(integrasjonerConfig.egenAnsattUri.path))
                            .willReturn(WireMock.okJson(objectMapper.writeValueAsString(egenAnsatt))),
                    WireMock.post(WireMock.urlEqualTo(integrasjonerConfig.tilgangRelasjonerUri.path))
                            .withRequestBody(WireMock.matching(".*ikkeTilgang.*"))
                            .atPriority(1)
                            .willReturn(lagIkkeTilgangResponse()),
                    WireMock.post(WireMock.urlEqualTo(integrasjonerConfig.tilgangRelasjonerUri.path))
                            .willReturn(WireMock.okJson(objectMapper.writeValueAsString(listOf(Tilgang(true, null))))),
                    WireMock.post(WireMock.urlEqualTo(integrasjonerConfig.tilgangPersonUri.path))
                            .withRequestBody(WireMock.matching(".*ikkeTilgang.*"))
                            .atPriority(1)
                            .willReturn(lagIkkeTilgangResponse()),
                    WireMock.post(WireMock.urlEqualTo(integrasjonerConfig.tilgangPersonUri.path))
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
                    WireMock.post(WireMock.urlPathEqualTo(integrasjonerConfig.journalPostUri.path))
                            .willReturn(WireMock.okJson(objectMapper.writeValueAsString(journalposter))),
                    WireMock.get(WireMock.urlPathMatching("${integrasjonerConfig.journalPostUri.path}/hentdokument/([0-9]*)/([0-9]*)"))
                            .withQueryParam("variantFormat", equalTo("ORIGINAL"))
                            .willReturn(WireMock.okJson(
                                    objectMapper.writeValueAsString(Ressurs.success(objectMapper.writeValueAsBytes(Testsøknad.søknadOvergangsstønad)))
                            )),
                    WireMock.get(WireMock.urlPathMatching("${integrasjonerConfig.journalPostUri.path}/hentdokument/([0-9]*)/([0-9]*)"))
                            .withQueryParam("variantFormat", equalTo("ARKIV"))
                            .willReturn(WireMock.okJson(objectMapper.writeValueAsString(Ressurs.success(pdfAsBase64String)))),
                    WireMock.put(WireMock.urlMatching("${integrasjonerConfig.dokarkivUri.path}.*"))
                            .willReturn(WireMock.okJson(objectMapper.writeValueAsString(oppdatertJournalpostResponse))),
                    WireMock.post(WireMock.urlMatching("${integrasjonerConfig.dokarkivUri.path}.*"))
                            .willReturn(WireMock.okJson(objectMapper.writeValueAsString(arkiverDokumentResponse))),
                    WireMock.post(WireMock.urlPathEqualTo(integrasjonerConfig.medlemskapUri.path))
                            .willReturn(WireMock.okJson(objectMapper.writeValueAsString(medl))),
                    WireMock.post(WireMock.urlEqualTo(integrasjonerConfig.navKontorUri.path))
                            .willReturn(WireMock.okJson(objectMapper.writeValueAsString(navKontorEnhet))),

                    WireMock.post(WireMock.urlEqualTo(integrasjonerConfig.infotrygdVedtaksperioder.path))
                            .willReturn(WireMock.okJson(objectMapper.writeValueAsString(infotrygdPerioder)))

            )

    private fun lagIkkeTilgangResponse() = WireMock.okJson(
            objectMapper.writeValueAsString(Tilgang(false,
                                                    "Mock sier: Du har " +
                                                    "ikke tilgang " +
                                                    "til person ikkeTilgang")))

    @Bean("mock-integrasjoner")
    @Profile("mock-integrasjoner")
    fun integrationMockServer(): WireMockServer {
        val mockServer = WireMockServer(8385)
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

        val fnr = "23097825289"
        private val medl =
                Ressurs.success(Medlemskapsinfo(personIdent = fnr,
                                                gyldigePerioder = emptyList(),
                                                uavklartePerioder = emptyList(),
                                                avvistePerioder = emptyList()))

        private val oppdatertJournalpostResponse =
                Ressurs.success(OppdaterJournalpostResponse(journalpostId = "1234"))
        private val arkiverDokumentResponse = Ressurs.success(ArkiverDokumentResponse(journalpostId = "1234", ferdigstilt = true))
        val journalpostFraIntegrasjoner = Journalpost(journalpostId = "1234",
                                                      journalposttype = Journalposttype.I,
                                                      journalstatus = Journalstatus.MOTTATT,
                                                      tema = "ENF",
                                                      behandlingstema = "ab0071",
                                                      tittel = "abrakadabra",
                                                      bruker = Bruker(type = BrukerIdType.FNR, id = fnr),
                                                      journalforendeEnhet = "4817",
                                                      kanal = "SKAN_IM",
                                                      relevanteDatoer = listOf(RelevantDato(LocalDateTime.now(),
                                                                                            "DATO_REGISTRERT")),
                                                      dokumenter =
                                                      listOf(DokumentInfo(dokumentInfoId = "12345",
                                                                          tittel = "Søknad om overgangsstønad - dokument 1",
                                                                          brevkode = DokumentBrevkode.OVERGANGSSTØNAD.verdi,
                                                                          dokumentvarianter =
                                                                          listOf(Dokumentvariant(variantformat = Dokumentvariantformat.ARKIV),
                                                                                 Dokumentvariant(variantformat = Dokumentvariantformat.ORIGINAL)
                                                                          )
                                                      ),
                                                             DokumentInfo(dokumentInfoId = "12345",
                                                                          tittel = "Søknad om barnetilsyn - dokument 1",
                                                                          brevkode = DokumentBrevkode.OVERGANGSSTØNAD.verdi,
                                                                          dokumentvarianter =
                                                                          listOf(Dokumentvariant(variantformat = Dokumentvariantformat.ARKIV))
                                                             ),
                                                             DokumentInfo(dokumentInfoId = "12345",
                                                                          tittel = "Samboeravtale",
                                                                          brevkode = DokumentBrevkode.OVERGANGSSTØNAD.verdi,
                                                                          dokumentvarianter =
                                                                          listOf(Dokumentvariant(variantformat = Dokumentvariantformat.ARKIV))
                                                             ),
                                                             DokumentInfo(dokumentInfoId = "12345",
                                                                          tittel = "EtFrykteligLangtDokumentNavnSomTroligIkkeBrekkerOgØdeleggerGUI",
                                                                          brevkode = DokumentBrevkode.OVERGANGSSTØNAD.verdi,
                                                                          dokumentvarianter =
                                                                          listOf(Dokumentvariant(variantformat = Dokumentvariantformat.ARKIV))
                                                             ),
                                                             DokumentInfo(dokumentInfoId = "12345",
                                                                          tittel = "Søknad om overgangsstønad - dokument 2",
                                                                          brevkode = DokumentBrevkode.OVERGANGSSTØNAD.verdi,
                                                                          dokumentvarianter =
                                                                          listOf(Dokumentvariant(variantformat = Dokumentvariantformat.ARKIV))
                                                             ),
                                                             DokumentInfo(dokumentInfoId = "12345",
                                                                          tittel = "Søknad om overgangsstønad - dokument 3",
                                                                          brevkode = DokumentBrevkode.OVERGANGSSTØNAD.verdi,
                                                                          dokumentvarianter =
                                                                          listOf(Dokumentvariant(variantformat = Dokumentvariantformat.ARKIV))
                                                             )
                                                      )
        )
        private val journalpost = Ressurs.success(journalpostFraIntegrasjoner)
        private val journalposter = Ressurs.success(listOf(journalpostFraIntegrasjoner))
        private val navKontorEnhet = Ressurs.success(NavKontorEnhet(enhetId = 100000194,
                                                                    navn = "NAV Kristiansand",
                                                                    enhetNr = "1001",
                                                                    status = "Aktiv"))

        private val infotrygdPerioder = Ressurs.success(PerioderOvergangsstønadResponse(emptyList()))
    }
}