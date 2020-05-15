package no.nav.familie.ef.sak.økonomi

import no.nav.familie.ef.sak.common.DbContainerInitializer
import no.nav.familie.ef.sak.config.ApplicationConfig
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.økonomi.DataGenerator
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@SpringBootTest(classes = [ApplicationConfig::class])
@ExtendWith(SpringExtension::class)
@ContextConfiguration(initializers = [DbContainerInitializer::class])
@ActiveProfiles("postgres", "mock-integrasjoner", "mock-oauth", "mock-auth")
@Tag("integration")
internal class TilkjentYtelseRepositoryTest {

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

        val antallAbdeler1 = andelTilkjentYtelseRepository.findByTilkjentYtelseId(tilkjentYtelseId1).size
        val antallAbdeler2 = andelTilkjentYtelseRepository.findByTilkjentYtelseId(tilkjentYtelseId2).size

        assertEquals(2, antallAbdeler1)
        assertEquals(4, antallAbdeler2)
    }
}