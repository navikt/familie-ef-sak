package no.nav.familie.ef.sak.no.nav.familie.ef.sak.service.steg

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.ef.sak.repository.domain.*
import no.nav.familie.ef.sak.service.steg.DistribuerVedtaksbrevSteg
import no.nav.familie.ef.sak.task.FerdigstillBehandlingTask
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.util.*

internal class DistribuerVedtaksbrevStegStegTest {


    private val taskRepository = mockk<TaskRepository>()
    val distribuerVedtaksbrev = DistribuerVedtaksbrevSteg(taskRepository)

    @Test
    internal fun `skal opprette ferdigstillVedtakTask etter distribuering av vedtaksbrev`() {
        val fnr = "12345678901"
        val fagsak = Fagsak(stønadstype = Stønadstype.OVERGANGSSTØNAD,
                            søkerIdenter = setOf(FagsakPerson(ident = fnr)))

        val taskSlot = slot<Task>()
        every {
            taskRepository.save(capture(taskSlot))
        } returns Task("", "", Properties())

        distribuerVedtaksbrev.utførSteg(Behandling(fagsakId = fagsak.id,
                                                type = BehandlingType.FØRSTEGANGSBEHANDLING,
                                                status = BehandlingStatus.IVERKSETTER_VEDTAK,
                                                steg = distribuerVedtaksbrev.stegType(),
                                                resultat = BehandlingResultat.IKKE_SATT),
                                     null)

        Assertions.assertThat(taskSlot.captured.type).isEqualTo(FerdigstillBehandlingTask.TYPE)
    }
}