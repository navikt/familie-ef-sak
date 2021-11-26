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
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import java.time.YearMonth

internal class UttrekkArbeidssøkerRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired private lateinit var fagsakRepository: FagsakRepository
    @Autowired private lateinit var behandlingRepository: BehandlingRepository
    @Autowired private lateinit var repository: UttrekkArbeidssøkerRepository

    private val årMåned = YearMonth.of(2021, 1)
    private val sort = Sort.by("id")

    @BeforeEach
    internal fun setUp() {
        val fagsak = fagsakRepository.insert(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak))
        for (i in 1..10) {
            repository.insert(UttrekkArbeidssøkere(fagsakId = fagsak.id,
                                                   vedtakId = behandling.id,
                                                   årMåned = årMåned))
        }

        for (i in 1..5) {
            repository.insert(UttrekkArbeidssøkere(fagsakId = fagsak.id,
                                                   vedtakId = behandling.id,
                                                   årMåned = årMåned,
                                                   kontrollert = true))
        }
    }

    @Test
    internal fun `antallKontrollert skal returnere de som er kontrollert`() {
        assertThat(repository.countByÅrMånedAndKontrollertIsTrue(årMåned)).isEqualTo(5)
        assertThat(repository.countByÅrMånedAndKontrollertIsTrue(årMåned.minusMonths(1))).isEqualTo(0)
    }

    @Test
    internal fun `skal hente paginerte arbeidsssøkere`() {
        with(repository.findAllByÅrMåned(årMåned, PageRequest.of(0, 100, sort))) {
            assertThat(this).hasSize(15)
            assertThat(this.totalElements).isEqualTo(15)
        }
        with(repository.findAllByÅrMåned(årMåned, PageRequest.of(0, 2, sort))) {
            assertThat(this).hasSize(2)
            assertThat(this.totalElements).isEqualTo(15)
        }
        with(repository.findAllByÅrMåned(årMåned, PageRequest.of(100, 2, sort))) {
            assertThat(this).isEmpty()
            assertThat(this.totalElements).isEqualTo(15)
        }
    }

    @Test
    internal fun `skal ikke finnes noen resultat for en annen måned`() {
        with(repository.findAllByÅrMåned(årMåned.minusMonths(1), PageRequest.of(100, 2, sort))) {
            assertThat(this).isEmpty()
            assertThat(this.totalElements).isEqualTo(0)
        }
    }

    @Test
    internal fun `skal hente paginerte arbeidsssøkere som er kontrollert`() {
        assertThat(repository.findAllByÅrMånedAndKontrollertIsFalse(årMåned, PageRequest.of(0, 100, sort))).hasSize(10)
        assertThat(repository.findAllByÅrMånedAndKontrollertIsFalse(årMåned, PageRequest.of(1, 6, sort))).hasSize(4)
        assertThat(repository.findAllByÅrMånedAndKontrollertIsFalse(årMåned, PageRequest.of(100, 2, sort))).isEmpty()
    }

}
