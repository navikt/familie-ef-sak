package no.nav.familie.ef.sak.no.nav.familie.ef.sak.service.steg

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.ef.sak.repository.domain.*
import no.nav.familie.ef.sak.service.steg.JournalførVedtaksbrevSteg
import no.nav.familie.ef.sak.task.DistribuerVedtaksbrevTask
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.util.*

internal class JournalførVedtaksbrevStegTest {


    private val taskRepository = mockk<TaskRepository>()
    val journalførVedtaksbrev = JournalførVedtaksbrevSteg(taskRepository)

    @Test
    internal fun `skal opprette distribuerVedtaksbrevTask etter journalføring av vedtaksbrev`() {
        val fnr = "12345678901"
        val fagsak = Fagsak(stønadstype = Stønadstype.OVERGANGSSTØNAD,
                            søkerIdenter = setOf(FagsakPerson(ident = fnr)))

        val taskSlot = slot<Task>()
        every {
            taskRepository.save(capture(taskSlot))
        } returns Task("", "", Properties())

        journalførVedtaksbrev.utførSteg(Behandling(fagsakId = fagsak.id,
                                                   type = BehandlingType.FØRSTEGANGSBEHANDLING,
                                                   status = BehandlingStatus.IVERKSETTER_VEDTAK,
                                                   steg = journalførVedtaksbrev.stegType(),
                                                   resultat = BehandlingResultat.IKKE_SATT),
                                        null)

        Assertions.assertThat(taskSlot.captured.type).isEqualTo(DistribuerVedtaksbrevTask.TYPE)
    }
}