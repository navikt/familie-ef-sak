package no.nav.familie.ef.sak.økonomi

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.økonomi.DataGenerator
import no.nav.familie.ef.sak.repository.CustomRepository
import no.nav.familie.ef.sak.økonomi.Utbetalingsoppdrag.lagUtbetalingsoppdrag
import no.nav.familie.ef.sak.økonomi.domain.TilkjentYtelse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("local", "mock-auth", "mock-oauth")
@Tag("integration")
internal class TilkjentYtelseRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository

    @Autowired
    private lateinit var customRepository: CustomRepository<TilkjentYtelse>

    @Test
    fun `Opprett og hent tilkjent ytelse`() {
        val tilkjentYtelse = DataGenerator.tilfeldigTilkjentYtelse()
        val tilkjentYtelseId = customRepository.persist(tilkjentYtelse).id

        val hentetTilkjentYtelse = tilkjentYtelseRepository.findByIdOrNull(tilkjentYtelseId)!!

        assertThat(hentetTilkjentYtelse.saksnummer).isEqualTo(tilkjentYtelse.saksnummer)
        assertThat(hentetTilkjentYtelse.andelerTilkjentYtelse).isNotEmpty
    }

    @Test
    fun `Opprett og hent andeler tilkjent ytelse`() {
        val tilkjentYtelse = DataGenerator.tilfeldigTilkjentYtelse(2)

        val tilkjentYtelseId = customRepository.persist(tilkjentYtelse).id

        val hentetTilkjentYtelse = tilkjentYtelseRepository.findByIdOrNull(tilkjentYtelseId)!!
        assertThat(hentetTilkjentYtelse.andelerTilkjentYtelse.size).isEqualTo(2)
    }

    @Test
    fun `Lagre utbetalingsoppdrag`() {
        val lagretTilkjentYtelse = customRepository.persist(DataGenerator.tilfeldigTilkjentYtelse(2))
        val utbetalingsoppdrag = lagUtbetalingsoppdrag("saksbehandler", lagretTilkjentYtelse)

        tilkjentYtelseRepository.save(lagretTilkjentYtelse.copy(utbetalingsoppdrag = utbetalingsoppdrag))

        val oppdatertTilkjentYtelse = tilkjentYtelseRepository.findByIdOrNull(lagretTilkjentYtelse.id)!!
        assertThat(oppdatertTilkjentYtelse.utbetalingsoppdrag).isEqualTo(utbetalingsoppdrag)
    }

    @Test
    fun `Finn tilkjent ytelse på personident`() {
        val tilkjentYtelse = DataGenerator.tilfeldigTilkjentYtelse()
        val lagretTilkjentYtelse = customRepository.persist(tilkjentYtelse)

        val hentetTilkjentYtelse =
                tilkjentYtelseRepository.findByPersonident(tilkjentYtelse.personident)

        assertThat(hentetTilkjentYtelse).isEqualTo(lagretTilkjentYtelse)
    }
}