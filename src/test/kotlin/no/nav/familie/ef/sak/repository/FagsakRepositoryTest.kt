package no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.fagsakpersoner
import no.nav.familie.ef.sak.repository.FagsakRepository
import no.nav.familie.ef.sak.repository.domain.FagsakPerson
import no.nav.familie.ef.sak.repository.domain.Sporbar
import no.nav.familie.ef.sak.repository.domain.Stønadstype
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import java.time.LocalDateTime
import java.util.UUID

internal class FagsakRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired private lateinit var fagsakRepository: FagsakRepository
    @Autowired private lateinit var behandlingRepository: BehandlingRepository

    @Test
    internal fun findByFagsakId() {
        val fagsakPersistert = fagsakRepository.insert(fagsak(fagsakpersoner(setOf("12345678901", "98765432109"))))
        val fagsak = fagsakRepository.findByIdOrNull(fagsakPersistert.id) ?: error("Finner ikke fagsak med id")

        assertThat(fagsak).isNotNull
        assertThat(fagsak.søkerIdenter).isNotEmpty
        assertThat(fagsak.søkerIdenter.map { it.ident }).contains("12345678901")
        assertThat(fagsak.søkerIdenter.map { it.ident }).contains("98765432109")
    }

    @Test
    internal fun findBySøkerIdent() {
        fagsakRepository.insert(fagsak(fagsakpersoner(setOf("12345678901", "98765432109"))))
        val fagsakHentetFinnesIkke = fagsakRepository.findBySøkerIdent("0", Stønadstype.OVERGANGSSTØNAD)

        assertThat(fagsakHentetFinnesIkke).isNull()

        val fagsak = fagsakRepository.findBySøkerIdent("12345678901", Stønadstype.OVERGANGSSTØNAD) ?: error("Finner ikke fagsak")

        assertThat(fagsak.søkerIdenter.map { it.ident }).contains("12345678901")
        assertThat(fagsak.søkerIdenter.map { it.ident }).contains("98765432109")
    }

    @Test
    internal fun `skal returnere en liste med fagsaker hvis stønadstypen ikke satt`() {
        val fagsakPerson = fagsakpersoner(setOf("12345678901"))
        var fagsak1 = fagsak(identer = fagsakPerson, stønadstype = Stønadstype.OVERGANGSSTØNAD)
        var fagsak2 = fagsak(identer = fagsakPerson, stønadstype = Stønadstype.SKOLEPENGER)
        fagsak1 = fagsakRepository.insert(fagsak1)
        fagsak2 = fagsakRepository.insert(fagsak2)
        val fagsaker = fagsakRepository.findBySøkerIdent("12345678901")

        assertThat(fagsaker.forEach { fagsak ->
            assertThat(fagsak.søkerIdenter.size).isEqualTo(1)
            assertThat(fagsak.søkerIdenter.map { it.ident }).contains("12345678901")
        })

        assertThat(fagsaker.map { it.stønadstype }).contains(Stønadstype.SKOLEPENGER)
        assertThat(fagsaker.map { it.stønadstype }).contains(Stønadstype.OVERGANGSSTØNAD)
        assertThat(fagsaker).containsExactlyInAnyOrder(fagsak1, fagsak2)
    }

    @Test
    internal fun finnMedEksternId() {
        val fagsak = fagsakRepository.insert(fagsak())
        val findByEksternId = fagsakRepository.finnMedEksternId(fagsak.eksternId.id)
                              ?: throw error("Fagsak med ekstern id ${fagsak.eksternId} finnes ikke")

        assertThat(findByEksternId).isEqualTo(fagsak)
    }

    @Test
    internal fun `finnMedEksternId skal gi null når det ikke finnes fagsak for gitt id`() {
        val findByEksternId = fagsakRepository.finnMedEksternId(100000L)
        assertThat(findByEksternId).isEqualTo(null)
    }

    @Test
    internal fun `finnAktivIdent - skal finne aktiv ident`() {
        val fagsak = fagsak(setOf(FagsakPerson(ident = "1"),
                                  FagsakPerson(ident = "2", sporbar = Sporbar(opprettetTid = LocalDateTime.now().plusDays(2))),
                                  FagsakPerson(ident = "3")))
        fagsakRepository.insert(fagsak)
        assertThat(fagsakRepository.finnAktivIdent(fagsak.id)).isEqualTo("2")
    }

    @Test
    internal fun `skal hente fagsak på behandlingId`() {
        var fagsak = fagsak(setOf(FagsakPerson(ident = "1"),
                                  FagsakPerson(ident = "2", sporbar = Sporbar(opprettetTid = LocalDateTime.now().plusDays(2))),
                                  FagsakPerson(ident = "3")))
        fagsak = fagsakRepository.insert(fagsak)
        val behandling = behandlingRepository.insert(behandling(fagsak))

        val finnFagsakTilBehandling = fagsakRepository.finnFagsakTilBehandling(behandling.id)!!

        assertThat(finnFagsakTilBehandling.id).isEqualTo(fagsak.id)
        assertThat(finnFagsakTilBehandling.søkerIdenter).hasSize(3)
        assertThat(finnFagsakTilBehandling.eksternId).isEqualTo(fagsak.eksternId)
    }
}