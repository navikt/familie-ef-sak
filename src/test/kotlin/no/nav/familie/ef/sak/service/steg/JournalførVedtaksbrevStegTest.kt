package no.nav.familie.ef.sak.no.nav.familie.ef.sak.service.steg

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ef.sak.repository.domain.Behandling
import no.nav.familie.ef.sak.repository.domain.BehandlingResultat
import no.nav.familie.ef.sak.repository.domain.BehandlingStatus
import no.nav.familie.ef.sak.repository.domain.BehandlingType
import no.nav.familie.ef.sak.repository.domain.Fagsak
import no.nav.familie.ef.sak.repository.domain.FagsakPerson
import no.nav.familie.ef.sak.repository.domain.Stønadstype
import no.nav.familie.ef.sak.service.BehandlingService
import no.nav.familie.ef.sak.service.VedtaksbrevIverksettingService
import no.nav.familie.ef.sak.service.steg.JournalførVedtaksbrevSteg
import no.nav.familie.ef.sak.task.DistribuerVedtaksbrevTask
import no.nav.familie.kontrakter.felles.journalpost.Journalposttype
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.Properties

internal class JournalførVedtaksbrevStegTest {

    private val taskRepository = mockk<TaskRepository>()
    private val vedtaksbrevService = mockk<VedtaksbrevIverksettingService>()
    private val behandlingService = mockk<BehandlingService>()
    val journalførVedtaksbrev = JournalførVedtaksbrevSteg(taskRepository, vedtaksbrevService, behandlingService)

    val fnr = "12345678901"
    val fagsak = Fagsak(stønadstype = Stønadstype.OVERGANGSSTØNAD,
                        søkerIdenter = setOf(FagsakPerson(ident = fnr)))
    val behandling = Behandling(fagsakId = fagsak.id,
                                type = BehandlingType.FØRSTEGANGSBEHANDLING,
                                status = BehandlingStatus.IVERKSETTER_VEDTAK,
                                steg = journalførVedtaksbrev.stegType(),
                                resultat = BehandlingResultat.IKKE_SATT)
    val journalpostId = "12345"

    @Test
    internal fun `skal opprette distribuerVedtaksbrevTask etter journalføring av vedtaksbrev`() {
        val taskSlot = slot<Task>()
        every {
            taskRepository.save(capture(taskSlot))
        } returns Task("", "", Properties())

        every { vedtaksbrevService.journalførVedtaksbrev(any()) } returns journalpostId
        every { behandlingService.leggTilBehandlingsjournalpost(any(), Journalposttype.U, behandling.id) } just Runs

        journalførVedtaksbrev.utførSteg(behandling, null)

        assertThat(taskSlot.captured.type).isEqualTo(DistribuerVedtaksbrevTask.TYPE)
    }

    @Test
    internal fun `skal journalføre vedtaksbrev ved utførelse av journalførVedtaksbrevSteg`() {
        every {
            taskRepository.save(any())
        } returns Task("", "", Properties())
        every { vedtaksbrevService.journalførVedtaksbrev(any()) } returns journalpostId
        every { behandlingService.leggTilBehandlingsjournalpost(any(), Journalposttype.U, behandling.id) } just Runs

        journalførVedtaksbrev.utførSteg(behandling, null)

        verify { vedtaksbrevService.journalførVedtaksbrev(behandling.id) }
    }

    @Test
    internal fun `journalpost skal knyttes til behandling ved utførelse av journalførVedtaksbrevSteg`() {
        val journalpostIdSlot = slot<String>()
        every {
            taskRepository.save(any())
        } returns Task("", "", Properties())

        every { vedtaksbrevService.journalførVedtaksbrev(any()) } returns journalpostId

        every {
            behandlingService.leggTilBehandlingsjournalpost(capture(journalpostIdSlot),
                                                            Journalposttype.U,
                                                            behandling.id)
        } just Runs

        journalførVedtaksbrev.utførSteg(behandling, null)

        assertThat(journalpostIdSlot.captured).isEqualTo(journalpostId)
    }
}