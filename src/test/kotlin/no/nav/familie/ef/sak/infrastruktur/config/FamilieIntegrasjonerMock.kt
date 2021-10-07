package no.nav.familie.ef.sak.config

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import no.nav.familie.ef.sak.arbeidsfordeling.Arbeidsfordelingsenhet
import no.nav.familie.ef.sak.felles.integration.dto.EgenAnsattResponse
import no.nav.familie.ef.sak.felles.integration.dto.Tilgang
import no.nav.familie.ef.sak.infrastruktur.config.IntegrasjonerConfig
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
                            .willReturn(WireMock.okJson(objectMapper
                                                                .writeValueAsString(Tilgang(false,
                                                                                            "Mock sier: Du har " +
                                                                                            "ikke tilgang " +
                                                                                            "til person ikkeTilgang")))),
                    WireMock.post(WireMock.urlEqualTo(integrasjonerConfig.tilgangRelasjonerUri.path))
                            .willReturn(WireMock.okJson(objectMapper.writeValueAsString(Tilgang(true, null)))),
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
                            .willReturn(WireMock.okJson(objectMapper.writeValueAsString(Ressurs.success(pdfAsBase64String2)))),
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
        val pdfAsBase64String =
                "JVBERi0xLjIgCjkgMCBvYmoKPDwKPj4Kc3RyZWFtCkJULyA5IFRmKFRlc3QpJyBFVAplbmRzdHJlYW0KZW5kb2JqCjQgMCBvYmoKPDwKL1R5cGUgL1BhZ2UKL1BhcmVudCA1IDAgUgovQ29udGVudHMgOSAwIFIKPj4KZW5kb2JqCjUgMCBvYmoKPDwKL0tpZHMgWzQgMCBSIF0KL0NvdW50IDEKL1R5cGUgL1BhZ2VzCi9NZWRpYUJveCBbIDAgMCA5OSA5IF0KPj4KZW5kb2JqCjMgMCBvYmoKPDwKL1BhZ2VzIDUgMCBSCi9UeXBlIC9DYXRhbG9nCj4+CmVuZG9iagp0cmFpbGVyCjw8Ci9Sb290IDMgMCBSCj4+CiUlRU9G"
        val pdfAsBase64String2 =
                "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"
        private val arkiverDokumentResponse = Ressurs.success(ArkiverDokumentResponse(journalpostId = "1234", ferdigstilt = true))
        val journalpostFraIntegrasjoner = Journalpost(
                journalpostId = "1234",
                journalposttype = Journalposttype.I,
                journalstatus = Journalstatus.MOTTATT,
                tema = "ENF",
                behandlingstema = "ab0071",
                tittel = "abrakadabra",
                bruker = Bruker(type = BrukerIdType.FNR, id = fnr),
                journalforendeEnhet = "4817",
                kanal = "SKAN_IM",
                relevanteDatoer = listOf(RelevantDato(LocalDateTime.now(), "DATO_REGISTRERT")),
                dokumenter =
                listOf(
                        DokumentInfo(
                                dokumentInfoId = "12345",
                                tittel = "Søknad om overgangsstønad - dokument 1",
                                brevkode = DokumentBrevkode.OVERGANGSSTØNAD.verdi,
                                dokumentvarianter =
                                listOf(
                                        Dokumentvariant(variantformat = Dokumentvariantformat.ARKIV),
                                        Dokumentvariant(variantformat = Dokumentvariantformat.ORIGINAL)
                                )
                        ),
                        DokumentInfo(
                                dokumentInfoId = "12345",
                                tittel = "Søknad om barnetilsyn - dokument 1",
                                brevkode = DokumentBrevkode.OVERGANGSSTØNAD.verdi,
                                dokumentvarianter =
                                listOf(Dokumentvariant(variantformat = Dokumentvariantformat.ARKIV))
                        ),
                        DokumentInfo(
                                dokumentInfoId = "12345",
                                tittel = "Samboeravtale",
                                brevkode = DokumentBrevkode.OVERGANGSSTØNAD.verdi,
                                dokumentvarianter =
                                listOf(Dokumentvariant(variantformat = Dokumentvariantformat.ARKIV))
                        ),
                        DokumentInfo(
                                dokumentInfoId = "12345",
                                tittel = "EtFrykteligLangtDokumentNavnSomTroligIkkeBrekkerOgØdeleggerGUI",
                                brevkode = DokumentBrevkode.OVERGANGSSTØNAD.verdi,
                                dokumentvarianter =
                                listOf(Dokumentvariant(variantformat = Dokumentvariantformat.ARKIV))
                        ),
                        DokumentInfo(
                                dokumentInfoId = "12345",
                                tittel = "Søknad om overgangsstønad - dokument 2",
                                brevkode = DokumentBrevkode.OVERGANGSSTØNAD.verdi,
                                dokumentvarianter =
                                listOf(Dokumentvariant(variantformat = Dokumentvariantformat.ARKIV))
                        ),
                        DokumentInfo(
                                dokumentInfoId = "12345",
                                tittel = "Søknad om overgangsstønad - dokument 3",
                                brevkode = DokumentBrevkode.OVERGANGSSTØNAD.verdi,
                                dokumentvarianter =
                                listOf(Dokumentvariant(variantformat = Dokumentvariantformat.ARKIV))
                        )
                )
        )
        private val journalpost =
                Ressurs.success(journalpostFraIntegrasjoner)
        private val journalposter =
                Ressurs.success(listOf(journalpostFraIntegrasjoner))

        private val navKontorEnhet = Ressurs.success(NavKontorEnhet(enhetId = 100000194,
                                                                    navn = "NAV Kristiansand",
                                                                    enhetNr = "1001",
                                                                    status = "Aktiv"))

        private val infotrygdPerioder = Ressurs.success(PerioderOvergangsstønadResponse(emptyList()))
    }
}