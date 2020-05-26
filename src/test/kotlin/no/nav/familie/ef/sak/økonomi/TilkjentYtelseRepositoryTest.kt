package no.nav.familie.ef.sak.økonomi

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.økonomi.DataGenerator
import no.nav.familie.ef.sak.økonomi.Utbetalingsoppdrag.lagUtbetalingsoppdrag
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.context.ActiveProfiles
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@ActiveProfiles("local", "mock-auth", "mock-oauth")
@Tag("integration")
internal class TilkjentYtelseRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository

    @Autowired
    private lateinit var andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository

    @Test
    fun `Opprett og hent tilkjent ytelse`() {

        val tilkjentYtelse = DataGenerator.tilfeldigTilkjentYtelse()

        val tilkjentYtelseId = tilkjentYtelseRepository.save(tilkjentYtelse).id

        assertNotNull(tilkjentYtelseId)

        val hentetTilkjentYtelse = tilkjentYtelseRepository.findByIdOrNull(tilkjentYtelseId)!!

        assertEquals(tilkjentYtelse.saksnummer, hentetTilkjentYtelse.saksnummer)
        assertEquals(tilkjentYtelse.eksternId, hentetTilkjentYtelse.eksternId)
    }

    @Test
    fun `Opprett og hent tilkjent ytelse med ekstern id`() {

        val tilkjentYtelse = DataGenerator.tilfeldigTilkjentYtelse()

        val eksternId = tilkjentYtelseRepository.save(tilkjentYtelse).eksternId

        val hentetTilkjentYtelse = tilkjentYtelseRepository.findByEksternIdOrNull(eksternId)!!

        assertEquals(tilkjentYtelse.saksnummer, hentetTilkjentYtelse.saksnummer)
    }

    @Test
    fun `Opprett andeler tilkjent ytelse`() {

        val tilkjentYtelse = DataGenerator.tilfeldigTilkjentYtelse()
        val tilkjentYtelseId = tilkjentYtelseRepository.save(tilkjentYtelse).id

        val andelTilkjentYtelse1 = DataGenerator.tilfeldigAndelTilkjentYtelse(tilkjentYtelseId)
        val andelTilkjentYtelse2 = DataGenerator.tilfeldigAndelTilkjentYtelse(tilkjentYtelseId)

        val lagredeAndelerTilkjentYtelse =
                andelTilkjentYtelseRepository.saveAll(listOf(andelTilkjentYtelse1, andelTilkjentYtelse2))

        assertEquals(2, lagredeAndelerTilkjentYtelse.count())

        lagredeAndelerTilkjentYtelse.forEach { assertTrue(it.id != 0L) }
    }

    @Test
    fun `Opprett og hent andeler tilkjent ytelse`() {

        val tilkjentYtelseId1 = tilkjentYtelseRepository.save(DataGenerator.tilfeldigTilkjentYtelse()).id
        val tilkjentYtelseId2 = tilkjentYtelseRepository.save(DataGenerator.tilfeldigTilkjentYtelse()).id

        val andelerTilkjentYtelse1 = DataGenerator.flereTilfeldigeAndelerTilkjentYtelse(tilkjentYtelseId1, 2)
        val andelerTilkjentYtelse2 = DataGenerator.flereTilfeldigeAndelerTilkjentYtelse(tilkjentYtelseId2, 4)

        andelTilkjentYtelseRepository.saveAll(andelerTilkjentYtelse1)
        andelTilkjentYtelseRepository.saveAll(andelerTilkjentYtelse2)

        val antallAndeler1 = andelTilkjentYtelseRepository.findByTilkjentYtelseId(tilkjentYtelseId1).size
        val antallAndeler2 = andelTilkjentYtelseRepository.findByTilkjentYtelseId(tilkjentYtelseId2).size

        assertEquals(2, antallAndeler1)
        assertEquals(4, antallAndeler2)
    }

    @Test
    fun `Lagre utbetalingsoppdrag`() {

        val lagretTilkjentYtelse = tilkjentYtelseRepository.save(DataGenerator.tilfeldigTilkjentYtelse())
        val andelerTilkjentYtelse = DataGenerator.flereTilfeldigeAndelerTilkjentYtelse(lagretTilkjentYtelse.id, 2)

        val utbetalingsoppdrag = lagUtbetalingsoppdrag("saksbehandler", lagretTilkjentYtelse, andelerTilkjentYtelse)

        tilkjentYtelseRepository.save(lagretTilkjentYtelse.copy(utbetalingsoppdrag = utbetalingsoppdrag))

        val oppdatertTilkjentYtelse = tilkjentYtelseRepository.findByIdOrNull(lagretTilkjentYtelse.id)!!

        assertEquals(utbetalingsoppdrag, oppdatertTilkjentYtelse.utbetalingsoppdrag)
    }

    @Test
    fun `Finn tilkjent ytelse på personident`() {

        val tilkjentYtelse = DataGenerator.tilfeldigTilkjentYtelse()
        val lagretTilkjentYtelse = tilkjentYtelseRepository.save(tilkjentYtelse)

        val hentetTilkjentYtelse = tilkjentYtelseRepository.findByPersonIdentifikatorOrNull(tilkjentYtelse.personIdentifikator)

        assertEquals(lagretTilkjentYtelse, hentetTilkjentYtelse)
        assertNull(tilkjentYtelseRepository.findByPersonIdentifikatorOrNull("Finnes ikke"))
    }

}