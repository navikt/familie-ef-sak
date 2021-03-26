package no.nav.familie.ef.sak.no.nav.familie.ef.sak.service.steg

import io.mockk.*
import no.nav.familie.ef.sak.blankett.Blankett
import no.nav.familie.ef.sak.blankett.BlankettRepository
import no.nav.familie.ef.sak.blankett.BlankettSteg
import no.nav.familie.ef.sak.integration.JournalpostClient
import no.nav.familie.ef.sak.repository.BehandlingRepository
import no.nav.familie.ef.sak.repository.domain.*
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.ef.sak.service.ArbeidsfordelingService
import no.nav.familie.ef.sak.service.BehandlingService
import no.nav.familie.kontrakter.ef.sak.DokumentBrevkode
import no.nav.familie.kontrakter.felles.dokarkiv.ArkiverDokumentRequest
import no.nav.familie.kontrakter.felles.dokarkiv.ArkiverDokumentResponse
import no.nav.familie.kontrakter.felles.dokarkiv.FilType
import no.nav.familie.kontrakter.felles.journalpost.*
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

class JournalførBlankettStegTest {

    private val behandlingRepository = mockk<BehandlingRepository>()
    private val behandlingService = mockk<BehandlingService>()
    private val journalpostClient = mockk<JournalpostClient>()
    private val taskRepository = mockk<TaskRepository>()
    private val blankettRepository = mockk<BlankettRepository>()
    private val arbeidsfordelingService = mockk<ArbeidsfordelingService>()

    private val blankettSteg = BlankettSteg(behandlingService, behandlingRepository, journalpostClient, arbeidsfordelingService, blankettRepository, taskRepository)

    private lateinit var taskSlot: MutableList<Task>

    val fnr = "12345678901"
    val fagsak = Fagsak(stønadstype = Stønadstype.OVERGANGSSTØNAD,
        søkerIdenter = setOf(FagsakPerson(ident = fnr)))

    private val journalpost =
        Journalpost(journalpostId = "1234",
            journalposttype = Journalposttype.I,
            journalstatus = Journalstatus.MOTTATT,
            tema = "ENF",
            behandlingstema = "ab0071",
            tittel = "abrakadabra",
            bruker = Bruker(type = BrukerIdType.FNR, id = fnr),
            journalforendeEnhet = "4817",
            kanal = "SKAN_IM",
            sak = Sak("1234", "arkivsaksystem", fagsak.id.toString()),
            dokumenter =
            listOf(DokumentInfo(dokumentInfoId = "12345",
                tittel = "Tittel",
                brevkode = DokumentBrevkode.OVERGANGSSTØNAD.verdi,
                dokumentvarianter = listOf(Dokumentvariant(variantformat = "ARKIV"))))
        )

    private val behandling = Behandling(fagsakId = fagsak.id,
                                type = BehandlingType.BLANKETT,
                                status = BehandlingStatus.IVERKSETTER_VEDTAK,
                                steg = blankettSteg.stegType(),
                                resultat = BehandlingResultat.IKKE_SATT)

    private val behandlingJournalpost = Behandlingsjournalpost(behandling.id, journalpost.journalpostId, journalpost.journalposttype)

    private val pdf = "enPdF".toByteArray()

    @BeforeEach
    internal fun setup() {

        every {
            journalpostClient.hentJournalpost(any())
        } returns journalpost

        every {
            behandlingRepository.finnAktivIdent(any())
        } returns fnr

        every {
            journalpostClient.arkiverDokument(any())
        } returns ArkiverDokumentResponse("1", false)

        every {
            blankettRepository.findByIdOrThrow(any())
        } returns Blankett(behandling.id, Fil(pdf))

        every {
            taskRepository.save(any())
        } returns Task("", "", Properties())

        every {
            arbeidsfordelingService.hentNavEnhetIdEllerBrukMaskinellEnhetHvisNull(any())
        } returns "1234"

        every { behandlingService.hentBehandlingsjournalposter(behandling.id) } returns listOf(behandlingJournalpost)
    }

    @Test
    internal fun `skal arkivere dokument ved utførelse av blankett steg`() {

        val arkiverDokumentRequestSlot = slot<ArkiverDokumentRequest>()
        val journalpostIdSlot = slot<String>()
        val journalpostId = "12345678"

        every {
            journalpostClient.arkiverDokument(capture(arkiverDokumentRequestSlot))
        } returns ArkiverDokumentResponse(journalpostId, false)

        every {
            behandlingService.leggTilBehandlingsjournalpost(capture(journalpostIdSlot), any(), any())
        } just Runs

        blankettSteg.utførSteg(behandling, null)

        assertThat(arkiverDokumentRequestSlot.captured.fnr).isEqualTo(fnr)
        assertThat(arkiverDokumentRequestSlot.captured.fagsakId).isEqualTo(fagsak.id.toString())
        assertThat(arkiverDokumentRequestSlot.captured.hoveddokumentvarianter.first().dokument).isEqualTo(pdf)
        assertThat(arkiverDokumentRequestSlot.captured.hoveddokumentvarianter.first().filType).isEqualTo(FilType.PDFA)
        assertThat(journalpostIdSlot.captured).isEqualTo(journalpostId)
    }

}