package no.nav.familie.ef.sak.journalføring

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import no.nav.familie.ef.sak.infrastruktur.config.PdlClientConfig.Companion.lagPersonKort
import no.nav.familie.ef.sak.infrastruktur.exception.ApiFeil
import no.nav.familie.ef.sak.infrastruktur.exception.ManglerTilgang
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.ef.sak.journalføring.dto.JournalføringRequestV2
import no.nav.familie.ef.sak.journalføring.dto.Journalføringsaksjon
import no.nav.familie.ef.sak.journalføring.dto.Journalføringsårsak
import no.nav.familie.ef.sak.journalføring.dto.OppdaterJournalpostMedDokumenterRequest
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdent
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdenter
import no.nav.familie.kontrakter.ef.sak.DokumentBrevkode
import no.nav.familie.kontrakter.felles.BrukerIdType
import no.nav.familie.kontrakter.felles.dokarkiv.OppdaterJournalpostResponse
import no.nav.familie.kontrakter.felles.journalpost.AvsenderMottaker
import no.nav.familie.kontrakter.felles.journalpost.AvsenderMottakerIdType
import no.nav.familie.kontrakter.felles.journalpost.Bruker
import no.nav.familie.kontrakter.felles.journalpost.DokumentInfo
import no.nav.familie.kontrakter.felles.journalpost.Dokumentvariant
import no.nav.familie.kontrakter.felles.journalpost.Dokumentvariantformat
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.familie.kontrakter.felles.journalpost.Journalposttype
import no.nav.familie.kontrakter.felles.journalpost.Journalstatus
import no.nav.familie.kontrakter.felles.journalpost.LogiskVedlegg
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertFailsWith

internal class JournalpostControllerTest {
    private val journalføringService = mockk<JournalføringService>()
    private val journalføringKlageService = mockk<JournalføringKlageService>()
    private val journalpostClient = mockk<JournalpostClient>()
    private val personService = mockk<PersonService>()
    private val tilgangService: TilgangService = mockk()
    private val featureToggleService: FeatureToggleService = mockk(relaxed = true)

    private val journalpostService = JournalpostService(journalpostClient)

    private val journalpostController =
        JournalpostController(
            journalføringService,
            journalføringKlageService,
            journalpostService,
            personService,
            tilgangService,
            featureToggleService,
        )

    @BeforeEach
    internal fun setUp() {
        every {
            tilgangService.validerTilgangTilPersonMedBarn(any(), any())
        } just Runs
        every { personService.hentPersonKortBolk(any()) } answers {
            firstArg<List<String>>().associateWith { lagPersonKort(it) }
        }
    }

    @Test
    internal fun `skal hente journalpost med personident utledet fra pdl`() {
        every {
            personService.hentPersonIdenter(aktørId)
        } returns PdlIdenter(listOf(PdlIdent(personIdentFraPdl, false)))

        every {
            journalpostService.hentJournalpost(any())
        } returns journalpostMedAktørId

        val journalpostResponse = journalpostController.hentJournalPost(journalpostId)

        assertThat(journalpostResponse.data?.personIdent).isEqualTo(personIdentFraPdl)
        assertThat(journalpostResponse.data?.journalpost?.journalpostId).isEqualTo(journalpostId)
        verify(exactly = 1) { personService.hentPersonKortBolk(any()) }
    }

    @Test
    internal fun `hentJournalpost skal ikke hente person fra pdl hvis avsender er lik bruker `() {
        every {
            personService.hentPersonIdenter(aktørId)
        } returns PdlIdenter(listOf(PdlIdent(personIdentFraPdl, false)))
        every {
            journalpostService.hentJournalpost(any())
        } returns journalpostMedAktørId.copy(avsenderMottaker = avsenderMottaker())

        val journalpostResponse = journalpostController.hentJournalPost(journalpostId)

        assertThat(journalpostResponse.data?.personIdent).isEqualTo(personIdentFraPdl)
        verify(exactly = 0) { personService.hentPersonKortBolk(any()) }
    }

    @Test
    internal fun `skal hente journalpost med personident fra journalposten`() {
        every {
            journalpostService.hentJournalpost(any())
        } returns journalpostMedFødselsnummer

        val journalpostResponse = journalpostController.hentJournalPost(journalpostId)

        assertThat(journalpostResponse.data?.personIdent).isEqualTo(personIdentFraPdl)
        assertThat(journalpostResponse.data?.journalpost?.journalpostId).isEqualTo(journalpostId)
    }

    @Test
    internal fun `skal feile med ManglerTilgang hvis behandler ikke har tilgang person`() {
        every {
            journalpostService.hentJournalpost(any())
        } returns journalpostMedFødselsnummer

        every {
            tilgangService.validerTilgangTilPersonMedBarn(any(), any())
        } throws ManglerTilgang("Ingen tilgang", "Mangler tilgang til bruker")

        assertFailsWith<ManglerTilgang> {
            journalpostController.hentJournalPost(journalpostId)
        }
    }

    @Test
    internal fun `skal feile hvis journalpost mangler bruker`() {
        every {
            journalpostService.hentJournalpost(any())
        } returns journalpostUtenBruker

        assertThrows<IllegalStateException> { journalpostController.hentJournalPost(journalpostId) }
    }

    @Test
    internal fun `skal feile hvis journalpost har orgnr`() {
        every {
            journalpostService.hentJournalpost(any())
        } returns journalpostMedOrgnr

        assertThrows<IllegalStateException> { journalpostController.hentJournalPost(journalpostId) }
    }

    @Test
    internal fun `skal feile hvis bruker er veileder`() {
        every {
            journalpostService.hentJournalpost(any())
        } returns journalpostMedFødselsnummer

        every {
            tilgangService.validerHarSaksbehandlerrolle()
        } throws ManglerTilgang("Bruker mangler tilgang", "Mangler tilgang til bruker")

        assertThrows<ManglerTilgang> {
            journalpostController.fullførJournalpostV2(
                journalpostMedFødselsnummer.journalpostId,
                JournalføringRequestV2(
                    dokumentTitler = null,
                    fagsakId = UUID.randomUUID(),
                    oppgaveId = "dummy-oppgave",
                    journalførendeEnhet = "9991",
                    aksjon = Journalføringsaksjon.OPPRETT_BEHANDLING,
                    årsak = Journalføringsårsak.DIGITAL_SØKNAD,
                ),
            )
        }
    }

    @Test
    fun `skal journalføre som klage i v2 hvis årsak er klage`() {
        setupFullførJournalføringV2()

        val journalføringRequest = opprettJournalføringRequestV2(årsak = Journalføringsårsak.KLAGE)
        journalpostController.fullførJournalpostV2(journalpostId, journalføringRequest)
        verify(exactly = 1) { journalføringKlageService.fullførJournalpostV2(journalføringRequest, any()) }
    }

    @Test
    fun `skal journalføre som klage i v2 hvis årsak er klage_tilbakekreving`() {
        setupFullførJournalføringV2()

        val journalføringRequest = opprettJournalføringRequestV2(årsak = Journalføringsårsak.KLAGE_TILBAKEKREVING)
        journalpostController.fullførJournalpostV2(journalpostId, journalføringRequest)
        verify(exactly = 1) { journalføringKlageService.fullførJournalpostV2(journalføringRequest, any()) }
    }

    @Test
    fun `skal journalføre som vanlig i v2 hvis årsaker ikke er klage`() {
        setupFullførJournalføringV2()

        listOf(
            Journalføringsårsak.PAPIRSØKNAD,
            Journalføringsårsak.DIGITAL_SØKNAD,
            Journalføringsårsak.ETTERSENDING,
        ).forEach {
            val journalføringRequest = opprettJournalføringRequestV2(årsak = it)
            journalpostController.fullførJournalpostV2(journalpostId, journalføringRequest)
            verify(exactly = 1) { journalføringService.fullførJournalpostV2(journalføringRequest, any()) }
        }
    }

    private fun setupFullførJournalføringV2() {
        every { journalpostService.hentJournalpost(any()) } returns journalpostMedFødselsnummer
        every { tilgangService.validerHarSaksbehandlerrolle() } just Runs
        every { journalføringKlageService.fullførJournalpostV2(any(), any()) } just Runs
        every { journalføringService.fullførJournalpostV2(any(), any()) } returns 1L
    }

    private fun opprettJournalføringRequestV2(årsak: Journalføringsårsak) =
        JournalføringRequestV2(
            dokumentTitler = emptyMap(),
            logiskeVedlegg = emptyMap(),
            fagsakId = UUID.randomUUID(),
            oppgaveId = "21345",
            journalførendeEnhet = "9999",
            årsak = årsak,
            aksjon = Journalføringsaksjon.OPPRETT_BEHANDLING,
            mottattDato = LocalDate.now(),
        )

    @Nested
    inner class HentDokument {
        @Test
        internal fun `skal kaste ApiFeil hvis vedlegget ikke inneholder dokumentvariant ARKIV`() {
            every {
                journalpostService.hentJournalpost(any())
            } returns
                journalpostMedFødselsnummer.copy(
                    dokumenter =
                        journalpostMedFødselsnummer.dokumenter!!.map {
                            it.copy(
                                dokumentvarianter =
                                    listOf(
                                        Dokumentvariant(
                                            Dokumentvariantformat.PRODUKSJON_DLF,
                                            saksbehandlerHarTilgang = true,
                                        ),
                                    ),
                            )
                        },
                )

            assertThrows<ApiFeil> { journalpostController.hentDokument(journalpostId, dokumentInfoId) }
        }

        @Test
        internal fun `skal kunne hente dokument med dokumentvariant ARKIV`() {
            every {
                journalpostService.hentJournalpost(any())
            } returns
                journalpostMedFødselsnummer.copy(
                    dokumenter =
                        journalpostMedFødselsnummer.dokumenter!!.map {
                            it.copy(
                                dokumentvarianter =
                                    listOf(
                                        Dokumentvariant(Dokumentvariantformat.PRODUKSJON_DLF, saksbehandlerHarTilgang = true),
                                        Dokumentvariant(Dokumentvariantformat.ARKIV, saksbehandlerHarTilgang = true),
                                    ),
                            )
                        },
                )

            every { journalpostService.hentDokument(any(), any()) } returns byteArrayOf()

            journalpostController.hentDokument(journalpostId, dokumentInfoId)

            verify(exactly = 1) { journalpostService.hentDokument(journalpostId, dokumentInfoId) }
        }
    }

    @Nested
    inner class OppdaterDokumenterPåJournalpost {
        @Test
        internal fun `må enten endre på dokumenttittel eller logiske vedlegg`() {
            every { journalpostService.hentJournalpost("1234") } returns journalpostMedFødselsnummer
            every { tilgangService.validerHarSaksbehandlerrolle() } just Runs

            val request = OppdaterJournalpostMedDokumenterRequest(null, null)

            val response = assertThrows<ApiFeil> { journalpostController.oppdaterDokumenter(journalpostId, request) }

            assertThat(response.message).isEqualTo("Mangler både dokumenttittler og logiske vedlegg i forbindelse med oppdatering av dokumenter til journalpost")
        }

        @Test
        internal fun `skal ikke kunne endre dokumenttittel til tom streng`() {
            every { journalpostService.hentJournalpost("1234") } returns journalpostMedFødselsnummer
            every { tilgangService.validerHarSaksbehandlerrolle() } just Runs

            val nyeTitler = mapOf("DokumentId" to "")
            val request = OppdaterJournalpostMedDokumenterRequest(nyeTitler, null)

            val response = assertThrows<ApiFeil> { journalpostController.oppdaterDokumenter(journalpostId, request) }

            assertThat(response.message).isEqualTo("Kan ikke endre dokumenttittel til tom streng")
        }

        @Test
        internal fun `id til endret dokument kan ikke være tom streng`() {
            every { journalpostService.hentJournalpost("1234") } returns journalpostMedFødselsnummer
            every { tilgangService.validerHarSaksbehandlerrolle() } just Runs

            val nyeTitler = mapOf("" to "Dokumenttittel")
            val request = OppdaterJournalpostMedDokumenterRequest(nyeTitler, null)

            val response = assertThrows<ApiFeil> { journalpostController.oppdaterDokumenter(journalpostId, request) }

            assertThat(response.message).isEqualTo("Mangler dokumentId på et eller flere dokumenter som skal endre tittel")
        }

        @Test
        internal fun `dokumentId til endredde logiske vedlegg kan ikke være tom streng`() {
            every { journalpostService.hentJournalpost("1234") } returns journalpostMedFødselsnummer
            every { tilgangService.validerHarSaksbehandlerrolle() } just Runs

            val nyeTitler = mapOf("DokumentId" to "Dokumenttittel")
            val nyeLogiskeVedlegg = mapOf("" to listOf<LogiskVedlegg>())
            val request = OppdaterJournalpostMedDokumenterRequest(nyeTitler, nyeLogiskeVedlegg)

            val response = assertThrows<ApiFeil> { journalpostController.oppdaterDokumenter(journalpostId, request) }

            assertThat(response.message).isEqualTo("Mangler dokumentId på et eller flere logiske vedlegg")
        }

        @Test
        internal fun `id til endret logisk vedlegg kan ikke være tom streng`() {
            every { journalpostService.hentJournalpost("1234") } returns journalpostMedFødselsnummer
            every { tilgangService.validerHarSaksbehandlerrolle() } just Runs

            val nyeTitler = mapOf("DokumentId" to "Dokumenttittel")
            val nyeLogiskeVedlegg = mapOf("DokumentId" to listOf(LogiskVedlegg("id_1", "Tittel 1"), LogiskVedlegg("", "Tittel 2")))
            val request = OppdaterJournalpostMedDokumenterRequest(nyeTitler, nyeLogiskeVedlegg)

            val response = assertThrows<ApiFeil> { journalpostController.oppdaterDokumenter(journalpostId, request) }

            assertThat(response.message).isEqualTo("Mangler id på et eller flere logiske vedlegg")
        }

        @Test
        internal fun `kan ikke endre et logisk vedlegg til tom streng`() {
            every { journalpostService.hentJournalpost("1234") } returns journalpostMedFødselsnummer
            every { tilgangService.validerHarSaksbehandlerrolle() } just Runs

            val nyeTitler = mapOf("DokumentId" to "Dokumenttittel")
            val nyeLogiskeVedlegg = mapOf("DokumentId" to listOf(LogiskVedlegg("id_1", "Tittel 1"), LogiskVedlegg("id_2", "")))
            val request = OppdaterJournalpostMedDokumenterRequest(nyeTitler, nyeLogiskeVedlegg)

            val response = assertThrows<ApiFeil> { journalpostController.oppdaterDokumenter(journalpostId, request) }

            assertThat(response.message).isEqualTo("Kan ikke endre et eller flere logiske vedlegg til tom streng")
        }

        @Test
        internal fun `skal kunne oppdatere dokumenttittel på journalpost`() {
            every { journalpostService.hentJournalpost("1234") } returns journalpostMedFødselsnummer
            every { tilgangService.validerHarSaksbehandlerrolle() } just Runs
            every { journalpostClient.oppdaterJournalpost(any(), any(), any()) } returns OppdaterJournalpostResponse()
            mockkObject(SikkerhetContext)
            every { SikkerhetContext.hentSaksbehandler() } returns "bob"

            val nyeTitler = mapOf("DokumentId" to "Dokumenttittel")
            val request = OppdaterJournalpostMedDokumenterRequest(nyeTitler, null)

            val response = journalpostController.oppdaterDokumenter(journalpostId, request)

            assertThat(response.data).isEqualTo(journalpostId)
            verify(exactly = 0) { journalpostClient.oppdaterLogiskeVedlegg(any(), any()) }
            verify(exactly = 1) { journalpostClient.oppdaterJournalpost(any(), any(), any()) }
            unmockkObject(SikkerhetContext)
        }

        @Test
        internal fun `skal kunne oppdatere logiske vedlegg til et dokument på journalpost`() {
            every { journalpostService.hentJournalpost("1234") } returns journalpostMedEtDokument
            every { tilgangService.validerHarSaksbehandlerrolle() } just Runs
            every { journalpostClient.oppdaterLogiskeVedlegg(any(), any()) } returns ""

            val nyeLogiskeVedlegg =
                mapOf(
                    "DokumentId" to
                        listOf(
                            LogiskVedlegg("id_1", "Tittel 1"),
                            LogiskVedlegg("id_2", "Tittel 2"),
                            LogiskVedlegg("id_3", "Tittel 3"),
                        ),
                )
            val request = OppdaterJournalpostMedDokumenterRequest(null, nyeLogiskeVedlegg)

            val response = journalpostController.oppdaterDokumenter(journalpostId, request)

            assertThat(response.data).isEqualTo(journalpostId)
            verify(exactly = 0) { journalpostClient.oppdaterJournalpost(any(), any(), any()) }
            verify(exactly = 1) { journalpostClient.oppdaterLogiskeVedlegg(any(), any()) }
            unmockkObject(SikkerhetContext)
        }

        @Test
        internal fun `skal kunne oppdatere logiske vedlegg til flere dokument på journalpost`() {
            every { journalpostService.hentJournalpost("1234") } returns journalpostMedFlereDokumenter
            every { tilgangService.validerHarSaksbehandlerrolle() } just Runs
            every { journalpostClient.oppdaterLogiskeVedlegg(any(), any()) } returns ""

            val nyeLogiskeVedlegg =
                mapOf(
                    "DokumentId_1" to listOf(LogiskVedlegg("id_1", "Tittel 1")),
                    "DokumentId_2" to listOf(LogiskVedlegg("id_2", "Tittel 2")),
                    "DokumentId_3" to listOf(LogiskVedlegg("id_3", "Tittel 3")),
                )
            val request = OppdaterJournalpostMedDokumenterRequest(null, nyeLogiskeVedlegg)

            val response = journalpostController.oppdaterDokumenter(journalpostId, request)

            assertThat(response.data).isEqualTo(journalpostId)
            verify(exactly = 0) { journalpostClient.oppdaterJournalpost(any(), any(), any()) }
            verify(exactly = 3) { journalpostClient.oppdaterLogiskeVedlegg(any(), any()) }
            unmockkObject(SikkerhetContext)
        }
    }

    private fun avsenderMottaker(erLikBruker: Boolean = true) =
        AvsenderMottaker(
            id = "1",
            type = AvsenderMottakerIdType.FNR,
            navn = "navn",
            land = "land",
            erLikBruker = erLikBruker,
        )

    private val aktørId = "11111111111"
    private val personIdentFraPdl = "12345678901"
    private val journalpostId = "1234"
    private val dokumentInfoId = "12345"

    private val journalpostMedAktørId =
        Journalpost(
            journalpostId = journalpostId,
            journalposttype = Journalposttype.I,
            journalstatus = Journalstatus.MOTTATT,
            tema = "ENF",
            behandlingstema = "ab0071",
            tittel = "abrakadabra",
            bruker = Bruker(type = BrukerIdType.AKTOERID, id = aktørId),
            journalforendeEnhet = "4817",
            kanal = "SKAN_IM",
            dokumenter =
                listOf(
                    DokumentInfo(
                        dokumentInfoId = dokumentInfoId,
                        tittel = "Tittel",
                        brevkode = DokumentBrevkode.OVERGANGSSTØNAD.verdi,
                        dokumentvarianter = listOf(Dokumentvariant(Dokumentvariantformat.ARKIV, saksbehandlerHarTilgang = true)),
                    ),
                ),
        )

    private val journalpostMedFødselsnummer =
        journalpostMedAktørId.copy(bruker = Bruker(type = BrukerIdType.FNR, id = personIdentFraPdl))
    private val journalpostMedEtDokument =
        journalpostMedAktørId.copy(dokumenter = listOf(DokumentInfo(dokumentInfoId = "DokumentId")), bruker = Bruker(type = BrukerIdType.FNR, id = personIdentFraPdl))
    private val journalpostMedFlereDokumenter = journalpostMedEtDokument.copy(dokumenter = listOf(DokumentInfo(dokumentInfoId = "DokumentId_1"), DokumentInfo(dokumentInfoId = "DokumentId_2"), DokumentInfo(dokumentInfoId = "DokumentId_3")))
    private val journalpostUtenBruker = journalpostMedAktørId.copy(bruker = null)
    private val journalpostMedOrgnr =
        journalpostMedAktørId.copy(bruker = Bruker(type = BrukerIdType.ORGNR, id = "12345"))
}
