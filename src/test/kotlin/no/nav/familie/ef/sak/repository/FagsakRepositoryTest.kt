package no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.fagsak.FagsakRepository
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.fagsak.domain.FagsakPersonOld
import no.nav.familie.ef.sak.fagsak.domain.Stønadstype
import no.nav.familie.ef.sak.fagsak.domain.tilFagsak
import no.nav.familie.ef.sak.felles.domain.Endret
import no.nav.familie.ef.sak.felles.domain.Sporbar
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import java.time.LocalDateTime

internal class FagsakRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired private lateinit var fagsakRepository: FagsakRepository
    @Autowired private lateinit var behandlingRepository: BehandlingRepository

    @Test
    internal fun findByFagsakId() {
        val fagsakPersistert = testoppsettService.lagreFagsak(fagsak(fagsakpersoner(setOf("12345678901", "98765432109"))))
        val fagsak = fagsakRepository.findByIdOrNull(fagsakPersistert.id) ?: error("Finner ikke fagsak med id")

        assertThat(fagsak).isNotNull
        assertThat(fagsak.søkerIdenter).isNotEmpty
        assertThat(fagsak.søkerIdenter.map { it.ident }).contains("12345678901")
        assertThat(fagsak.søkerIdenter.map { it.ident }).contains("98765432109")
    }

    @Test
    internal fun findBySøkerIdent() {
        testoppsettService.lagreFagsak(fagsak(fagsakpersoner(setOf("12345678901", "98765432109"))))
        val fagsakHentetFinnesIkke = fagsakRepository.findBySøkerIdent(setOf("0"), Stønadstype.OVERGANGSSTØNAD)

        assertThat(fagsakHentetFinnesIkke).isNull()

        val fagsak = fagsakRepository.findBySøkerIdent(setOf("12345678901"), Stønadstype.OVERGANGSSTØNAD)
                     ?: error("Finner ikke fagsak")

        assertThat(fagsak.søkerIdenter.map { it.ident }).contains("12345678901")
        assertThat(fagsak.søkerIdenter.map { it.ident }).contains("98765432109")
    }

    @Test
    internal fun `skal returnere en liste med fagsaker hvis stønadstypen ikke satt`() {
        val fagsakPerson = fagsakpersoner(setOf("12345678901"))
        var fagsak1 = testoppsettService.lagreFagsak(fagsak(identer = fagsakPerson, stønadstype = Stønadstype.OVERGANGSSTØNAD))
        var fagsak2 = testoppsettService.lagreFagsak(fagsak(identer = fagsakPerson, stønadstype = Stønadstype.SKOLEPENGER))
        val fagsaker = fagsakRepository.findBySøkerIdent(setOf("12345678901")).map { it.tilFagsak() }

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
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val findByEksternId = fagsakRepository.finnMedEksternId(fagsak.eksternId.id)?.tilFagsak()
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
        val fagsak = opprettFagsakMedFlereIdenter()
        testoppsettService.lagreFagsak(fagsak)
        assertThat(fagsakRepository.finnAktivIdent(fagsak.id)).isEqualTo("2")
    }

    @Test
    internal fun `skal hente fagsak på behandlingId`() {
        var fagsak = opprettFagsakMedFlereIdenter()
        fagsak = testoppsettService.lagreFagsak(fagsak)
        val behandling = behandlingRepository.insert(behandling(fagsak))

        val finnFagsakTilBehandling = fagsakRepository.finnFagsakTilBehandling(behandling.id)!!

        assertThat(finnFagsakTilBehandling.id).isEqualTo(fagsak.id)
        assertThat(finnFagsakTilBehandling.søkerIdenter).hasSize(3)
        assertThat(finnFagsakTilBehandling.eksternId).isEqualTo(fagsak.eksternId)
    }

    @Test
    internal fun `skal sette eksternId til 200_000_000 som default`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        assertThat(fagsak.eksternId.id).isGreaterThanOrEqualTo(200_000_000)
    }

    @Test
    internal fun `skal hente siste identen for hver fagsak`() {
        val fagsak = testoppsettService.lagreFagsak(opprettFagsakMedFlereIdenter())
        val fagsak2 = testoppsettService.lagreFagsak(opprettFagsakMedFlereIdenter("4", "5", "6"))
        val aktiveIdenterPerFagsak = fagsakRepository.finnAktivIdenter(setOf(fagsak.id, fagsak2.id))
        assertThat(aktiveIdenterPerFagsak).hasSize(2)
        assertThat(aktiveIdenterPerFagsak.single { it.first == fagsak.id }.second).isEqualTo("2")
        assertThat(aktiveIdenterPerFagsak.single { it.first == fagsak2.id }.second).isEqualTo("5")
    }

    @Test
    internal fun `skal kunne søke opp fagsak basert på forskjellige personidenter - kun ett treff per fagsak`() {
        val fagsakMedFlereIdenter = testoppsettService.lagreFagsak(opprettFagsakMedFlereIdenter("4", "5", "6"))

        assertThat(fagsakMedFlereIdenter.søkerIdenter).hasSize(3)
        assertThat(fagsakRepository.findBySøkerIdent(fagsakMedFlereIdenter.søkerIdenter.map { it.ident }.toSet(), Stønadstype.OVERGANGSSTØNAD)).isNotNull
        assertThat(fagsakRepository.findBySøkerIdent(setOf(fagsakMedFlereIdenter.søkerIdenter.map { it.ident }.first()))).hasSize(1)
        assertThat(fagsakRepository.findBySøkerIdent(fagsakMedFlereIdenter.søkerIdenter.map { it.ident }.toSet())).hasSize(1)
    }

    private fun opprettFagsakMedFlereIdenter(ident: String = "1", ident2: String = "2", ident3: String = "3"): Fagsak {
        val endret2DagerSiden = Sporbar(endret = Endret(endretTid = LocalDateTime.now().plusDays(2)))
        return fagsak(setOf(FagsakPersonOld(ident = ident),
                            FagsakPersonOld(ident = ident2, sporbar = endret2DagerSiden),
                            FagsakPersonOld(ident = ident3)))
    }
}