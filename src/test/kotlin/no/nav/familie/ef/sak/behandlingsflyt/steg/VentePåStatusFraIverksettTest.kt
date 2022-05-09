package no.nav.familie.ef.sak.behandlingsflyt.steg

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.ef.sak.repository.saksbehandling
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.ef.sak.økonomi.lagAndelTilkjentYtelse
import no.nav.familie.ef.sak.økonomi.lagTilkjentYtelse
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.ef.iverksett.IverksettStatus
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.familie.prosessering.error.TaskExceptionUtenStackTrace
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class VentePåStatusFraIverksettTest {

    private val iverksettClient = mockk<IverksettClient>()
    private val tilkjentYtelseService = mockk<TilkjentYtelseService>()
    private val taskRepository = mockk<TaskRepository>()

    private val steg = VentePåStatusFraIverksett(iverksettClient, tilkjentYtelseService, taskRepository)

    @BeforeEach
    internal fun setUp() {
        every { taskRepository.save(any()) } answers { firstArg() }
    }

    @Test
    internal fun `KORRIGERING_UTEN_BREV - ok når status er OK_MOT_OPPDRAG`() {
        mockIverksettStatus(IverksettStatus.OK_MOT_OPPDRAG)
        mockTilkjentYtelse()

        steg.utførSteg(saksbehandling(årsak = BehandlingÅrsak.KORRIGERING_UTEN_BREV), null)

        verify(exactly = 0) { tilkjentYtelseService.hentForBehandling(any()) }
        verify(exactly = 1) { taskRepository.save(any()) }
    }

    @Test
    internal fun `migrering - ok når status er OK_MOT_OPPDRAG`() {
        mockIverksettStatus(IverksettStatus.OK_MOT_OPPDRAG)
        mockTilkjentYtelse()

        steg.utførSteg(saksbehandling(årsak = BehandlingÅrsak.MIGRERING), null)

        verify(exactly = 0) { tilkjentYtelseService.hentForBehandling(any()) }
        verify(exactly = 1) { taskRepository.save(any()) }
    }

    @Test
    internal fun `migrering - SENDT_TIL_OPPDRAG ok hvis 0-beløp då den ikke iverksetter`() {
        mockIverksettStatus(IverksettStatus.SENDT_TIL_OPPDRAG)
        mockTilkjentYtelse(med0beløp = true)

        steg.utførSteg(saksbehandling(årsak = BehandlingÅrsak.MIGRERING), null)

        verify(exactly = 1) { tilkjentYtelseService.hentForBehandling(any()) }
        verify(exactly = 1) { taskRepository.save(any()) }
    }

    @Test
    internal fun `migrering - SENDT_TIL_OPPDRAG feil hvis ikke 0-beløp, den forventes å motta kvittering fra oppdrag`() {
        mockIverksettStatus(IverksettStatus.SENDT_TIL_OPPDRAG)
        mockTilkjentYtelse()

        assertThatThrownBy { steg.utførSteg(saksbehandling(årsak = BehandlingÅrsak.MIGRERING), null) }
                .isInstanceOf(TaskExceptionUtenStackTrace::class.java)

        verify(exactly = 1) { tilkjentYtelseService.hentForBehandling(any()) }
        verify(exactly = 0) { taskRepository.save(any()) }
    }

    @Test
    internal fun `migrering - IKKE_PÅBEGYNT kaster feil`() {
        mockIverksettStatus(IverksettStatus.IKKE_PÅBEGYNT)
        mockTilkjentYtelse()

        assertThatThrownBy { steg.utførSteg(saksbehandling(årsak = BehandlingÅrsak.MIGRERING), null) }
                .isInstanceOf(TaskExceptionUtenStackTrace::class.java)

        verify(exactly = 0) { tilkjentYtelseService.hentForBehandling(any()) }
        verify(exactly = 0) { taskRepository.save(any()) }
    }

    private fun mockTilkjentYtelse(med0beløp: Boolean = false) {
        val beløp = if (med0beløp) 0 else 1
        every { tilkjentYtelseService.hentForBehandling(any()) } returns
                lagTilkjentYtelse(listOf(lagAndelTilkjentYtelse(beløp = beløp, LocalDate.now(), LocalDate.now())))
    }

    private fun mockIverksettStatus(status: IverksettStatus) {
        every { iverksettClient.hentStatus(any()) } returns status
    }
}