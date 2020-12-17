package no.nav.familie.ef.sak.økonomi

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.BehandlingRepository
import no.nav.familie.ef.sak.repository.FagsakRepository
import no.nav.familie.ef.sak.repository.TilkjentYtelseRepository
import no.nav.familie.ef.sak.repository.domain.TilkjentYtelseMedMetaData
import no.nav.familie.ef.sak.repository.domain.Behandling
import no.nav.familie.ef.sak.økonomi.UtbetalingsoppdragGenerator.lagTilkjentYtelseMedUtbetalingsoppdrag
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import java.time.LocalDate


internal class TilkjentYtelseRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Test
    fun `Opprett og hent tilkjent ytelse`() {
        val tilkjentYtelse = DataGenerator.tilfeldigTilkjentYtelse(opprettBehandling())
        val tilkjentYtelseId = tilkjentYtelseRepository.insert(tilkjentYtelse).id

        val hentetTilkjentYtelse = tilkjentYtelseRepository.findByIdOrNull(tilkjentYtelseId)!!

        assertThat(hentetTilkjentYtelse.behandlingId).isEqualTo(tilkjentYtelse.behandlingId)
        assertThat(hentetTilkjentYtelse.andelerTilkjentYtelse).isNotEmpty
    }

    @Test
    fun `Opprett og hent andeler tilkjent ytelse`() {
        val fagsak = fagsakRepository.insert(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak = fagsak))
        val tilkjentYtelse = DataGenerator.tilfeldigTilkjentYtelse(opprettBehandling(), 2)

        val tilkjentYtelseId = tilkjentYtelseRepository.insert(tilkjentYtelse).id

        val hentetTilkjentYtelse = tilkjentYtelseRepository.findByIdOrNull(tilkjentYtelseId)!!
        assertThat(hentetTilkjentYtelse.andelerTilkjentYtelse.size).isEqualTo(2)
    }

    @Test
    fun `Lagre utbetalingsoppdrag`() {
        val fagsak = fagsakRepository.insert(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak = fagsak))
        val lagretTilkjentYtelse =
                tilkjentYtelseRepository.insert(DataGenerator.tilfeldigTilkjentYtelse(opprettBehandling(), 2))
        val utbetalingsoppdrag =
                lagTilkjentYtelseMedUtbetalingsoppdrag(TilkjentYtelseMedMetaData(lagretTilkjentYtelse,
                                                                                 behandling.eksternId.id,
                                                                                 fagsak.stønadstype,
                                                                                 fagsak.eksternId.id)).utbetalingsoppdrag!!

        tilkjentYtelseRepository.update(lagretTilkjentYtelse.copy(utbetalingsoppdrag = utbetalingsoppdrag))

        val oppdatertTilkjentYtelse = tilkjentYtelseRepository.findByIdOrNull(lagretTilkjentYtelse.id)!!
        assertThat(oppdatertTilkjentYtelse.utbetalingsoppdrag).isEqualTo(utbetalingsoppdrag)
    }

    @Test
    fun `Finn tilkjent ytelse på personident`() {
        val tilkjentYtelse = DataGenerator.tilfeldigTilkjentYtelse(opprettBehandling())
        val lagretTilkjentYtelse = tilkjentYtelseRepository.insert(tilkjentYtelse)

        val hentetTilkjentYtelse =
                tilkjentYtelseRepository.findByPersonident(tilkjentYtelse.personident)

        assertThat(hentetTilkjentYtelse).isEqualTo(lagretTilkjentYtelse)
    }

    @Test
    internal fun `finn periodeIdn for behandlinger`() {
        val fagsak = fagsakRepository.insert(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak))
        var tilkjentYtelse = DataGenerator.tilfeldigTilkjentYtelse(behandling)
        val nyeAndeler =
                tilkjentYtelse.andelerTilkjentYtelse.map { it.copy(stønadTom = LocalDate.now().plusYears(1),
                                                                   periodeId = 1) }
        tilkjentYtelse = tilkjentYtelse.copy(andelerTilkjentYtelse = nyeAndeler)
        tilkjentYtelseRepository.insert(tilkjentYtelse)
        val finnKildeBehandlingIdFraAndelTilkjentYtelse2 =
                tilkjentYtelseRepository.finnKildeBehandlingIdFraAndelTilkjentYtelse2(LocalDate.now(), listOf(behandling.id))
        assertThat(finnKildeBehandlingIdFraAndelTilkjentYtelse2).hasSize(1)
        assertThat(finnKildeBehandlingIdFraAndelTilkjentYtelse2[0].first).isEqualTo(fagsak.eksternId.id)
        assertThat(finnKildeBehandlingIdFraAndelTilkjentYtelse2[0].second)
                .isEqualTo(tilkjentYtelse.andelerTilkjentYtelse[0].periodeId)

    }

    private fun opprettBehandling() : Behandling {
        val fagsak = fagsakRepository.insert(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak))

        return behandling;
    }
}