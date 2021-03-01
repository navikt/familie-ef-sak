package no.nav.familie.ef.sak.no.nav.familie.ef.sak.service.steg

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.ef.sak.integration.JournalpostClient
import no.nav.familie.ef.sak.repository.BehandlingRepository
import no.nav.familie.ef.sak.repository.domain.*
import no.nav.familie.ef.sak.service.steg.BlankettSteg
import no.nav.familie.kontrakter.ef.sak.DokumentBrevkode
import no.nav.familie.kontrakter.felles.dokarkiv.ArkiverDokumentRequest
import no.nav.familie.kontrakter.felles.dokarkiv.OppdaterJournalpostResponse
import no.nav.familie.kontrakter.felles.journalpost.*
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class JournalførBlankettStegTest {

    private val behandlingRepository = mockk<BehandlingRepository>()
    private val journalpostClient = mockk<JournalpostClient>()
    private val taskRepository = mockk<TaskRepository>()

    private val blankettSteg = BlankettSteg(behandlingRepository, journalpostClient, taskRepository)

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

    private val behandlingJournalpost = Behandlingsjournalpost(journalpost.journalpostId, journalpost.journalposttype)

    @BeforeEach
    internal fun setup() {

        every {
            journalpostClient.hentJournalpost(any())
        } returns journalpost

        every {
            behandlingRepository.finnGjeldendeIdentForBehandling(any())
        } returns fnr

        every {
            journalpostClient.arkiverDokument(any())
        } returns OppdaterJournalpostResponse("1")
    }

    @Test
    internal fun `skal arkivere dokument ved utførelse av blankett steg`() {

        val arkiverDokumentRequestSlot = slot<ArkiverDokumentRequest>()
        every {
            journalpostClient.arkiverDokument(capture(arkiverDokumentRequestSlot))
        } returns OppdaterJournalpostResponse("1234")


        blankettSteg.utførSteg(Behandling(fagsakId = fagsak.id,
                                          type = BehandlingType.BLANKETT,
                                          status = BehandlingStatus.IVERKSETTER_VEDTAK,
                                          steg = blankettSteg.stegType(),
                                          journalposter = setOf(behandlingJournalpost),
                                          resultat = BehandlingResultat.IKKE_SATT), null)

        Assertions.assertThat(arkiverDokumentRequestSlot.captured.fnr).isEqualTo(fnr)
        Assertions.assertThat(arkiverDokumentRequestSlot.captured.fagsakId).isEqualTo(fagsak.id.toString())
    }

}