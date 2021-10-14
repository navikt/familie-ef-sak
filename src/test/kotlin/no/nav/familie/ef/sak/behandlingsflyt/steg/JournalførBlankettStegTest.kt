package no.nav.familie.ef.sak.service.steg

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.ef.sak.blankett.Blankett
import no.nav.familie.ef.sak.blankett.BlankettRepository
import no.nav.familie.ef.sak.blankett.BlankettSteg
import no.nav.familie.ef.sak.journalføring.JournalpostClient
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.behandling.domain.Behandlingsjournalpost
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.fagsak.domain.FagsakPerson
import no.nav.familie.ef.sak.felles.domain.Fil
import no.nav.familie.ef.sak.fagsak.domain.Stønadstype
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.ef.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.vedtak.TotrinnskontrollService
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.ef.sak.DokumentBrevkode
import no.nav.familie.kontrakter.felles.BrukerIdType
import no.nav.familie.kontrakter.felles.dokarkiv.ArkiverDokumentResponse
import no.nav.familie.kontrakter.felles.dokarkiv.v2.ArkiverDokumentRequest
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Filtype
import no.nav.familie.kontrakter.felles.journalpost.Bruker
import no.nav.familie.kontrakter.felles.journalpost.DokumentInfo
import no.nav.familie.kontrakter.felles.journalpost.Dokumentvariant
import no.nav.familie.kontrakter.felles.journalpost.Dokumentvariantformat
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.familie.kontrakter.felles.journalpost.Journalposttype
import no.nav.familie.kontrakter.felles.journalpost.Journalstatus
import no.nav.familie.kontrakter.felles.journalpost.Sak
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Properties

class JournalførBlankettStegTest {

    private val behandlingRepository = mockk<BehandlingRepository>()
    private val behandlingService = mockk<BehandlingService>()
    private val journalpostClient = mockk<JournalpostClient>()
    private val taskRepository = mockk<TaskRepository>()
    private val blankettRepository = mockk<BlankettRepository>()
    private val arbeidsfordelingService = mockk<ArbeidsfordelingService>()
    private val totrinnskontrollService = mockk<TotrinnskontrollService>(relaxed = true)

    private val blankettSteg = BlankettSteg(behandlingService = behandlingService,
                                            behandlingRepository = behandlingRepository,
                                            journalpostClient = journalpostClient,
                                            arbeidsfordelingService = arbeidsfordelingService,
                                            blankettRepository = blankettRepository,
                                            totrinnskontrollService = totrinnskontrollService,
                                            taskRepository = taskRepository)

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
                                            dokumentvarianter = listOf(Dokumentvariant(variantformat = Dokumentvariantformat.ARKIV))))
            )

    private val behandling = Behandling(fagsakId = fagsak.id,
                                        type = BehandlingType.BLANKETT,
                                        status = BehandlingStatus.IVERKSETTER_VEDTAK,
                                        steg = blankettSteg.stegType(),
                                        resultat = BehandlingResultat.IKKE_SATT,
                                        årsak = BehandlingÅrsak.SØKNAD)

    private val behandlingJournalpost =
            Behandlingsjournalpost(behandling.id, journalpost.journalpostId, journalpost.journalposttype)

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
            journalpostClient.arkiverDokument(any(), null)
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
            journalpostClient.arkiverDokument(capture(arkiverDokumentRequestSlot), any())
        } returns ArkiverDokumentResponse(journalpostId, false)

        every {
            behandlingService.leggTilBehandlingsjournalpost(capture(journalpostIdSlot), any(), any())
        } just Runs

        blankettSteg.utførSteg(behandling, null)

        assertThat(arkiverDokumentRequestSlot.captured.fnr).isEqualTo(fnr)
        assertThat(arkiverDokumentRequestSlot.captured.fagsakId).isEqualTo(fagsak.id.toString())
        assertThat(arkiverDokumentRequestSlot.captured.hoveddokumentvarianter.first().dokument).isEqualTo(pdf)
        assertThat(arkiverDokumentRequestSlot.captured.hoveddokumentvarianter.first().filtype).isEqualTo(Filtype.PDFA)
        assertThat(journalpostIdSlot.captured).isEqualTo(journalpostId)
    }

}