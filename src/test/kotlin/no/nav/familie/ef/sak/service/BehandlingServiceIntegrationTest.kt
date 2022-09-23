package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
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
                status = BehandlingStatus.UTREDES
            )
        )
        assertThatThrownBy {
            behandlingService.opprettBehandling(
                BehandlingType.REVURDERING,
                fagsak.id,
                behandlingsårsak = behandlingÅrsak
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
                behandlingsårsak = behandlingÅrsak
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

    @Nested
    inner class finnSisteBehandling {

        var datoCounter = 0L

        @Nested
        inner class finnSisteIverksatteBehandling {

            @ParameterizedTest
            @EnumSource(BehandlingResultat::class, names = ["INNVILGET", "OPPHØRT"])
            internal fun `skal finne siste behandlingen av ferdigstilt`(resultat: BehandlingResultat) {
                val fagsak = testoppsettService.lagreFagsak(fagsak())
                leggInnBehandlingerForAlleBehandlingsresultat(fagsak)
                val behandling = lagreBehandling(fagsak, resultat)

                val sisteBehandling = behandlingService.finnSisteIverksatteEllerAvslåtteBehandling(fagsak.id)

                assertThat(sisteBehandling?.id).isEqualTo(behandling.id)
            }

            @ParameterizedTest
            @EnumSource(BehandlingResultat::class, names = ["IKKE_SATT", "HENLAGT", "AVSLÅTT"])
            internal fun `skal ikke finne behandling med ikke_satt, avslått eller henlagt som resultat`(resultat: BehandlingResultat) {
                val fagsak = testoppsettService.lagreFagsak(fagsak())
                lagreBehandling(fagsak, resultat)

                val sisteBehandling = behandlingService.finnSisteIverksatteBehandling(fagsak.id)

                assertThat(sisteBehandling).isNull()
            }
        }

        @Nested
        inner class finnSisteIverksatteEllerAvslåtteBehandling {
            @ParameterizedTest
            @EnumSource(BehandlingResultat::class, names = ["INNVILGET", "OPPHØRT", "AVSLÅTT"])
            internal fun `skal finne siste behandlingen av ferdigstilt, opphørt, eller avslått`(resultat: BehandlingResultat) {
                val fagsak = testoppsettService.lagreFagsak(fagsak())
                leggInnBehandlingerForAlleBehandlingsresultat(fagsak)
                val behandling = lagreBehandling(fagsak, resultat)

                val sisteBehandling = behandlingService.finnSisteIverksatteEllerAvslåtteBehandling(fagsak.id)

                assertThat(sisteBehandling?.id).isEqualTo(behandling.id)
            }

            @ParameterizedTest
            @EnumSource(BehandlingResultat::class, names = ["IKKE_SATT", "HENLAGT"])
            internal fun `skal ikke finne behandling med ikke_satt eller henlagt som resultat`(resultat: BehandlingResultat) {
                val fagsak = testoppsettService.lagreFagsak(fagsak())
                lagreBehandling(fagsak, resultat)

                val sisteBehandling = behandlingService.finnSisteIverksatteEllerAvslåtteBehandling(fagsak.id)

                assertThat(sisteBehandling).isNull()
            }
        }

        private fun leggInnBehandlingerForAlleBehandlingsresultat(fagsak: Fagsak) {
            BehandlingResultat.values().forEach { lagreBehandling(fagsak, it) }
        }

        /**
         * For å få behandlinger i riktig rekkefølge brukes [datoCounter] som legger til en minut på hver behandling som opprettes
         */
        private fun lagreBehandling(
            fagsak: Fagsak,
            resultat: BehandlingResultat
        ): Behandling {
            val behandling = behandling(fagsak)
            val opprettetTid = behandling.sporbar.opprettetTid.plusMinutes(datoCounter++)
            return behandlingRepository.insert(
                behandling.copy(
                    resultat = resultat,
                    status = BehandlingStatus.FERDIGSTILT,
                    sporbar = behandling.sporbar.copy(opprettetTid = opprettetTid)
                )
            )
        }
    }

    @Test
    internal fun `hentBehandlingForGjenbrukAvVilkår - skal returnere en sortert liste av aktuelle behandlinger for vilkårsgjenbruk`() {
        val fagsakPersonId = UUID.randomUUID()
        val fagsakOs = testoppsettService.lagreFagsak(
            fagsak(
                stønadstype = StønadType.OVERGANGSSTØNAD,
                fagsakPersonId = fagsakPersonId
            )
        )
        val fagsakBt = testoppsettService.lagreFagsak(
            fagsak(
                stønadstype = StønadType.BARNETILSYN,
                fagsakPersonId = fagsakPersonId
            )
        )
        val fagsakSp = testoppsettService.lagreFagsak(
            fagsak(
                stønadstype = StønadType.SKOLEPENGER,
                fagsakPersonId = fagsakPersonId
            )
        )

        behandlingRepository.insert(
            behandling(fagsakOs).copy(
                resultat = BehandlingResultat.HENLAGT,
                status = BehandlingStatus.FERDIGSTILT
            )
        )
        val førstegangBt = behandlingRepository.insert(
            behandling(fagsakBt).copy(
                resultat = BehandlingResultat.INNVILGET,
                status = BehandlingStatus.FERDIGSTILT
            )
        )
        val førstegangSp = behandlingRepository.insert(
            behandling(fagsakSp).copy(
                resultat = BehandlingResultat.INNVILGET,
                status = BehandlingStatus.FERDIGSTILT
            )
        )
        val revurderingUnderArbeidSP = behandlingRepository.insert(
            behandling(fagsakSp).copy(
                resultat = BehandlingResultat.IKKE_SATT,
                status = BehandlingStatus.UTREDES
            )
        )

        val behandlingerForVilkårsgjenbrukHentet = behandlingService.hentBehandlingerForGjenbrukAvVilkår(fagsakPersonId)
        val behandlingerForVilkårsgjenbrukkLagret = listOf(revurderingUnderArbeidSP, førstegangSp, førstegangBt)
        assertThat(behandlingerForVilkårsgjenbrukHentet).isEqualTo(behandlingerForVilkårsgjenbrukkLagret)
    }
}
