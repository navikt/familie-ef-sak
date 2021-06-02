package no.nav.familie.ef.sak.økonomi

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.BehandlingRepository
import no.nav.familie.ef.sak.repository.FagsakRepository
import no.nav.familie.ef.sak.repository.TilkjentYtelseRepository
import no.nav.familie.ef.sak.repository.domain.Behandling
import no.nav.familie.ef.sak.repository.domain.BehandlingStatus
import no.nav.familie.ef.sak.repository.domain.BehandlingType
import no.nav.familie.ef.sak.repository.domain.Sporbar
import no.nav.familie.ef.sak.repository.domain.Stønadstype
import no.nav.familie.ef.sak.repository.domain.TilkjentYtelseMedMetaData
import no.nav.familie.ef.sak.økonomi.UtbetalingsoppdragGenerator.lagTilkjentYtelseMedUtbetalingsoppdrag
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import java.time.LocalDate


internal class TilkjentYtelseRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var repository: TilkjentYtelseRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Test
    fun `Opprett og hent tilkjent ytelse`() {
        val tilkjentYtelse = DataGenerator.tilfeldigTilkjentYtelse(opprettBehandling())
        val tilkjentYtelseId = repository.insert(tilkjentYtelse).id

        val hentetTilkjentYtelse = repository.findByIdOrNull(tilkjentYtelseId)!!

        assertThat(hentetTilkjentYtelse.behandlingId).isEqualTo(tilkjentYtelse.behandlingId)
        assertThat(hentetTilkjentYtelse.andelerTilkjentYtelse).isNotEmpty
    }

    @Test
    fun `Opprett og hent andeler tilkjent ytelse`() {
        val fagsak = fagsakRepository.insert(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak = fagsak))
        val tilkjentYtelse = DataGenerator.tilfeldigTilkjentYtelse(opprettBehandling(), 2)

        val tilkjentYtelseId = repository.insert(tilkjentYtelse).id

        val hentetTilkjentYtelse = repository.findByIdOrNull(tilkjentYtelseId)!!
        assertThat(hentetTilkjentYtelse.andelerTilkjentYtelse.size).isEqualTo(2)
    }

    @Test
    fun `Lagre utbetalingsoppdrag`() {
        val fagsak = fagsakRepository.insert(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak = fagsak))
        val lagretTilkjentYtelse =
                repository.insert(DataGenerator.tilfeldigTilkjentYtelse(opprettBehandling(), 2))
        val utbetalingsoppdrag =
                lagTilkjentYtelseMedUtbetalingsoppdrag(TilkjentYtelseMedMetaData(lagretTilkjentYtelse,
                                                                                 behandling.eksternId.id,
                                                                                 fagsak.stønadstype,
                                                                                 fagsak.eksternId.id)).utbetalingsoppdrag!!

        repository.update(lagretTilkjentYtelse.copy(utbetalingsoppdrag = utbetalingsoppdrag))

        val oppdatertTilkjentYtelse = repository.findByIdOrNull(lagretTilkjentYtelse.id)!!
        assertThat(oppdatertTilkjentYtelse.utbetalingsoppdrag).isEqualTo(utbetalingsoppdrag)
    }

    @Test
    fun `Finn tilkjent ytelse på personident`() {
        val tilkjentYtelse = DataGenerator.tilfeldigTilkjentYtelse(opprettBehandling())
        val lagretTilkjentYtelse = repository.insert(tilkjentYtelse)

        val hentetTilkjentYtelse =
                repository.findByPersonident(tilkjentYtelse.personident)

        assertThat(hentetTilkjentYtelse).isEqualTo(lagretTilkjentYtelse)
    }

    @Test
    fun `Finn tilkjent ytelse på behandlingId`() {
        val behandling = opprettBehandling()
        val tilkjentYtelse = DataGenerator.tilfeldigTilkjentYtelse(behandling)
        val lagretTilkjentYtelse = repository.insert(tilkjentYtelse)

        val hentetTilkjentYtelse = repository.findByBehandlingId(behandling.id)

        assertThat(hentetTilkjentYtelse).isEqualTo(lagretTilkjentYtelse)
    }

    @Test
    internal fun `finnTilkjentYtelserTilKonsistensAvstemming`() {
        val fagsak = fagsakRepository.insert(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak, status = BehandlingStatus.FERDIGSTILT))

        val tilkjentYtelse = DataGenerator.tilfeldigTilkjentYtelse(behandling)
        val stønadFom = tilkjentYtelse.andelerTilkjentYtelse.minOf { it.stønadFom }

        repository.insert(tilkjentYtelse)

        assertThat(repository.finnTilkjentYtelserTilKonsistensavstemming(Stønadstype.OVERGANGSSTØNAD, stønadFom.minusDays(1)))
                .hasSize(1)
        assertThat(repository.finnTilkjentYtelserTilKonsistensavstemming(Stønadstype.OVERGANGSSTØNAD, stønadFom))
                .hasSize(1)

        assertThat(repository.finnTilkjentYtelserTilKonsistensavstemming(Stønadstype.OVERGANGSSTØNAD, stønadFom.plusDays(1)))
                .isEmpty()

        assertThat(repository.finnTilkjentYtelserTilKonsistensavstemming(Stønadstype.BARNETILSYN, stønadFom))
                .withFailMessage("Må matche stønadstype")
                .isEmpty()
    }

    @Test
    internal fun `skal kun finne siste behandlingen sin tilkjenteytelse`() {
        val fagsak = fagsakRepository.insert(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak, status = BehandlingStatus.FERDIGSTILT)
                                                             .copy(sporbar = Sporbar(opprettetTid = LocalDate.of(2021, 1, 1)
                                                                     .atStartOfDay())))
        val behandling2 = behandlingRepository.insert(behandling(fagsak, status = BehandlingStatus.FERDIGSTILT))
        repository.insert(DataGenerator.tilfeldigTilkjentYtelse(behandling))
        repository.insert(DataGenerator.tilfeldigTilkjentYtelse(behandling2))

        assertThat(repository.findAll().map { it.behandlingId }).containsExactlyInAnyOrder(behandling.id, behandling2.id)

        val result = repository.finnTilkjentYtelserTilKonsistensavstemming(Stønadstype.OVERGANGSSTØNAD, LocalDate.now())
        assertThat(result.map { it.behandlingId }).containsExactly(behandling2.id)
    }

    @Test
    internal fun `finnTilkjentYtelserTilKonsistensAvstemming - Skal ikke finne når status ikke er ferdigstillt`() {
        val fagsak = fagsakRepository.insert(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak, status = BehandlingStatus.UTREDES))

        val tilkjentYtelse = DataGenerator.tilfeldigTilkjentYtelse(behandling)
        val stønadFom = tilkjentYtelse.andelerTilkjentYtelse.minOf { it.stønadFom }

        repository.insert(tilkjentYtelse)

        assertThat(repository.finnTilkjentYtelserTilKonsistensavstemming(Stønadstype.OVERGANGSSTØNAD, stønadFom.minusDays(1)))
                .isEmpty()
    }

    @Test
    internal fun `finnTilkjentYtelserTilKonsistensAvstemming - Skal ikke finne når type er feil`() {
        val fagsak = fagsakRepository.insert(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak,
                                                                status = BehandlingStatus.FERDIGSTILT,
                                                                type = BehandlingType.BLANKETT))

        val tilkjentYtelse = DataGenerator.tilfeldigTilkjentYtelse(behandling)
        val stønadFom = tilkjentYtelse.andelerTilkjentYtelse.minOf { it.stønadFom }

        repository.insert(tilkjentYtelse)

        assertThat(repository.finnTilkjentYtelserTilKonsistensavstemming(Stønadstype.OVERGANGSSTØNAD, stønadFom.minusDays(1)))
                .isEmpty()
    }

    private fun opprettBehandling(): Behandling {
        val fagsak = fagsakRepository.insert(fagsak())

        return behandlingRepository.insert(behandling(fagsak))
    }
}