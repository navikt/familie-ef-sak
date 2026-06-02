package no.nav.familie.ef.sak.infrastruktur.config

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.matching
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.put
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlMatching
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching
import no.nav.familie.ef.sak.arbeidsfordeling.Arbeidsfordelingsenhet
import no.nav.familie.ef.sak.felles.integration.dto.Tilgang
import no.nav.familie.ef.sak.journalføring.JournalføringTestUtil.avsenderMottaker
import no.nav.familie.kontrakter.ef.sak.DokumentBrevkode
import no.nav.familie.kontrakter.ef.søknad.Testsøknad
import no.nav.familie.kontrakter.felles.BrukerIdType
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.dokarkiv.ArkiverDokumentResponse
import no.nav.familie.kontrakter.felles.dokarkiv.OppdaterJournalpostResponse
import no.nav.familie.kontrakter.felles.journalpost.Bruker
import no.nav.familie.kontrakter.felles.journalpost.DokumentInfo
import no.nav.familie.kontrakter.felles.journalpost.Dokumentvariant
import no.nav.familie.kontrakter.felles.journalpost.Dokumentvariantformat
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.familie.kontrakter.felles.journalpost.Journalposttype
import no.nav.familie.kontrakter.felles.journalpost.Journalstatus
import no.nav.familie.kontrakter.felles.journalpost.LogiskVedlegg
import no.nav.familie.kontrakter.felles.journalpost.RelevantDato
import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.kontrakter.felles.kodeverk.BeskrivelseDto
import no.nav.familie.kontrakter.felles.kodeverk.BetydningDto
import no.nav.familie.kontrakter.felles.kodeverk.InntektKodeverkDto
import no.nav.familie.kontrakter.felles.kodeverk.KodeverkDto
import no.nav.familie.kontrakter.felles.medlemskap.Medlemskapsinfo
import no.nav.familie.kontrakter.felles.navkontor.NavKontorEnhet
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.net.URI
import java.time.LocalDate
import java.time.LocalDateTime

@Component
class FamilieIntegrasjonerMock(
    integrasjonerConfig: IntegrasjonerConfig,
) {
    private val responses =
        listOf(
            get(urlEqualTo(integrasjonerConfig.pingUri.path))
                .willReturn(aResponse().withStatus(200)),
            post(urlEqualTo(integrasjonerConfig.tilgangRelasjonerUri.path))
                .withRequestBody(matching(".*ikkeTilgang.*"))
                .atPriority(1)
                .willReturn(okJson(jsonMapper.writeValueAsString(lagIkkeTilgangResponse()))),
            post(urlEqualTo(integrasjonerConfig.tilgangRelasjonerUri.path))
                .willReturn(okJson(jsonMapper.writeValueAsString(Tilgang(true, null)))),
            post(urlEqualTo(integrasjonerConfig.tilgangPersonUri.path))
                .withRequestBody(matching(".*ikkeTilgang.*"))
                .atPriority(1)
                .willReturn(okJson(jsonMapper.writeValueAsString(listOf(lagIkkeTilgangResponse())))),
            post(urlEqualTo(integrasjonerConfig.tilgangPersonUri.path))
                .willReturn(okJson(jsonMapper.writeValueAsString(listOf(Tilgang(true, null))))),
            get(urlEqualTo(integrasjonerConfig.kodeverkPoststedUri.path))
                .willReturn(okJson(jsonMapper.writeValueAsString(kodeverkPoststed))),
            get(urlEqualTo(integrasjonerConfig.kodeverkLandkoderUri.path))
                .willReturn(okJson(jsonMapper.writeValueAsString(kodeverkLand))),
            get(urlEqualTo(integrasjonerConfig.kodeverkInntektUri.path))
                .willReturn(okJson(jsonMapper.writeValueAsString(kodeverkInntekt))),
            post(urlEqualTo(integrasjonerConfig.arbeidsfordelingUri.path))
                .willReturn(okJson(jsonMapper.writeValueAsString(arbeidsfordeling))),
            post(urlEqualTo(integrasjonerConfig.arbeidsfordelingOppfølgingUri.path))
                .willReturn(okJson(jsonMapper.writeValueAsString(arbeidsfordeling))),
            post(urlEqualTo(integrasjonerConfig.arbeidsfordelingMedRelasjonerUri.path))
                .willReturn(okJson(jsonMapper.writeValueAsString(arbeidsfordeling))),
            get(urlPathEqualTo(integrasjonerConfig.journalPostUri.path))
                .withQueryParam("journalpostId", equalTo("1234"))
                .willReturn(okJson(jsonMapper.writeValueAsString(journalpost))),
            get(urlPathEqualTo(integrasjonerConfig.journalPostUri.path))
                .withQueryParam("journalpostId", equalTo("23456"))
                .willReturn(okJson(jsonMapper.writeValueAsString(journalpostPapirsøknad("23456")))),
            get(urlPathEqualTo(integrasjonerConfig.journalPostUri.path))
                .withQueryParam("journalpostId", equalTo("23457"))
                .willReturn(okJson(jsonMapper.writeValueAsString(journalpostPapirsøknad("23457")))),
            get(urlPathEqualTo(integrasjonerConfig.journalPostUri.path))
                .withQueryParam("journalpostId", equalTo("23458"))
                .willReturn(okJson(jsonMapper.writeValueAsString(journalpostPapirsøknad("23458")))),
            post(urlPathEqualTo(integrasjonerConfig.journalPostUri.path))
                .willReturn(okJson(jsonMapper.writeValueAsString(journalposter))),
            get(urlPathMatching("${integrasjonerConfig.journalPostUri.path}/hentdokument/([0-9]*)/([0-9]*)"))
                .withQueryParam("variantFormat", equalTo("ORIGINAL"))
                .willReturn(
                    okJson(
                        jsonMapper.writeValueAsString(
                            Ressurs.success(
                                jsonMapper.writeValueAsBytes(Testsøknad.søknadOvergangsstønad),
                            ),
                        ),
                    ),
                ),
            get(urlPathMatching("${integrasjonerConfig.journalPostUri.path}/hentdokument/([0-9]*)/([0-9]*)"))
                .withQueryParam("variantFormat", equalTo("ARKIV"))
                .willReturn(
                    okJson(
                        jsonMapper.writeValueAsString(
                            Ressurs.success(this::class.java.getResource("/dummy/fiktivt_skjema.pdf").readBytes()),
                        ),
                    ),
                ),
            put(urlMatching("${integrasjonerConfig.dokarkivUri.path}.*"))
                .willReturn(okJson(jsonMapper.writeValueAsString(oppdatertJournalpostResponse))),
            post(urlMatching("${integrasjonerConfig.dokarkivUri.path}.*"))
                .willReturn(okJson(jsonMapper.writeValueAsString(arkiverDokumentResponse))),
            post(urlEqualTo(integrasjonerConfig.navKontorUri.path))
                .willReturn(okJson(jsonMapper.writeValueAsString(navKontorEnhet))),
            post(urlEqualTo(integrasjonerConfig.adressebeskyttelse.path))
                .willReturn(
                    okJson(
                        jsonMapper.writeValueAsString(
                            Ressurs.success(
                                ADRESSEBESKYTTELSEGRADERING
                                    .UGRADERT,
                            ),
                        ),
                    ),
                ),
        )

    private fun lagIkkeTilgangResponse() =
        Tilgang(
            false,
            "Mock sier: Du har " +
                "ikke tilgang " +
                "til person ikkeTilgang",
        )

    @Bean("mock-integrasjoner")
    @Profile("mock-integrasjoner")
    fun integrationMockServer(
        @Value("\${FAMILIE_INTEGRASJONER_URL}") uri: URI,
    ): WireMockServer {
        val mockServer = WireMockServer(uri.port)
        responses.forEach {
            mockServer.stubFor(it)
        }
        mockServer.start()
        return mockServer
    }

    companion object {
        private val poststed =
            KodeverkDto(
                mapOf(
                    "0575" to
                        listOf(
                            BetydningDto(
                                LocalDate.MIN,
                                LocalDate.MAX,
                                mapOf(
                                    "nb" to
                                        BeskrivelseDto(
                                            "OSLO",
                                            "OSLO",
                                        ),
                                ),
                            ),
                        ),
                ),
            )
        private val land =
            KodeverkDto(
                mapOf(
                    "NOR" to
                        listOf(
                            BetydningDto(
                                LocalDate.MIN,
                                LocalDate.MAX,
                                mapOf(
                                    "nb" to
                                        BeskrivelseDto(
                                            "NORGE",
                                            "NORGE",
                                        ),
                                ),
                            ),
                        ),
                ),
            )
        private val kodeverkPoststed = Ressurs.success(poststed)
        private val kodeverkLand = Ressurs.success(land)
        private val kodeverkInntekt: Ressurs<InntektKodeverkDto> = Ressurs.success(emptyMap())

        private val arbeidsfordeling =
            Ressurs.success(listOf(Arbeidsfordelingsenhet("4489", "nerd-enhet")))

        private const val FØDSELSNUMMER = "23097825289"
        private val medl =
            Ressurs.success(
                Medlemskapsinfo(
                    personIdent = FØDSELSNUMMER,
                    gyldigePerioder = emptyList(),
                    uavklartePerioder = emptyList(),
                    avvistePerioder = emptyList(),
                ),
            )

        private val oppdatertJournalpostResponse =
            Ressurs.success(OppdaterJournalpostResponse(journalpostId = "1234"))
        private val arkiverDokumentResponse = Ressurs.success(ArkiverDokumentResponse(journalpostId = "1234", ferdigstilt = true))
        private val journalpostFraIntegrasjoner =
            Journalpost(
                avsenderMottaker = avsenderMottaker,
                journalpostId = "1234",
                journalposttype = Journalposttype.I,
                journalstatus = Journalstatus.MOTTATT,
                tema = "ENF",
                behandlingstema = "ab0071",
                tittel = "abrakadabra",
                bruker = Bruker(type = BrukerIdType.FNR, id = FØDSELSNUMMER),
                journalforendeEnhet = "4817",
                kanal = "SKAN_IM",
                relevanteDatoer = listOf(RelevantDato(LocalDateTime.now(), "DATO_REGISTRERT")),
                dokumenter =
                    listOf(
                        DokumentInfo(
                            dokumentInfoId = "123451",
                            tittel = "Søknad om overgangsstønad - dokument 1",
                            brevkode = DokumentBrevkode.OVERGANGSSTØNAD.verdi,
                            dokumentvarianter =
                                listOf(
                                    Dokumentvariant(variantformat = Dokumentvariantformat.ARKIV, saksbehandlerHarTilgang = true),
                                    Dokumentvariant(variantformat = Dokumentvariantformat.ORIGINAL, saksbehandlerHarTilgang = true),
                                ),
                        ),
                        DokumentInfo(
                            dokumentInfoId = "123452",
                            tittel = "Søknad om barnetilsyn - dokument 1",
                            brevkode = DokumentBrevkode.OVERGANGSSTØNAD.verdi,
                            dokumentvarianter =
                                listOf(Dokumentvariant(variantformat = Dokumentvariantformat.ARKIV, saksbehandlerHarTilgang = true)),
                        ),
                        DokumentInfo(
                            dokumentInfoId = "123453",
                            tittel = "Samboeravtale",
                            brevkode = DokumentBrevkode.OVERGANGSSTØNAD.verdi,
                            dokumentvarianter =
                                listOf(Dokumentvariant(variantformat = Dokumentvariantformat.ARKIV, saksbehandlerHarTilgang = true)),
                        ),
                        DokumentInfo(
                            dokumentInfoId = "123454",
                            tittel = "Manuelt skannet dokument",
                            brevkode = DokumentBrevkode.OVERGANGSSTØNAD.verdi,
                            dokumentvarianter =
                                listOf(Dokumentvariant(variantformat = Dokumentvariantformat.ARKIV, saksbehandlerHarTilgang = true)),
                            logiskeVedlegg =
                                listOf(
                                    LogiskVedlegg(
                                        logiskVedleggId = "1",
                                        tittel = "Manuelt skannet samværsavtale",
                                    ),
                                    LogiskVedlegg(
                                        logiskVedleggId = "2",
                                        tittel = "Annen fritekst fra gosys",
                                    ),
                                ),
                        ),
                        DokumentInfo(
                            dokumentInfoId = "123455",
                            tittel = "EtFrykteligLangtDokumentNavnSomTroligIkkeBrekkerOgØdeleggerGUI",
                            brevkode = DokumentBrevkode.OVERGANGSSTØNAD.verdi,
                            dokumentvarianter =
                                listOf(Dokumentvariant(variantformat = Dokumentvariantformat.ARKIV, saksbehandlerHarTilgang = true)),
                        ),
                        DokumentInfo(
                            dokumentInfoId = "123456",
                            tittel = "Søknad om overgangsstønad - dokument 2",
                            brevkode = DokumentBrevkode.OVERGANGSSTØNAD.verdi,
                            dokumentvarianter =
                                listOf(Dokumentvariant(variantformat = Dokumentvariantformat.ARKIV, saksbehandlerHarTilgang = true)),
                        ),
                        DokumentInfo(
                            dokumentInfoId = "123457",
                            tittel = "Søknad om overgangsstønad - dokument 3",
                            brevkode = DokumentBrevkode.OVERGANGSSTØNAD.verdi,
                            dokumentvarianter =
                                listOf(Dokumentvariant(variantformat = Dokumentvariantformat.ARKIV, saksbehandlerHarTilgang = true)),
                        ),
                    ),
            )

        private fun journalpostPapirsøknad(id: String) =
            Ressurs.success(
                Journalpost(
                    avsenderMottaker = avsenderMottaker,
                    journalpostId = id,
                    journalposttype = Journalposttype.I,
                    journalstatus = Journalstatus.MOTTATT,
                    tema = "ENF",
                    behandlingstema = "ab0071",
                    tittel = "abrakadabra",
                    bruker = Bruker(type = BrukerIdType.FNR, id = FØDSELSNUMMER),
                    journalforendeEnhet = "4817",
                    kanal = "SKAN_IM",
                    relevanteDatoer = listOf(RelevantDato(LocalDateTime.now(), "DATO_REGISTRERT")),
                    dokumenter =
                        listOf(
                            DokumentInfo(
                                dokumentInfoId = "12341",
                                tittel = "Søknad om overgangsstønad - dokument 1",
                                brevkode = DokumentBrevkode.OVERGANGSSTØNAD.verdi,
                                dokumentvarianter =
                                    listOf(Dokumentvariant(variantformat = Dokumentvariantformat.ARKIV, saksbehandlerHarTilgang = true)),
                                logiskeVedlegg =
                                    listOf(
                                        LogiskVedlegg("1", "Tittel logisk vedlegg"),
                                        LogiskVedlegg("2", "Annet logiskt vedlegg"),
                                    ),
                            ),
                        ),
                ),
            )

        private val journalpost = Ressurs.success(journalpostFraIntegrasjoner)
        private val journalposter = Ressurs.success(listOf(journalpostFraIntegrasjoner))
        private val navKontorEnhet =
            Ressurs.success(
                NavKontorEnhet(
                    enhetId = 100000194,
                    navn = "NAV Kristiansand",
                    enhetNr = "1001",
                    status = "Aktiv",
                ),
            )
    }
}
