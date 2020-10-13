package no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.Testsøknad.søknad
import no.nav.familie.ef.sak.repository.BehandlingRepository
import no.nav.familie.ef.sak.repository.FagsakRepository
import no.nav.familie.ef.sak.repository.SøknadRepository
import no.nav.familie.ef.sak.repository.domain.*
import no.nav.familie.kontrakter.felles.objectMapper
import org.assertj.core.api.Assertions.assertThat
import org.h2.util.MathUtils
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class SøknadRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired lateinit var søknadRepository: SøknadRepository
    @Autowired private lateinit var fagsakRepository: FagsakRepository
    @Autowired private lateinit var behandlingRepository: BehandlingRepository

    @Test
    fun `finner søknad på behandlingId`() {
        val fagsak = fagsakRepository.insert(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak))
        opprettSøknad("1", "11111122222", behandling.id)

        val søknad = søknadRepository.findByBehandlingId(behandling.id)
        assertThat(søknad).isNotNull
    }

    private fun opprettSøknad(saksnummer: String, fødselsnummer: String, behandlingId: UUID) {
        søknadRepository.insert(Søknad(
                søknad = objectMapper.writeValueAsBytes(søknad),
                behandlingId = behandlingId,
                type = SøknadType.OVERGANGSSTØNAD,
                saksnummerInfotrygd = saksnummer,
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
