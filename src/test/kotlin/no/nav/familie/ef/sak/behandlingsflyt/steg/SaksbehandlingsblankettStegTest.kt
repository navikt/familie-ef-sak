package no.nav.familie.ef.sak.behandlingsflyt.steg

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ef.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.blankett.BlankettService
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.journalføring.JournalpostClient
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.fagsakpersoner
import no.nav.familie.ef.sak.repository.saksbehandling
import no.nav.familie.ef.sak.vedtak.TotrinnskontrollService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.dokarkiv.ArkiverDokumentResponse
import no.nav.familie.kontrakter.felles.dokarkiv.Dokumenttype
import no.nav.familie.kontrakter.felles.dokarkiv.v2.ArkiverDokumentRequest
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.familie.kontrakter.felles.journalpost.Journalposttype
import no.nav.familie.kontrakter.felles.journalpost.Journalstatus
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.restklient.client.RessursException
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException

internal class SaksbehandlingsblankettStegTest {
    private val blankettServiceMock = mockk<BlankettService>()
    private val taskServiceMock = mockk<TaskService>()
    private val arbeidsfordelingServiceMock = mockk<ArbeidsfordelingService>()
    private val totrinnskontrollServiceMock = mockk<TotrinnskontrollService>()
    private val journalpostClientMock = mockk<JournalpostClient>()
    private val behandlingServiceMock = mockk<BehandlingService>()
    private val fagsakServiceMock = mockk<FagsakService>()

    private val saksbehandlingsblankettSteg =
        SaksbehandlingsblankettSteg(
            blankettService = blankettServiceMock,
            taskService = taskServiceMock,
            arbeidsfordelingService = arbeidsfordelingServiceMock,
            totrinnskontrollService = totrinnskontrollServiceMock,
            journalpostClient = journalpostClientMock,
            behandlingService = behandlingServiceMock,
            fagsakService = fagsakServiceMock,
        )

    val arkiverDokumentRequestSlot = slot<ArkiverDokumentRequest>()

    @BeforeEach
    internal fun setUp() {
        every { blankettServiceMock.lagBlankett(any()) } returns "123".toByteArray()
        val arkiverDokumentResponse =
            ArkiverDokumentResponse(journalpostId = "12341234", ferdigstilt = true, dokumenter = listOf())
        every {
            journalpostClientMock.arkiverDokument(
                capture(arkiverDokumentRequestSlot),
                any(),
            )
        } returns arkiverDokumentResponse
        every { arbeidsfordelingServiceMock.hentNavEnhetIdEllerBrukMaskinellEnhetHvisNull(any()) } returns "4489"
        every { totrinnskontrollServiceMock.hentBeslutter(any()) } returns "BeslutterPerson"
        every { behandlingServiceMock.leggTilBehandlingsjournalpost(any(), any(), any()) } just Runs
        every { taskServiceMock.save(any()) } answers { firstArg() }
    }

    @Test
    internal fun `skal opprette, lagre og arkivere blankett for førstegangsbehandling`() {
        every { fagsakServiceMock.hentFagsak(any()) } returns fagsak(fagsakpersoner(setOf("12345678912")))
        val behandling = saksbehandling(type = BehandlingType.FØRSTEGANGSBEHANDLING)
        saksbehandlingsblankettSteg.utførSteg(behandling, null)
        verify(exactly = 1) { blankettServiceMock.lagBlankett(any()) }
        verify(exactly = 1) { journalpostClientMock.arkiverDokument(any(), any()) }
        verify(exactly = 0) { journalpostClientMock.finnJournalposter(any()) }
    }

    @Test
    internal fun `skal journalføre blankett for overgangsstønad hvis det er revurdering`() {
        every { fagsakServiceMock.hentFagsak(any()) } returns fagsak(fagsakpersoner(setOf("12345678912")))
        val behandling =
            saksbehandling(type = BehandlingType.REVURDERING).copy(stønadstype = StønadType.OVERGANGSSTØNAD)
        saksbehandlingsblankettSteg.utførSteg(behandling, null)
        verify(exactly = 1) { blankettServiceMock.lagBlankett(any()) }
        verify(exactly = 1) { journalpostClientMock.arkiverDokument(any(), any()) }
        arkiverDokumentRequestSlot.captured.hoveddokumentvarianter.forEach {
            Assertions.assertThat(it.dokumenttype).isEqualTo(Dokumenttype.OVERGANGSSTØNAD_BLANKETT_SAKSBEHANDLING)
        }
    }

    @Test
    internal fun `skal journalføre blankett for barnetilsyn hvis det er revurdering`() {
        every { fagsakServiceMock.hentFagsak(any()) } returns
            fagsak(
                fagsakpersoner(setOf("12345678912")),
                stønadstype = StønadType.BARNETILSYN,
            )
        val behandling = saksbehandling(type = BehandlingType.REVURDERING).copy(stønadstype = StønadType.BARNETILSYN)
        saksbehandlingsblankettSteg.utførSteg(behandling, null)
        verify(exactly = 1) { blankettServiceMock.lagBlankett(any()) }
        verify(exactly = 1) { journalpostClientMock.arkiverDokument(any(), any()) }
        arkiverDokumentRequestSlot.captured.hoveddokumentvarianter.forEach {
            Assertions.assertThat(it.dokumenttype).isEqualTo(Dokumenttype.BARNETILSYN_BLANKETT_SAKSBEHANDLING)
        }
    }

    @Test
    internal fun `skal håndtere 409 Conflict ved journalføring av  blankett`() {
        every { fagsakServiceMock.hentFagsak(any()) } returns
            fagsak(
                fagsakpersoner(setOf("12345678912")),
                stønadstype = StønadType.BARNETILSYN,
            )

        every {
            journalpostClientMock.arkiverDokument(
                capture(arkiverDokumentRequestSlot),
                any(),
            )
        } throws
            RessursException(
                Ressurs.failure(),
                HttpClientErrorException.create(HttpStatus.CONFLICT, "status=${HttpStatus.CONFLICT}", HttpHeaders(), null, null),
                HttpStatus.CONFLICT,
            )

        val behandling = saksbehandling(type = BehandlingType.REVURDERING).copy(stønadstype = StønadType.BARNETILSYN)
        every {
            journalpostClientMock.finnJournalposter(any())
        } returns
            listOf(
                Journalpost(
                    journalpostId = "123456",
                    journalposttype = Journalposttype.N,
                    journalstatus = Journalstatus.JOURNALFOERT,
                    eksternReferanseId = "${behandling.id}-blankett",
                ),
            )

        saksbehandlingsblankettSteg.utførSteg(behandling, null)
        verify(exactly = 1) { blankettServiceMock.lagBlankett(any()) }
        verify(exactly = 1) { journalpostClientMock.arkiverDokument(any(), any()) }
        verify(exactly = 1) { journalpostClientMock.finnJournalposter(any()) }
        verify(exactly = 1) {
            behandlingServiceMock.leggTilBehandlingsjournalpost(
                journalpostId = "123456",
                journalposttype = Journalposttype.N,
                behandlingId = behandling.id,
            )
        }
    }

    @Test
    internal fun `skal ikke håndtere feil som ikke er av typen 409 conflict ved arkivering av blankett`() {
        every { fagsakServiceMock.hentFagsak(any()) } returns
            fagsak(
                fagsakpersoner(setOf("12345678912")),
                stønadstype = StønadType.BARNETILSYN,
            )

        every {
            journalpostClientMock.arkiverDokument(
                capture(arkiverDokumentRequestSlot),
                any(),
            )
        } throws
            RessursException(
                Ressurs.failure(),
                HttpClientErrorException.create(HttpStatus.BAD_REQUEST, "status=${HttpStatus.BAD_REQUEST}", HttpHeaders(), null, null),
                HttpStatus.BAD_REQUEST,
            )

        val behandling = saksbehandling(type = BehandlingType.REVURDERING).copy(stønadstype = StønadType.BARNETILSYN)

        val feil = assertThrows<RessursException> { saksbehandlingsblankettSteg.utførSteg(behandling, null) }
        assertThat(feil.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST)
    }
}
