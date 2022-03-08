package no.nav.familie.ef.sak.økonomi

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.fagsak.FagsakRepository
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.innvilgetOgFerdigstilt
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseRepository
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
        val tilkjentYtelse = DataGenerator.tilfeldigTilkjentYtelse(opprettBehandling(), 2)

        val tilkjentYtelseId = repository.insert(tilkjentYtelse).id

        val hentetTilkjentYtelse = repository.findByIdOrNull(tilkjentYtelseId)!!
        assertThat(hentetTilkjentYtelse.andelerTilkjentYtelse.size).isEqualTo(2)
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
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak).innvilgetOgFerdigstilt())

        val tilkjentYtelse = DataGenerator.tilfeldigTilkjentYtelse(behandling)
        val stønadFom = tilkjentYtelse.andelerTilkjentYtelse.minOf { it.stønadFom }

        repository.insert(tilkjentYtelse)

        assertThat(repository.finnTilkjentYtelserTilKonsistensavstemming(fagsak.stønadstype, stønadFom.minusDays(1)))
                .withFailMessage("Skal finne alle fremtidlige tilkjente ytelser")
                .hasSize(1)
        assertThat(repository.finnTilkjentYtelserTilKonsistensavstemming(fagsak.stønadstype, stønadFom))
                .hasSize(1)

        assertThat(repository.finnTilkjentYtelserTilKonsistensavstemming(fagsak.stønadstype, stønadFom.plusDays(1)))
                .isEmpty()
    }


    @Test
    internal fun `skal kun finne siste behandlingen sin tilkjenteytelse`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val opprettetTid = LocalDate.of(2021, 1, 1).atStartOfDay()
        val behandling = behandlingRepository.insert(behandling(fagsak, opprettetTid = opprettetTid).innvilgetOgFerdigstilt())
        val behandling2 = behandlingRepository.insert(behandling(fagsak).innvilgetOgFerdigstilt())
        repository.insert(DataGenerator.tilfeldigTilkjentYtelse(behandling))
        repository.insert(DataGenerator.tilfeldigTilkjentYtelse(behandling2))

        assertThat(repository.findAll().map { it.behandlingId }).containsExactlyInAnyOrder(behandling.id, behandling2.id)

        val result = repository.finnTilkjentYtelserTilKonsistensavstemming(fagsak.stønadstype, LocalDate.now())
        assertThat(result.map { it.behandlingId }).containsExactly(behandling2.id)
    }

    @Test
    internal fun `finnTilkjentYtelserTilKonsistensavstemming skal ikke få med tilkjent ytelser som kun har 0-beløp`() {
        val beløp = 0
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak, opprettetTid = LocalDate.of(2021, 1, 1).atStartOfDay())
                                                             .innvilgetOgFerdigstilt())
        val andelerTilkjentYtelse = listOf(lagAndelTilkjentYtelse(beløp = beløp,
                                                                  fraOgMed = LocalDate.now(),
                                                                  tilOgMed = LocalDate.now().plusDays(1),
                                                                  kildeBehandlingId = behandling.id))
        repository.insert(DataGenerator.tilfeldigTilkjentYtelse(behandling).copy(andelerTilkjentYtelse = andelerTilkjentYtelse))

        assertThat(repository.finnTilkjentYtelserTilKonsistensavstemming(fagsak.stønadstype, LocalDate.now()))
                .isEmpty()
    }

    private fun opprettBehandling(): Behandling {
        val fagsak = testoppsettService.lagreFagsak(fagsak())

        return behandlingRepository.insert(behandling(fagsak))
    }
}