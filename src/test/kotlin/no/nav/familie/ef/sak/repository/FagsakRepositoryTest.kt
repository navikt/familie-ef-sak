package no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.repository.FagsakRepository
import no.nav.familie.ef.sak.repository.domain.Stønadstype
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("local", "mock-oauth")
internal class FagsakRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired private lateinit var fagsakRepository: FagsakRepository

    @Test
    internal fun `findByFagsakId`() {
        val fagsakPersistert = fagsakRepository.insert(fagsak(fagsakpersoner(setOf("12345678901", "98765432109"))))
        val fagsak = fagsakRepository.findByIdOrNull(fagsakPersistert.id) ?: error("Finner ikke fagsak med id")

        assertThat(fagsak).isNotNull
        assertThat(fagsak.søkerIdenter).isNotEmpty
        assertThat(fagsak.søkerIdenter.map { it.ident }).contains("12345678901")
        assertThat(fagsak.søkerIdenter.map { it.ident }).contains("98765432109")
    }

    @Test
    internal fun `findBySøkerIdent`() {
        fagsakRepository.insert(fagsak(fagsakpersoner(setOf("12345678901", "98765432109"))))
        val fagsakHentetFinnesIkke = fagsakRepository.findBySøkerIdent("0", Stønadstype.OVERGANGSSTØNAD)

        assertThat(fagsakHentetFinnesIkke).isNull()

        val fagsak = fagsakRepository.findBySøkerIdent("12345678901", Stønadstype.OVERGANGSSTØNAD) ?: error("Finner ikke fagsak")

        assertThat(fagsak.søkerIdenter.map { it.ident }).contains("12345678901")
        assertThat(fagsak.søkerIdenter.map { it.ident }).contains("98765432109")
    }

}