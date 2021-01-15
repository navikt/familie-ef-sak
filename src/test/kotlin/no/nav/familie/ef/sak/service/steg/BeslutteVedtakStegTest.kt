package no.nav.familie.ef.sak.service.steg

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.ef.sak.repository.domain.*
import no.nav.familie.ef.sak.service.FagsakService
import no.nav.familie.ef.sak.task.IverksettMotOppdragTask
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.util.*

internal class BeslutteVedtakStegTest {


    private val taskRepository = mockk<TaskRepository>()
    private val fagsakService = mockk<FagsakService>()
    val beslutteVedtakSteg = BeslutteVedtakSteg(taskRepository, fagsakService)

    @Test
    internal fun `skal opprette iverksettMotOppdragTask etter beslutte vedtak`() {
        val fnr = "12345678901"
        val fagsak = Fagsak(stønadstype = Stønadstype.OVERGANGSSTØNAD,
                            søkerIdenter = setOf(FagsakPerson(ident = fnr)))

        val taskSlot = slot<Task>()
        every {
            fagsakService.hentFagsak(any())
        } returns fagsak

        every {
            taskRepository.save(capture(taskSlot))
        } returns Task("", "", Properties())

        beslutteVedtakSteg.utførSteg(Behandling(fagsakId = fagsak.id,
                                                type = BehandlingType.FØRSTEGANGSBEHANDLING,
                                                status = BehandlingStatus.FATTER_VEDTAK,
                                                steg = beslutteVedtakSteg.stegType()),
                                     null)

        Assertions.assertThat(taskSlot.captured.type).isEqualTo(IverksettMotOppdragTask.TYPE)
    }
}