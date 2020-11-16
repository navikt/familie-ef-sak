package no.nav.familie.ef.sak.økonomi

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.BehandlingRepository
import no.nav.familie.ef.sak.repository.FagsakRepository
import no.nav.familie.ef.sak.repository.TilkjentYtelseRepository
import no.nav.familie.ef.sak.repository.domain.TilkjentYtelseMedMetaData
import no.nav.familie.ef.sak.økonomi.UtbetalingsoppdragGenerator.lagTilkjentYtelseMedUtbetalingsoppdrag
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull


internal class TilkjentYtelseRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Test
    fun `Opprett og hent tilkjent ytelse`() {
        val fagsak = fagsakRepository.insert(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak = fagsak))
        val tilkjentYtelse = DataGenerator.tilfeldigTilkjentYtelse(behandlingId = behandling.id)
        val tilkjentYtelseId = tilkjentYtelseRepository.insert(tilkjentYtelse).id

        val hentetTilkjentYtelse = tilkjentYtelseRepository.findByIdOrNull(tilkjentYtelseId)!!

        assertThat(hentetTilkjentYtelse.behandlingId).isEqualTo(tilkjentYtelse.behandlingId)
        assertThat(hentetTilkjentYtelse.andelerTilkjentYtelse).isNotEmpty
    }

    @Test
    fun `Opprett og hent andeler tilkjent ytelse`() {
        val fagsak = fagsakRepository.insert(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak = fagsak))
        val tilkjentYtelse = DataGenerator.tilfeldigTilkjentYtelse(2, behandlingId = behandling.id)

        val tilkjentYtelseId = tilkjentYtelseRepository.insert(tilkjentYtelse).id

        val hentetTilkjentYtelse = tilkjentYtelseRepository.findByIdOrNull(tilkjentYtelseId)!!
        assertThat(hentetTilkjentYtelse.andelerTilkjentYtelse.size).isEqualTo(2)
    }

    @Test
    fun `Lagre utbetalingsoppdrag`() {
        val fagsak = fagsakRepository.insert(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak = fagsak))
        val lagretTilkjentYtelse =
                tilkjentYtelseRepository.insert(DataGenerator.tilfeldigTilkjentYtelse(2, behandlingId = behandling.id))
        val utbetalingsoppdrag =
                lagTilkjentYtelseMedUtbetalingsoppdrag(TilkjentYtelseMedMetaData(lagretTilkjentYtelse,
                                                                                 behandling.eksternId.id,
                                                                                 fagsak.eksternId.id)).utbetalingsoppdrag!!

        tilkjentYtelseRepository.update(lagretTilkjentYtelse.copy(utbetalingsoppdrag = utbetalingsoppdrag))

        val oppdatertTilkjentYtelse = tilkjentYtelseRepository.findByIdOrNull(lagretTilkjentYtelse.id)!!
        assertThat(oppdatertTilkjentYtelse.utbetalingsoppdrag).isEqualTo(utbetalingsoppdrag)
    }

    @Test
    fun `Finn tilkjent ytelse på personident`() {
        val fagsak = fagsakRepository.insert(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak = fagsak))
        val tilkjentYtelse = DataGenerator.tilfeldigTilkjentYtelse(behandlingId = behandling.id)
        val lagretTilkjentYtelse = tilkjentYtelseRepository.insert(tilkjentYtelse)

        val hentetTilkjentYtelse =
                tilkjentYtelseRepository.findByPersonident(tilkjentYtelse.personident)

        assertThat(hentetTilkjentYtelse).isEqualTo(lagretTilkjentYtelse)
    }
}