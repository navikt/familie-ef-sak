package no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.repository.BehandlingRepository
import no.nav.familie.ef.sak.repository.FagsakRepository
import no.nav.familie.ef.sak.repository.SøknadRepository
import no.nav.familie.ef.sak.repository.domain.Sporbar
import no.nav.familie.ef.sak.repository.domain.Søker
import no.nav.familie.ef.sak.repository.domain.Søknad
import no.nav.familie.ef.sak.repository.domain.SøknadType
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
                soknadsskjemaId = UUID.randomUUID(),
                behandlingId = behandlingId,
                type = SøknadType.OVERGANGSSTØNAD,
                saksnummerInfotrygd = saksnummer,
                søker = Søker(fødselsnummer, "Navn"),
                journalpostId = "journalId$saksnummer",
                sporbar = Sporbar(opprettetTid = LocalDateTime.of(MathUtils.randomInt(2020),
                                                                  MathUtils.randomInt(11) + 1,
                                                                  MathUtils.randomInt(27) + 1,
                                                                  MathUtils.randomInt(23),
                                                                  MathUtils.randomInt(59))),
        relaterteFnr = setOf("654654654")))
    }
}
