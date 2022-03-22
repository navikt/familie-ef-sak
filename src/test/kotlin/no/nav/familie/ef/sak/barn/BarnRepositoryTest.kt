package no.nav.familie.ef.sak.no.nav.familie.ef.sak.barn

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.barn.BarnRepository
import no.nav.familie.ef.sak.barn.BehandlingBarn
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import java.io.InvalidClassException
import java.time.LocalDate
import java.util.UUID

class BarnRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired private lateinit var behandlingRepository: BehandlingRepository
    @Autowired private lateinit var barnRepository: BarnRepository

    private val personIdent = "12345678901"
    private val navn = "Test Barnesen"
    private val fødselTermindato = LocalDate.now()

    @Test
    internal fun `skal lagre ned barn med personIdent`() {
        lagreOgVerifiserBarn(personIdent = personIdent, navn = navn, fødselTermindato = null)
    }

    @Test
    internal fun `skal lagre ned barn med termindato`() {
        lagreOgVerifiserBarn(personIdent = null, navn = null, fødselTermindato = fødselTermindato)
    }

    @Test
    internal fun `skal lagre ned barn med personIdent og termindato`() {
        lagreOgVerifiserBarn(personIdent = personIdent, navn = navn, fødselTermindato = fødselTermindato)
    }

    @Test
    internal fun `barn må ha ident eller termindato`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak))
        val nyttBarn = BehandlingBarn(id = UUID.randomUUID(),
                                      behandlingId = behandling.id,
                                      søknadBarnId = UUID.randomUUID(),
                                      personIdent = null,
                                      navn = "A",
                                      fødselTermindato = null)
        val cause = assertThatThrownBy {
            barnRepository.insert(nyttBarn)
        }.cause
        cause.isInstanceOf(DataIntegrityViolationException::class.java)
        cause.hasMessageContaining("violates check constraint \"behandling_barn_ident_fodsel_termindato_check\"")
    }

    private fun lagreOgVerifiserBarn(personIdent: String?, navn: String?, fødselTermindato: LocalDate?) {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak))
        val nyttBarn = BehandlingBarn(id = UUID.randomUUID(),
                                      behandlingId = behandling.id,
                                      søknadBarnId = UUID.randomUUID(),
                                      personIdent = personIdent,
                                      navn = navn,
                                      fødselTermindato = fødselTermindato)
        barnRepository.insert(nyttBarn)
        val barnet = barnRepository.findByIdOrThrow(nyttBarn.id)
        assertThat(barnet).usingRecursiveComparison().ignoringFields("sporbar").isEqualTo(nyttBarn)

        val barnForBehandling = barnRepository.findByBehandlingId(nyttBarn.behandlingId)
        assertThat(barnForBehandling).hasSize(1)
        assertThat(barnForBehandling.first()).usingRecursiveComparison().ignoringFields("sporbar").isEqualTo(nyttBarn)
    }

}