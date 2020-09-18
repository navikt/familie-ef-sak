package no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.BehandlingRepository
import no.nav.familie.ef.sak.repository.CustomRepository
import no.nav.familie.ef.sak.repository.FagsakRepository
import no.nav.familie.ef.sak.repository.domain.BehandlingStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.context.ActiveProfiles
import java.util.*

@ActiveProfiles("local", "mock-oauth")
internal class FagsakRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired private lateinit var customRepository: CustomRepository
    @Autowired private lateinit var fagsakRepository: FagsakRepository

    @Test
    internal fun `findByFagsakId`() {
        val fagsak = customRepository.persist(fagsak(fagsakpersoner(setOf("12345678901", "98765432109"))))
        val fagsakPersistert = fagsakRepository.findByIdOrNull(fagsak.id) ?: error("Finner ikke fagsak med id")

        assertThat(fagsakPersistert).isNotNull()
        assertThat(fagsakPersistert.søkerIdenter).isNotEmpty()
        assertThat(fagsakPersistert.søkerIdenter.map { it.ident }).contains("12345678901")
        assertThat(fagsakPersistert.søkerIdenter.map { it.ident }).contains("98765432109")
    }

}