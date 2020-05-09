package no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.common.DbContainerInitializer
import no.nav.familie.ef.sak.config.ApplicationConfig
import no.nav.familie.ef.sak.repository.domain.Sak
import no.nav.familie.ef.sak.repository.domain.Søker
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDate

@SpringBootTest(classes = [ApplicationConfig::class])
@ExtendWith(SpringExtension::class)
@ContextConfiguration(initializers = [DbContainerInitializer::class])
@ActiveProfiles("postgres","mock-integrasjoner","mock-oauth", "mock-auth")
@Tag("integration")
internal class TilkjentYtelseRepositoryTest {

    @Autowired
    private lateinit var sakRepository: SakRepository

    @Test
    fun test() {

        val sak = Sak(
                søknad = ByteArray(1024),
                saksnummer = "saksnr",
                journalpostId = "jpid",
                søker = Søker("12345678910","Navn Navnsen"),
                barn = emptySet()
        )

        val sakId = sakRepository.save(sak).id

        val hentetTilkjentYtelse = sakRepository.findById(sakId)
    }


}