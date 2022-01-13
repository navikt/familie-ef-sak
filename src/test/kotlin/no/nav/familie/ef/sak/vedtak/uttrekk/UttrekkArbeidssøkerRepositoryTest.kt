package no.nav.familie.ef.sak.vedtak.uttrekk

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.fagsak.FagsakRepository
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Sort
import java.time.YearMonth

internal class UttrekkArbeidssøkerRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired private lateinit var fagsakRepository: FagsakRepository
    @Autowired private lateinit var behandlingRepository: BehandlingRepository
    @Autowired private lateinit var repository: UttrekkArbeidssøkerRepository

    private val årMåned = YearMonth.of(2021, 1)

    @BeforeEach
    internal fun setUp() {
        val fagsak = fagsakRepository.insert(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak))
        for (i in 1..10) {
            repository.insert(UttrekkArbeidssøkere(fagsakId = fagsak.id,
                                                   vedtakId = behandling.id,
                                                   årMåned = årMåned,
                                                   registrertArbeidssøker = false))
        }

        for (i in 1..5) {
            repository.insert(UttrekkArbeidssøkere(fagsakId = fagsak.id,
                                                   vedtakId = behandling.id,
                                                   årMåned = årMåned.plusMonths(1),
                                                   kontrollert = true,
                                                   registrertArbeidssøker = false))
        }
        for (i in 1..2) {
            repository.insert(UttrekkArbeidssøkere(fagsakId = fagsak.id,
                                                   vedtakId = behandling.id,
                                                   årMåned = årMåned.plusMonths(1),
                                                   registrertArbeidssøker = true))
        }
    }

    @Test
    internal fun `skal hente uttrekk for gitt måned`() {
        assertThat(repository.findAllByÅrMånedAndRegistrertArbeidssøkerIsFalse(årMåned.minusMonths(1))).isEmpty()
        assertThat(repository.findAllByÅrMånedAndRegistrertArbeidssøkerIsFalse(årMåned)).hasSize(10)
        assertThat(repository.findAllByÅrMånedAndRegistrertArbeidssøkerIsFalse(årMåned.plusMonths(1))).hasSize(5)
        assertThat(repository.findAllByÅrMånedAndRegistrertArbeidssøkerIsFalse(årMåned.plusMonths(2))).isEmpty()
    }
}
