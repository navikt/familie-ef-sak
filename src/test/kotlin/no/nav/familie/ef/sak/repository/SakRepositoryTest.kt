package no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.repository.CustomRepository
import no.nav.familie.ef.sak.repository.SakRepository
import no.nav.familie.ef.sak.repository.domain.Barn
import no.nav.familie.ef.sak.repository.domain.Sak
import no.nav.familie.ef.sak.repository.domain.Søker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import java.time.LocalDate

@ActiveProfiles("local", "mock-auth", "mock-oauth")
@TestPropertySource(properties = ["FAMILIE_INTEGRASJONER_URL=http://localhost:28085"])
internal class SakRepositoryTest : OppslagSpringRunnerTest(){

    @Autowired lateinit var sakRepository: SakRepository
    @Autowired lateinit var customRepository: CustomRepository<Sak>

    @Test
    fun `finner saker på fødelsnummer`() {
        val sak = Sak(
                søknad = byteArrayOf(12),
                saksnummer = "1",
                søker = Søker("11111122222", "Navn"),
                barn = setOf(Barn(fødselsdato = LocalDate.now(), harSammeAdresse = true, fødselsnummer = null, navn = "Navn")),
                journalpostId = "journalId"
        )
        customRepository.persist(sak)

        val saker = sakRepository.findBySøkerFødselsnummer("11111122222")
        assertThat(saker.count()).isEqualTo(1)
        assertThat(saker[0].barn).isNotEmpty
        assertThat(saker[0].søker).isNotNull
    }
}