package no.nav.familie.ef.sak.ekstern

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.fagsakpersoner
import no.nav.familie.kontrakter.felles.klage.IkkeOpprettetÅrsak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class EksternBehandlingServiceTest: OppslagSpringRunnerTest() {

    @Autowired
    lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    lateinit var eksternBehandlingService: EksternBehandlingService

    val fagsakUtenBehandling = fagsak(fagsakpersoner("1"))
    val fagsakMedÅpenBehandling = fagsak(fagsakpersoner("2"))
    val fagsakMedFerdigstiltBehandling = fagsak(fagsakpersoner("3"))

    @BeforeEach
    internal fun setUp() {
        testoppsettService.lagreFagsak(fagsakUtenBehandling)
        testoppsettService.lagreFagsak(fagsakMedÅpenBehandling)
        testoppsettService.lagreFagsak(fagsakMedFerdigstiltBehandling)

        behandlingRepository.insert(behandling(fagsakMedÅpenBehandling))
        behandlingRepository.insert(behandling(fagsakMedFerdigstiltBehandling, status = BehandlingStatus.FERDIGSTILT))
    }

    @Nested
    inner class opprettRevurderingKlage {

        @Test
        internal fun `kan opprette revurdering hvis det finnes en ferdigstilt behandling`() {
            val result = eksternBehandlingService.opprettRevurderingKlage(fagsakMedFerdigstiltBehandling.eksternId.id)

            assertThat(result.opprettetBehandling).isTrue

            val behandling = behandlingRepository.finnMedEksternId(result.opprettet!!.eksternBehandlingId.toLong())

            assertThat(behandling).isNotNull
        }

        @Test
        internal fun `hva skal skje hvis det ikke finnes noen behandling`() {
            TODO("Not yet implemented")
        }

        @Test
        internal fun `kan ikke opprette recvurdering hvis det finnes åpen behandling`() {
            val result = eksternBehandlingService.opprettRevurderingKlage(fagsakMedÅpenBehandling.eksternId.id)
            assertThat(result.opprettetBehandling).isFalse
            assertThat(result.ikkeOpprettet!!.årsak).isEqualTo(IkkeOpprettetÅrsak.ÅPEN_BEHANDLING)
        }


    }


}