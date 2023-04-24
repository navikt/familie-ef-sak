package no.nav.familie.ef.sak.ekstern

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.felles.util.BrukerContextUtil.testWithBrukerContext
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.fagsakpersoner
import no.nav.familie.ef.sak.repository.vilkårsvurdering
import no.nav.familie.ef.sak.vilkår.VilkårsvurderingRepository
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.felles.klage.IkkeOpprettetÅrsak
import no.nav.familie.kontrakter.felles.klage.KanIkkeOppretteRevurderingÅrsak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class EksternBehandlingServiceTest : OppslagSpringRunnerTest() {

    @Autowired
    lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    lateinit var eksternBehandlingService: EksternBehandlingService

    @Autowired
    lateinit var vilkårsvurderingRepository: VilkårsvurderingRepository

    @Nested
    inner class OpprettRevurderingKlage {

        @Test
        internal fun `kan opprette revurdering hvis det finnes en ferdigstilt behandling`() {
            val fagsakMedFerdigstiltBehandling = testoppsettService.lagreFagsak(fagsak(fagsakpersoner("3")))
            val førstegangsbehandling = behandlingRepository.insert(
                behandling(
                    fagsakMedFerdigstiltBehandling,
                    resultat = BehandlingResultat.INNVILGET,
                    status = BehandlingStatus.FERDIGSTILT,
                ),
            )
            vilkårsvurderingRepository.insert(vilkårsvurdering(førstegangsbehandling.id))

            val result = testWithBrukerContext {
                eksternBehandlingService.opprettRevurderingKlage(fagsakMedFerdigstiltBehandling.eksternId.id)
            }

            assertThat(result.opprettetBehandling).isTrue

            val behandling = behandlingRepository.finnMedEksternId(result.opprettet!!.eksternBehandlingId.toLong())

            assertThat(behandling!!.årsak).isEqualTo(BehandlingÅrsak.KLAGE)
        }

        @Test
        internal fun `mangler behandling skal feile`() {
            val fagsakUtenBehandling = testoppsettService.lagreFagsak(fagsak(fagsakpersoner("1")))

            val result = eksternBehandlingService.opprettRevurderingKlage(fagsakUtenBehandling.eksternId.id)

            assertThat(result.opprettetBehandling).isFalse
            assertThat(result.ikkeOpprettet!!.årsak).isEqualTo(IkkeOpprettetÅrsak.INGEN_BEHANDLING)
        }

        @Test
        internal fun `henlagt behandling skal feile`() {
            val fagsakMedHenlagtBehandling = testoppsettService.lagreFagsak(fagsak(fagsakpersoner("4")))
            behandlingRepository.insert(
                behandling(
                    fagsakMedHenlagtBehandling,
                    resultat = BehandlingResultat.HENLAGT,
                    status = BehandlingStatus.FERDIGSTILT,
                ),
            )

            val result = eksternBehandlingService.opprettRevurderingKlage(fagsakMedHenlagtBehandling.eksternId.id)

            assertThat(result.opprettetBehandling).isFalse
            assertThat(result.ikkeOpprettet!!.årsak).isEqualTo(IkkeOpprettetÅrsak.INGEN_BEHANDLING)
        }

        @Test
        internal fun `kan ikke opprette recvurdering hvis det finnes åpen behandling`() {
            val fagsakMedÅpenBehandling = testoppsettService.lagreFagsak(fagsak(fagsakpersoner("2")))
            behandlingRepository.insert(behandling(fagsakMedÅpenBehandling))

            val result = eksternBehandlingService.opprettRevurderingKlage(fagsakMedÅpenBehandling.eksternId.id)

            assertThat(result.opprettetBehandling).isFalse
            assertThat(result.ikkeOpprettet!!.årsak).isEqualTo(IkkeOpprettetÅrsak.ÅPEN_BEHANDLING)
        }
    }

    @Nested
    inner class KanOppretteRevurdering {

        @Test
        internal fun `kan opprette revurdering hvis det finnes en ferdigstilt behandling`() {
            val fagsakMedFerdigstiltBehandling = testoppsettService.lagreFagsak(fagsak(fagsakpersoner("3")))
            val førstegangsbehandling = behandlingRepository.insert(
                behandling(
                    fagsakMedFerdigstiltBehandling,
                    resultat = BehandlingResultat.INNVILGET,
                    status = BehandlingStatus.FERDIGSTILT,
                ),
            )
            val result = eksternBehandlingService.kanOppretteRevurdering(fagsakMedFerdigstiltBehandling.eksternId.id)

            assertThat(result.kanOpprettes).isTrue
            assertThat(result.årsak).isNull()
        }

        @Test
        internal fun `kan ikke opprette recvurdering hvis det finnes åpen behandling`() {
            val fagsakMedÅpenBehandling = testoppsettService.lagreFagsak(fagsak(fagsakpersoner("2")))
            behandlingRepository.insert(behandling(fagsakMedÅpenBehandling))

            val result = eksternBehandlingService.kanOppretteRevurdering(fagsakMedÅpenBehandling.eksternId.id)

            assertThat(result.kanOpprettes).isFalse
            assertThat(result.årsak).isEqualTo(KanIkkeOppretteRevurderingÅrsak.ÅPEN_BEHANDLING)
        }

        @Test
        internal fun `kan ikke opprette revurdering hvis det ikke finnes noen behandlinger`() {
            val fagsakUtenBehandling = testoppsettService.lagreFagsak(fagsak(fagsakpersoner("1")))

            val result = eksternBehandlingService.kanOppretteRevurdering(fagsakUtenBehandling.eksternId.id)

            assertThat(result.kanOpprettes).isFalse
            assertThat(result.årsak).isEqualTo(KanIkkeOppretteRevurderingÅrsak.INGEN_BEHANDLING)
        }
    }
}
