package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

internal class BehandlingServiceIntegrationTest : OppslagSpringRunnerTest() {
    @Autowired
    lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    lateinit var behandlingService: BehandlingService
    private val behandlingÅrsak = BehandlingÅrsak.SØKNAD

    @Test
    internal fun `opprettBehandling skal ikke være mulig å opprette en revurdering om forrige behandling ikke er ferdigstilt`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        behandlingRepository.insert(
            behandling(
                fagsak = fagsak,
                status = BehandlingStatus.UTREDES,
            ),
        )
        assertThatThrownBy {
            behandlingService.opprettBehandling(
                BehandlingType.REVURDERING,
                fagsak.id,
                behandlingsårsak = behandlingÅrsak,
            )
        }.hasMessage("Det finnes en behandling på fagsaken som ikke er ferdigstilt")
    }

    @Test
    internal fun `opprettBehandling - skal ikke være mulig å opprette en revurdering om det ikke finnes en behandling fra før`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        assertThatThrownBy {
            behandlingService.opprettBehandling(
                BehandlingType.REVURDERING,
                fagsak.id,
                behandlingsårsak = behandlingÅrsak,
            )
        }.hasMessage("Det finnes ikke en tidligere behandling på fagsaken")
    }

    @Test
    internal fun `hentBehandlinger - skal kaste feil hvis behandling ikke finnes`() {
        assertThatThrownBy { behandlingService.hentBehandlinger(setOf(UUID.randomUUID())) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Finner ikke Behandling for")
    }

    @Test
    internal fun `hentBehandlinger - skal returnere behandlinger`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak, status = BehandlingStatus.FERDIGSTILT))
        val behandling2 = behandlingRepository.insert(behandling(fagsak))

        assertThat(behandlingService.hentBehandlinger(setOf(behandling.id, behandling2.id))).hasSize(2)
    }

    @Test
    internal fun `skal finne siste behandling med avslåtte hvis kun avslått`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling =
            behandlingRepository.insert(
                behandling(fagsak, resultat = BehandlingResultat.AVSLÅTT, status = BehandlingStatus.FERDIGSTILT),
            )
        val sisteBehandling = behandlingService.finnSisteIverksatteBehandlingMedEventuellAvslått(fagsak.id)
        assertThat(sisteBehandling?.id).isEqualTo(behandling.id)
    }

    @Test
    internal fun `skal finne siste behandling med avslåtte hvis avslått og henlagt`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val avslag =
            behandlingRepository.insert(
                behandling(fagsak, resultat = BehandlingResultat.AVSLÅTT, status = BehandlingStatus.FERDIGSTILT),
            )
        behandlingRepository.insert(
            behandling(fagsak, resultat = BehandlingResultat.HENLAGT, status = BehandlingStatus.FERDIGSTILT),
        )
        val sisteBehandling = behandlingService.finnSisteIverksatteBehandlingMedEventuellAvslått(fagsak.id)
        assertThat(sisteBehandling?.id).isEqualTo(avslag.id)
    }

    @Test
    internal fun `skal plukke ut førstegangsbehandling hvis det finnes førstegangsbehandling, avslått og henlagt`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val førstegang =
            behandlingRepository.insert(
                behandling(fagsak, resultat = BehandlingResultat.INNVILGET, status = BehandlingStatus.FERDIGSTILT),
            )
        behandlingRepository.insert(
            behandling(fagsak, resultat = BehandlingResultat.AVSLÅTT, status = BehandlingStatus.FERDIGSTILT),
        )
        behandlingRepository.insert(
            behandling(fagsak, resultat = BehandlingResultat.HENLAGT, status = BehandlingStatus.FERDIGSTILT),
        )
        val sisteBehandling = behandlingService.finnSisteIverksatteBehandlingMedEventuellAvslått(fagsak.id)
        assertThat(sisteBehandling?.id).isEqualTo(førstegang.id)
    }

    @Test
    internal fun `hentBehandlingForGjenbrukAvVilkår - skal returnere en sortert liste av aktuelle behandlinger for vilkårsgjenbruk`() {
        val fagsakPersonId = UUID.randomUUID()
        val fagsakOs =
            testoppsettService.lagreFagsak(
                fagsak(
                    stønadstype = StønadType.OVERGANGSSTØNAD,
                    fagsakPersonId = fagsakPersonId,
                ),
            )
        val fagsakBt =
            testoppsettService.lagreFagsak(
                fagsak(
                    stønadstype = StønadType.BARNETILSYN,
                    fagsakPersonId = fagsakPersonId,
                ),
            )
        val fagsakSp =
            testoppsettService.lagreFagsak(
                fagsak(
                    stønadstype = StønadType.SKOLEPENGER,
                    fagsakPersonId = fagsakPersonId,
                ),
            )

        behandlingRepository.insert(
            behandling(fagsakOs, resultat = BehandlingResultat.HENLAGT, status = BehandlingStatus.FERDIGSTILT),
        )
        val førstegangBt =
            behandlingRepository.insert(
                behandling(fagsakBt, resultat = BehandlingResultat.INNVILGET, status = BehandlingStatus.FERDIGSTILT),
            )
        val førstegangSp =
            behandlingRepository.insert(
                behandling(fagsakSp, resultat = BehandlingResultat.INNVILGET, status = BehandlingStatus.FERDIGSTILT),
            )
        val revurderingUnderArbeidSP =
            behandlingRepository.insert(
                behandling(fagsakSp, resultat = BehandlingResultat.IKKE_SATT, status = BehandlingStatus.UTREDES),
            )

        val behandlingerForVilkårsgjenbrukHentet = behandlingService.hentBehandlingerForGjenbrukAvVilkårOgSamværsavtaler(fagsakPersonId)
        val behandlingerForVilkårsgjenbrukkLagret = listOf(revurderingUnderArbeidSP, førstegangSp, førstegangBt)
        assertThat(behandlingerForVilkårsgjenbrukHentet).isEqualTo(behandlingerForVilkårsgjenbrukkLagret)
    }

    @Nested
    inner class BehandlingPåVent {
        @Test
        internal fun `opprettBehandling av førstegangsbehandling er ikke mulig hvis det finnes en førstegangsbehandling på vent`() {
            val fagsak = testoppsettService.lagreFagsak(fagsak())
            behandlingRepository.insert(behandling(fagsak, BehandlingStatus.SATT_PÅ_VENT))
            assertThatThrownBy {
                behandlingService.opprettBehandling(
                    BehandlingType.FØRSTEGANGSBEHANDLING,
                    fagsak.id,
                    behandlingsårsak = behandlingÅrsak,
                )
            }.hasMessage("Kan ikke opprette ny behandling når det finnes en førstegangsbehandling på vent")
        }

        @Test
        internal fun `opprettBehandling av revurdering er ikke mulig hvis det finnes en førstegangsbehandling på vent`() {
            val fagsak = testoppsettService.lagreFagsak(fagsak())
            behandlingRepository.insert(behandling(fagsak, BehandlingStatus.SATT_PÅ_VENT))
            assertThatThrownBy {
                behandlingService.opprettBehandling(
                    BehandlingType.REVURDERING,
                    fagsak.id,
                    behandlingsårsak = behandlingÅrsak,
                )
            }.hasMessage("Kan ikke opprette ny behandling når det finnes en førstegangsbehandling på vent")
        }

        @Test
        internal fun `opprettBehandling er mulig hvis det finnes en revurdering på vent`() {
            val fagsak = testoppsettService.lagreFagsak(fagsak())
            behandlingRepository.insert(behandling(fagsak, BehandlingStatus.FERDIGSTILT))
            behandlingRepository.insert(
                behandling(fagsak, BehandlingStatus.SATT_PÅ_VENT, type = BehandlingType.REVURDERING),
            )
            behandlingService.opprettBehandling(
                BehandlingType.REVURDERING,
                fagsak.id,
                behandlingsårsak = behandlingÅrsak,
            )
        }
    }
}
