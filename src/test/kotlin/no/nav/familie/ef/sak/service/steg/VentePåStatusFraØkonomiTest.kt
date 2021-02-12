package no.nav.familie.ef.sak.service.steg

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.ef.sak.repository.domain.*
import no.nav.familie.ef.sak.service.TilkjentYtelseService
import no.nav.familie.ef.sak.task.JournalførVedtaksbrevTask
import no.nav.familie.kontrakter.felles.oppdrag.OppdragStatus
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.util.*

internal class VentePåStatusFraØkonomiTest {

    private val taskRepository = mockk<TaskRepository>()
    private val tilkjentYtelseService = mockk<TilkjentYtelseService>()
    val ventPåStatusFraØkonomi = VentePåStatusFraØkonomi(tilkjentYtelseService, taskRepository)

    @Test
    internal fun `skal opprette journalførVedtaksBrevTask etter ok-status fra økonomi`() {
        val fnr = "12345678901"
        val fagsak = Fagsak(stønadstype = Stønadstype.OVERGANGSSTØNAD,
                            søkerIdenter = setOf(FagsakPerson(ident = fnr)))

        val taskSlot = slot<Task>()
        every {
            taskRepository.save(capture(taskSlot))
        } returns Task("", "", Properties())

        every {
            tilkjentYtelseService.hentStatus(any())
        } returns OppdragStatus.KVITTERT_OK

        ventPåStatusFraØkonomi.utførSteg(Behandling(fagsakId = fagsak.id,
                                                    type = BehandlingType.FØRSTEGANGSBEHANDLING,
                                                    status = BehandlingStatus.IVERKSETTER_VEDTAK,
                                                    steg = ventPåStatusFraØkonomi.stegType(),
                                                    resultat = BehandlingResultat.INNVILGET),
                                         null)

        Assertions.assertThat(taskSlot.captured.type).isEqualTo(JournalførVedtaksbrevTask.TYPE)
    }
}