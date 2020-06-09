package no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.repository.CustomRepository
import no.nav.familie.ef.sak.repository.SakRepository
import no.nav.familie.ef.sak.repository.domain.Barn
import no.nav.familie.ef.sak.repository.domain.Sak
import no.nav.familie.ef.sak.repository.domain.Søker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import java.time.LocalDate

@ActiveProfiles("local", "mock-auth", "mock-oauth")
@TestPropertySource(properties = ["FAMILIE_INTEGRASJONER_URL=http://localhost:28085"])
internal class SakRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired lateinit var sakRepository: SakRepository
    @Autowired lateinit var customRepository: CustomRepository<Sak>

    @BeforeEach
    internal fun setUp() {
        customRepository.jdbcAggregateOperations.deleteAll(Sak::class.java)
    }

    @Test
    fun `finner 1 sak på fødselsnummer`() {
        opprettSak("1", "11111122222")

        val saker = sakRepository.findBySøkerFødselsnummer("11111122222")
        assertThat(saker).hasSize(1)
        assertThat(saker[0].barn).isNotEmpty
        assertThat(saker[0].søker).isNotNull
    }

    @Test
    fun `finner 2 saker på fødselsnummer`() {
        opprettSak("1", "11111122222")
        opprettSak("2", "11111122222")
        opprettSak("3", "22222211111")

        val saker = sakRepository.findBySøkerFødselsnummer("11111122222")
        assertThat(saker).hasSize(2)
        assertThat(saker.filter { it.saksnummer == "1" }).hasSize(1)
        assertThat(saker.filter { it.saksnummer == "2" }).hasSize(1)
    }

    @Test
    fun `finner ingen saker på fødselsnummer`() {
        val saker = sakRepository.findBySøkerFødselsnummer("11111122222")
        assertThat(saker.size).isEqualTo(0)
    }

    private fun opprettSak(saksnummer: String, fødselsnummer: String) {
        customRepository.persist(Sak(
                søknad = byteArrayOf(12),
                saksnummer = saksnummer,
                søker = Søker(fødselsnummer, "Navn"),
                barn = setOf(Barn(fødselsdato = LocalDate.now(), harSammeAdresse = true, fødselsnummer = null, navn = "Navn")),
                journalpostId = "journalId$saksnummer"
        ))
    }
}
