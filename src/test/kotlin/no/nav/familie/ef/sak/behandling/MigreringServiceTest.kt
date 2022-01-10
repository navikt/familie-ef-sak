package no.nav.familie.ef.sak.behandling

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.dto.RevurderingDto
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType
import no.nav.familie.ef.sak.behandlingsflyt.task.FerdigstillBehandlingTask
import no.nav.familie.ef.sak.behandlingsflyt.task.PollStatusFraIverksettTask
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Stønadstype
import no.nav.familie.ef.sak.felles.util.BrukerContextUtil.testWithBrukerContext
import no.nav.familie.ef.sak.simulering.SimuleringsresultatRepository
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.prosessering.domene.TaskRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

internal class MigreringServiceTest : OppslagSpringRunnerTest() {

    @Autowired private lateinit var fagsakService: FagsakService
    @Autowired private lateinit var behandlingService: BehandlingService
    @Autowired private lateinit var revurderingService: RevurderingService
    @Autowired private lateinit var migreringService: MigreringService
    @Autowired private lateinit var tilkjentYtelseService: TilkjentYtelseService
    @Autowired private lateinit var taskRepository: TaskRepository
    @Autowired private lateinit var pollStatusFraIverksettTask: PollStatusFraIverksettTask
    @Autowired private lateinit var ferdigstillBehandlingTask: FerdigstillBehandlingTask
    @Autowired private lateinit var simuleringsresultatRepository: SimuleringsresultatRepository

    private val fra = YearMonth.now() // TODO
    private val til = YearMonth.now().plusMonths(1) // TODO
    private val forventetInntekt = BigDecimal.ZERO // TODO
    private val samordningsfradrag = BigDecimal.ZERO // TODO

    @Test
    internal fun `skal opprette migrering og sende til iverksett`() {
        val migrering = opprettOgIverksettMigrering()

        with(tilkjentYtelseService.hentForBehandling(migrering.id).andelerTilkjentYtelse) {
            assertThat(this).hasSize(1)
            assertThat(this[0].stønadFom).isEqualTo(fra.atDay(1))
            assertThat(this[0].stønadTom).isEqualTo(til.atEndOfMonth())
        }
        with(behandlingService.hentBehandling(migrering.id)) {
            assertThat(this.status).isEqualTo(BehandlingStatus.FERDIGSTILT)
            assertThat(this.resultat).isEqualTo(BehandlingResultat.INNVILGET)
            assertThat(this.steg).isEqualTo(StegType.PUBLISER_VEDTAKSHENDELSE)
        }
        assertThat(simuleringsresultatRepository.findByIdOrNull(migrering.id)).isNotNull
    }

    @Test
    internal fun `skal opprette revurering på migrering`() {
        val migrering = opprettOgIverksettMigrering()
        val revurderingDto = RevurderingDto(fagsakId = migrering.fagsakId,
                                            behandlingsårsak = BehandlingÅrsak.NYE_OPPLYSNINGER,
                                            kravMottatt = LocalDate.now())
        testWithBrukerContext { revurderingService.opprettRevurderingManuelt(revurderingDto) }
    }

    private fun opprettOgIverksettMigrering(): Behandling {
        val fagsak = fagsakService.hentEllerOpprettFagsak("1", Stønadstype.OVERGANGSSTØNAD)
        val behandling = migreringService.opprettMigrering(fagsak, fra, til, forventetInntekt, samordningsfradrag)

        pollStatusFraIverksettTask.doTask(taskRepository.findAll().single { it.type == PollStatusFraIverksettTask.TYPE })
        ferdigstillBehandlingTask.doTask(taskRepository.findAll().single { it.type == FerdigstillBehandlingTask.TYPE })
        return behandling
    }
}