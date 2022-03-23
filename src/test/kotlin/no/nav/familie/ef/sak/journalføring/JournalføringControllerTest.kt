package no.nav.familie.ef.sak.journalføring

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ef.sak.infrastruktur.exception.ApiFeil
import no.nav.familie.ef.sak.infrastruktur.exception.ManglerTilgang
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.ef.sak.journalføring.dto.JournalføringBehandling
import no.nav.familie.ef.sak.journalføring.dto.JournalføringRequest
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PdlClient
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdent
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdenter
import no.nav.familie.kontrakter.ef.sak.DokumentBrevkode
import no.nav.familie.kontrakter.felles.BrukerIdType
import no.nav.familie.kontrakter.felles.journalpost.Bruker
import no.nav.familie.kontrakter.felles.journalpost.DokumentInfo
import no.nav.familie.kontrakter.felles.journalpost.Dokumentvariant
import no.nav.familie.kontrakter.felles.journalpost.Dokumentvariantformat
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.familie.kontrakter.felles.journalpost.Journalposttype
import no.nav.familie.kontrakter.felles.journalpost.Journalstatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID
import kotlin.test.assertFailsWith

internal class JournalføringControllerTest {


    private val journalføringService = mockk<JournalføringService>()
    private val pdlClient = mockk<PdlClient>()
    private val tilgangService: TilgangService = mockk()
    private val featureToggleService: FeatureToggleService = mockk(relaxed = true)
    private val journalføringController =
            JournalføringController(journalføringService, pdlClient, tilgangService, featureToggleService)

    @BeforeEach
    internal fun setUp() {
        every {
            tilgangService.validerTilgangTilPersonMedBarn(any(), any())
        } just Runs
    }

    @Test
    internal fun `skal hente journalpost med personident utledet fra pdl`() {


        every {
            pdlClient.hentPersonidenter(aktørId)
        } returns PdlIdenter(listOf(PdlIdent(personIdentFraPdl, false)))

        every {
            journalføringService.hentJournalpost(any())
        } returns journalpostMedAktørId

        val journalpostResponse = journalføringController.hentJournalPost(journalpostId)
        assertThat(journalpostResponse.data?.personIdent).isEqualTo(personIdentFraPdl)
        assertThat(journalpostResponse.data?.journalpost?.journalpostId).isEqualTo(journalpostId)
    }

    @Test
    internal fun `skal hente journalpost med personident fra journalposten`() {

        every {
            journalføringService.hentJournalpost(any())
        } returns journalpostMedFødselsnummer

        val journalpostResponse = journalføringController.hentJournalPost(journalpostId)
        assertThat(journalpostResponse.data?.personIdent).isEqualTo(personIdentFraPdl)
        assertThat(journalpostResponse.data?.journalpost?.journalpostId).isEqualTo(journalpostId)
    }

    @Test
    internal fun `skal feile med ManglerTilgang hvis behandler ikke har tilgang person`() {

        every {
            journalføringService.hentJournalpost(any())
        } returns journalpostMedFødselsnummer

        every {
            tilgangService.validerTilgangTilPersonMedBarn(any(), any())
        } throws ManglerTilgang("Ingen tilgang")

        assertFailsWith<ManglerTilgang> {
            journalføringController.hentJournalPost(journalpostId)
        }
    }

    @Test
    internal fun `skal feile hvis journalpost mangler bruker`() {
        every {
            journalføringService.hentJournalpost(any())
        } returns journalpostUtenBruker

        assertThrows<IllegalStateException> { journalføringController.hentJournalPost(journalpostId) }
    }


    @Test
    internal fun `skal feile hvis journalpost har orgnr`() {
        every {
            journalføringService.hentJournalpost(any())
        } returns journalpostMedOrgnr

        assertThrows<IllegalStateException> { journalføringController.hentJournalPost(journalpostId) }
    }

    @Test
    internal fun `skal feile hvis bruker er veileder`() {
        every {
            journalføringService.hentJournalpost(any())
        } returns journalpostMedFødselsnummer

        every {
            tilgangService.validerHarSaksbehandlerrolle()
        } throws ManglerTilgang("Bruker mangler tilgang")

        assertThrows<ManglerTilgang> {
            journalføringController.fullførJournalpost(journalpostMedFødselsnummer.journalpostId,
                                                       JournalføringRequest(null,
                                                                            UUID.randomUUID(),
                                                                            "dummy-oppgave",
                                                                            JournalføringBehandling(UUID.randomUUID()),
                                                                            "9991"))
        }
    }

    @Test
    internal fun `skal kaste ApiFeil hvis vedlegget ikke inneholder dokumentvariant ARKIV`() {
        every {
            journalføringService.hentJournalpost(any())
        } returns journalpostMedFødselsnummer.copy(dokumenter = journalpostMedFødselsnummer.dokumenter!!.map {
            it.copy(dokumentvarianter = listOf(Dokumentvariant(Dokumentvariantformat.PRODUKSJON_DLF)))
        })

        assertThrows<ApiFeil> { journalføringController.hentDokument(journalpostId, dokumentInfoId) }
    }

    @Test
    internal fun `skal hente dokument med dokumentvariant ARKIV`() {
        every {
            journalføringService.hentJournalpost(any())
        } returns journalpostMedFødselsnummer.copy(dokumenter = journalpostMedFødselsnummer.dokumenter!!.map {
            it.copy(dokumentvarianter = listOf(Dokumentvariant(Dokumentvariantformat.PRODUKSJON_DLF),
                                               Dokumentvariant(Dokumentvariantformat.ARKIV)))
        })

        every { journalføringService.hentDokument(any(), any()) } returns byteArrayOf()

        journalføringController.hentDokument(journalpostId, dokumentInfoId)

        verify(exactly = 1) { journalføringService.hentDokument(journalpostId, dokumentInfoId) }
    }

    private val aktørId = "11111111111"
    private val personIdentFraPdl = "12345678901"
    private val journalpostId = "1234"
    private val dokumentInfoId = "12345"

    private val journalpostMedAktørId =
            Journalpost(journalpostId = journalpostId,
                        journalposttype = Journalposttype.I,
                        journalstatus = Journalstatus.MOTTATT,
                        tema = "ENF",
                        behandlingstema = "ab0071",
                        tittel = "abrakadabra",
                        bruker = Bruker(type = BrukerIdType.AKTOERID, id = aktørId),
                        journalforendeEnhet = "4817",
                        kanal = "SKAN_IM",
                        dokumenter =
                        listOf(DokumentInfo(dokumentInfoId = dokumentInfoId,
                                            tittel = "Tittel",
                                            brevkode = DokumentBrevkode.OVERGANGSSTØNAD.verdi,
                                            dokumentvarianter = listOf(Dokumentvariant(Dokumentvariantformat.ARKIV))))
            )

    private val journalpostMedFødselsnummer =
            journalpostMedAktørId.copy(bruker = Bruker(type = BrukerIdType.FNR, id = personIdentFraPdl))
    private val journalpostUtenBruker = journalpostMedAktørId.copy(bruker = null)
    private val journalpostMedOrgnr = journalpostMedAktørId.copy(bruker = Bruker(type = BrukerIdType.ORGNR, id = "12345"))

}