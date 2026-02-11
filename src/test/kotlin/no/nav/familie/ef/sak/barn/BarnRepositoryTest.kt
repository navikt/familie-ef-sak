package no.nav.familie.ef.sak.barn

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.postgresql.util.PSQLException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import java.time.LocalDate
import java.util.UUID

class BarnRepositoryTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var barnRepository: BarnRepository

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
    internal fun `kan ikke ha 2 barn med samme ident på samme behandling`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak))

        barnRepository.insert(lagBarn(behandling, personIdent = personIdent))

        val cause =
            assertThatThrownBy {
                barnRepository.insert(lagBarn(behandling, personIdent = personIdent))
            }.cause()
        cause.isInstanceOf(PSQLException::class.java)
        cause.hasMessageContaining("duplicate key value violates unique constraint")
    }

    @Test
    internal fun `kan ha 2 barn med ulik ident på ulike behandlinger`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak, status = BehandlingStatus.FERDIGSTILT))
        val behandling2 = behandlingRepository.insert(behandling(fagsak))

        barnRepository.insert(lagBarn(behandling, personIdent = "1"))
        barnRepository.insert(lagBarn(behandling, personIdent = "2"))

        barnRepository.insert(lagBarn(behandling2, personIdent = "1"))
        barnRepository.insert(lagBarn(behandling2, personIdent = "2"))
    }

    @Test
    internal fun `kan ha 2 barn med termindato på ulike behandlinger`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak, status = BehandlingStatus.FERDIGSTILT))
        val behandling2 = behandlingRepository.insert(behandling(fagsak))

        barnRepository.insert(lagBarn(behandling, fødselTermindato = LocalDate.now()))
        barnRepository.insert(lagBarn(behandling, fødselTermindato = LocalDate.now()))

        barnRepository.insert(lagBarn(behandling2, fødselTermindato = LocalDate.now()))
        barnRepository.insert(lagBarn(behandling2, fødselTermindato = LocalDate.now()))
    }

    @Test
    internal fun `barn må ha ident eller termindato`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak))
        val nyttBarn = lagBarn(behandling, navn = "A")

        val cause =
            assertThatThrownBy {
                barnRepository.insert(nyttBarn)
            }.cause()

        cause.isInstanceOf(PSQLException::class.java)
        cause.hasMessageContaining("violates check constraint \"behandling_barn_ident_fodsel_termindato_check\"")
    }

    private fun lagreOgVerifiserBarn(
        personIdent: String?,
        navn: String?,
        fødselTermindato: LocalDate?,
    ) {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak))
        val nyttBarn = lagBarn(behandling, personIdent, navn, fødselTermindato)

        barnRepository.insert(nyttBarn)
        val barnet = barnRepository.findByIdOrThrow(nyttBarn.id)
        assertThat(barnet).usingRecursiveComparison().ignoringFields("sporbar").isEqualTo(nyttBarn)

        val barnForBehandling = barnRepository.findByBehandlingId(nyttBarn.behandlingId)
        assertThat(barnForBehandling).hasSize(1)
        assertThat(barnForBehandling.first()).usingRecursiveComparison().ignoringFields("sporbar").isEqualTo(nyttBarn)
    }

    private fun lagBarn(
        behandling: Behandling,
        personIdent: String? = null,
        navn: String? = null,
        fødselTermindato: LocalDate? = null,
    ) = BehandlingBarn(
        behandlingId = behandling.id,
        søknadBarnId = UUID.randomUUID(),
        personIdent = personIdent,
        navn = navn,
        fødselTermindato = fødselTermindato,
    )
}
