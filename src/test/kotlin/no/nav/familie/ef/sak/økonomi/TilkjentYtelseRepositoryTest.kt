package no.nav.familie.ef.sak.økonomi

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.repository.TilkjentYtelseRepository
import no.nav.familie.ef.sak.økonomi.UtbetalingsoppdragGenerator.lagTilkjentYtelseMedUtbetalingsoppdrag
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull


internal class TilkjentYtelseRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository

    @Test
    fun `Opprett og hent tilkjent ytelse`() {
        val tilkjentYtelse = DataGenerator.tilfeldigTilkjentYtelse()
        val tilkjentYtelseId = tilkjentYtelseRepository.insert(tilkjentYtelse).id

        val hentetTilkjentYtelse = tilkjentYtelseRepository.findByIdOrNull(tilkjentYtelseId)!!

        assertThat(hentetTilkjentYtelse.saksnummer).isEqualTo(tilkjentYtelse.saksnummer)
        assertThat(hentetTilkjentYtelse.andelerTilkjentYtelse).isNotEmpty
    }

    @Test
    fun `Opprett og hent andeler tilkjent ytelse`() {
        val tilkjentYtelse = DataGenerator.tilfeldigTilkjentYtelse(2)

        val tilkjentYtelseId = tilkjentYtelseRepository.insert(tilkjentYtelse).id

        val hentetTilkjentYtelse = tilkjentYtelseRepository.findByIdOrNull(tilkjentYtelseId)!!
        assertThat(hentetTilkjentYtelse.andelerTilkjentYtelse.size).isEqualTo(2)
    }

    @Test
    fun `Lagre utbetalingsoppdrag`() {
        val lagretTilkjentYtelse = tilkjentYtelseRepository.insert(DataGenerator.tilfeldigTilkjentYtelse(2))
        val utbetalingsoppdrag = lagTilkjentYtelseMedUtbetalingsoppdrag(lagretTilkjentYtelse).utbetalingsoppdrag!!

        tilkjentYtelseRepository.update(lagretTilkjentYtelse.copy(utbetalingsoppdrag = utbetalingsoppdrag))

        val oppdatertTilkjentYtelse = tilkjentYtelseRepository.findByIdOrNull(lagretTilkjentYtelse.id)!!
        assertThat(oppdatertTilkjentYtelse.utbetalingsoppdrag).isEqualTo(utbetalingsoppdrag)
    }

    @Test
    fun `Finn tilkjent ytelse på personident`() {
        val tilkjentYtelse = DataGenerator.tilfeldigTilkjentYtelse()
        val lagretTilkjentYtelse = tilkjentYtelseRepository.insert(tilkjentYtelse)

        val hentetTilkjentYtelse =
                tilkjentYtelseRepository.findByPersonident(tilkjentYtelse.personident)

        assertThat(hentetTilkjentYtelse).isEqualTo(lagretTilkjentYtelse)
    }
}