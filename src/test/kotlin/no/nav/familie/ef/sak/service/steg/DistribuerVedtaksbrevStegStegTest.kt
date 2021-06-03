package no.nav.familie.ef.sak.no.nav.familie.ef.sak.service.steg

import io.mockk.every
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
import no.nav.familie.ef.sak.service.VedtaksbrevIverksettingService
import no.nav.familie.ef.sak.service.steg.DistribuerVedtaksbrevSteg
import no.nav.familie.ef.sak.task.FerdigstillBehandlingTask
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.util.Properties
import java.util.UUID

internal class DistribuerVedtaksbrevStegStegTest {

    private val taskRepository = mockk<TaskRepository>()
    private val vedtaksbrevService = mockk<VedtaksbrevIverksettingService>()
    val distribuerVedtaksbrev = DistribuerVedtaksbrevSteg(taskRepository, vedtaksbrevService)
    val fnr = "12345678901"
    val fagsak = Fagsak(stønadstype = Stønadstype.OVERGANGSSTØNAD,
                        søkerIdenter = setOf(FagsakPerson(ident = fnr)))
    val behandlingId = UUID.randomUUID()


    @Test
    internal fun `skal opprette ferdigstillVedtakTask etter distribuering av vedtaksbrev`() {
        val taskSlot = slot<Task>()

        every {
            taskRepository.save(capture(taskSlot))
        } returns Task("", "", Properties())

        every { vedtaksbrevService.distribuerVedtaksbrev(any(), any()) } returns "99999"

        distribuerVedtaksbrev.utførSteg(Behandling(id = behandlingId,
                                                   fagsakId = fagsak.id,
                                                   type = BehandlingType.FØRSTEGANGSBEHANDLING,
                                                   status = BehandlingStatus.IVERKSETTER_VEDTAK,
                                                   steg = distribuerVedtaksbrev.stegType(),
                                                   resultat = BehandlingResultat.IKKE_SATT),
                                        "1234")

        Assertions.assertThat(taskSlot.captured.type).isEqualTo(FerdigstillBehandlingTask.TYPE)
    }

    @Test
    internal fun `skal distribuere brev ved utførelse av steg`() {
        val journalpostId = "12345"
        every {
            taskRepository.save(any())
        } returns Task("", "", Properties())

        every { vedtaksbrevService.distribuerVedtaksbrev(any(), any()) } returns "99999"

        distribuerVedtaksbrev.utførSteg(Behandling(id = behandlingId,
                                                   fagsakId = fagsak.id,
                                                   type = BehandlingType.FØRSTEGANGSBEHANDLING,
                                                   status = BehandlingStatus.IVERKSETTER_VEDTAK,
                                                   steg = distribuerVedtaksbrev.stegType(),
                                                   resultat = BehandlingResultat.IKKE_SATT),
                                        journalpostId)

        verify { vedtaksbrevService.distribuerVedtaksbrev(behandlingId, journalpostId) }
    }
}