package no.nav.familie.ef.sak.service.steg

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.ef.sak.repository.domain.*
import no.nav.familie.ef.sak.service.TilkjentYtelseService
import no.nav.familie.ef.sak.task.VentePåStatusFraØkonomiTask
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.util.*

internal class IverksettMotOppdragStegTest {

    private val taskRepository = mockk<TaskRepository>()
    private val tilkjentYtelseService = mockk<TilkjentYtelseService>()
    val iverksettMotOppdragSteg = IverksettMotOppdragSteg(tilkjentYtelseService, taskRepository)

    @Test
    internal fun `skal opprette ventePåStatusFraØkonomiTask etter å ha iverksatt mot oppdrag`() {
        val fnr = "12345678901"
        val fagsak = Fagsak(stønadstype = Stønadstype.OVERGANGSSTØNAD,
                            søkerIdenter = setOf(FagsakPerson(ident = fnr)))
        val behandling = Behandling(fagsakId = fagsak.id,
                                    type = BehandlingType.FØRSTEGANGSBEHANDLING,
                                    status = BehandlingStatus.IVERKSETTER_VEDTAK,
                                    steg = iverksettMotOppdragSteg.stegType(),
                                    resultat = BehandlingResultat.IKKE_SATT)

        val taskSlot = slot<Task>()
        every {
            taskRepository.save(capture(taskSlot))
        } returns Task("", "", Properties())

        every {
            tilkjentYtelseService.oppdaterTilkjentYtelseMedUtbetalingsoppdragOgIverksett(any())
        } returns TilkjentYtelse(behandlingId = behandling.id,
                                 personident = fagsak.hentAktivIdent(),
                                 type = TilkjentYtelseType.FØRSTEGANGSBEHANDLING,
                                 andelerTilkjentYtelse = emptyList())

        iverksettMotOppdragSteg.utførSteg(behandling, null)

        Assertions.assertThat(taskSlot.captured.type).isEqualTo(VentePåStatusFraØkonomiTask.TYPE)
    }

}