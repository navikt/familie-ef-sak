package no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.api.external.Testsøknad.søknad
import no.nav.familie.ef.sak.repository.CustomRepository
import no.nav.familie.ef.sak.repository.SakRepository
import no.nav.familie.ef.sak.repository.domain.*
import no.nav.familie.kontrakter.felles.objectMapper
import org.assertj.core.api.Assertions.assertThat
import org.h2.util.MathUtils
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import java.time.LocalDate
import java.time.LocalDateTime

@ActiveProfiles("local", "mock-auth", "mock-oauth")
@TestPropertySource(properties = ["FAMILIE_INTEGRASJONER_URL=http://localhost:28085"])
internal class SakRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired lateinit var sakRepository: SakRepository
    @Autowired lateinit var customRepository: CustomRepository<Sak>

    @Test
    fun `finner 1 sak når vi henter top 10`() {
        opprettSak("1", "11111122222")

        val saker = sakRepository.findTop10ByOrderBySporbar_OpprettetTidDesc()

        assertThat(saker).hasSize(1)
    }

    @Test
    fun `finner 10 sorterte saker når vi henter top 10`() {
        repeat(12) {
            opprettSak("1", "11111122222")
        }

        val saker = sakRepository.findTop10ByOrderBySporbar_OpprettetTidDesc()

        assertThat(saker).hasSize(10)
        var forrigeSaksOpprettetTid = LocalDateTime.MAX
        saker.forEach {
            assertThat(it.sporbar.opprettetTid).isBefore(forrigeSaksOpprettetTid)
            forrigeSaksOpprettetTid = it.sporbar.opprettetTid
        }
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
                søknad = objectMapper.writeValueAsBytes(søknad),
                type = SøknadType.OVERGANGSSTØNAD,
                saksnummer = saksnummer,
                søker = Søker(fødselsnummer, "Navn"),
                barn = setOf(Barn(fødselsdato = LocalDate.now(), harSammeAdresse = true, fødselsnummer = null, navn = "Navn")),
                journalpostId = "journalId$saksnummer",
                sporbar = Sporbar(opprettetTid = LocalDateTime.of(MathUtils.randomInt(2020),
                                                                  MathUtils.randomInt(11) + 1,
                                                                  MathUtils.randomInt(27) + 1,
                                                                  MathUtils.randomInt(23),
                                                                  MathUtils.randomInt(59)))
        ))
    }
}
