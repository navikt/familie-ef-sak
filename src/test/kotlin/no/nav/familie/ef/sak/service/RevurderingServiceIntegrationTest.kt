package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.api.dto.Sivilstandstype
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.fagsakpersoner
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.vilkårsvurdering
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.util.BrukerContextUtil
import no.nav.familie.ef.sak.regler.HovedregelMetadata
import no.nav.familie.ef.sak.regler.vilkår.SivilstandRegel
import no.nav.familie.ef.sak.repository.BehandlingRepository
import no.nav.familie.ef.sak.repository.FagsakRepository
import no.nav.familie.ef.sak.repository.VilkårsvurderingRepository
import no.nav.familie.ef.sak.repository.domain.Behandling
import no.nav.familie.ef.sak.repository.domain.BehandlingStatus
import no.nav.familie.ef.sak.repository.domain.BehandlingType
import no.nav.familie.ef.sak.repository.domain.Fagsak
import no.nav.familie.ef.sak.repository.domain.VilkårType
import no.nav.familie.ef.sak.repository.domain.Vilkårsresultat
import no.nav.familie.ef.sak.repository.domain.søknad.SøknadsskjemaOvergangsstønad
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.kontrakter.ef.søknad.Testsøknad
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class RevurderingServiceIntegrationTest : OppslagSpringRunnerTest() {

    @Autowired lateinit var revurderingService: RevurderingService
    @Autowired lateinit var behandlingRepository: BehandlingRepository
    @Autowired lateinit var fagsakRepository: FagsakRepository
    @Autowired lateinit var vilkårsvurderingRepository: VilkårsvurderingRepository
    @Autowired lateinit var søknadService: SøknadService

    private lateinit var fagsak: Fagsak
    private val personIdent = "123456789012"


    @BeforeEach
    fun setUp() {
        BrukerContextUtil.mockBrukerContext("Heider")
        val identer = fagsakpersoner(setOf(personIdent))
        fagsak = fagsakRepository.insert(fagsak(identer = identer))
    }

    @AfterEach
    internal fun tearDown() {
        BrukerContextUtil.clearBrukerContext()
    }

    @Test
    internal fun `skal opprette revurdering`() {
        val behandling = behandlingRepository.insert(behandling(fagsak = fagsak, status = BehandlingStatus.FERDIGSTILT))
        lagreSøknad(behandling, fagsak)

        val opprettRevurderingManuelt = revurderingService.opprettRevurderingManuelt(fagsak.id)

        val revurdering = behandlingRepository.findByIdOrThrow(opprettRevurderingManuelt.id)
        assertThat(revurdering.type).isEqualTo(BehandlingType.REVURDERING)
    }

    @Test
    internal fun `revurdering - skal kopiere vilkår`() {
        val behandling = behandlingRepository.insert(behandling(fagsak = fagsak, status = BehandlingStatus.FERDIGSTILT))
        val søknad = lagreSøknad(behandling, fagsak)
        opprettVilkår(behandling, søknad)

        val revurdering = revurderingService.opprettRevurderingManuelt(fagsak.id)
        val vilkårForBehandling = vilkårsvurderingRepository.findByBehandlingId(behandling.id)[0]
        val vilkårForRevurdering = vilkårsvurderingRepository.findByBehandlingId(revurdering.id)[0]

        assertThat(vilkårForBehandling.id).isNotEqualTo(vilkårForRevurdering.id)
        assertThat(vilkårForBehandling.behandlingId).isNotEqualTo(vilkårForRevurdering.behandlingId)
        assertThat(vilkårForBehandling.sporbar.opprettetTid).isNotEqualTo(vilkårForRevurdering.sporbar.opprettetTid)

        assertThat(vilkårForBehandling.resultat).isEqualTo(vilkårForRevurdering.resultat)
        assertThat(vilkårForBehandling.type).isEqualTo(vilkårForRevurdering.type)
        assertThat(vilkårForBehandling.barnId).isEqualTo(vilkårForRevurdering.barnId)
        assertThat(vilkårForBehandling.delvilkårsvurdering).isEqualTo(vilkårForRevurdering.delvilkårsvurdering)
    }

    @Test
    internal fun `skal ikke være mulig å opprette fagsak hvis siste behandling ikke er ferdig`() {
        behandlingRepository.insert(behandling(fagsak = fagsak, status = BehandlingStatus.UTREDES))

        assertThat(catchThrowable { revurderingService.opprettRevurderingManuelt(fagsak.id) })
                .hasMessageContaining("Revurdering må ha eksisterende iverksatt behandling")
    }

    @Test
    internal fun `skal ikke være mulig å opprette fagsak hvis det ikke finnes en behandling fra før`() {
        assertThat(catchThrowable { revurderingService.opprettRevurderingManuelt(fagsak.id) })
                .hasMessageContaining("Revurdering må ha eksisterende iverksatt behandling")
    }

    private fun lagreSøknad(behandling: Behandling,
                            fagsak: Fagsak): SøknadsskjemaOvergangsstønad {
        søknadService.lagreSøknadForOvergangsstønad(Testsøknad.søknadOvergangsstønad, behandling.id, fagsak.id, "1L")
        return søknadService.hentOvergangsstønad(behandling.id)
    }

    private fun opprettVilkår(behandling: Behandling,
                              søknad: SøknadsskjemaOvergangsstønad) {
        val barnId = søknad.barn.first().id
        val delvilkårsvurdering = SivilstandRegel().initereDelvilkårsvurdering(HovedregelMetadata(søknad,
                                                                                                  Sivilstandstype.ENKE_ELLER_ENKEMANN))
        vilkårsvurderingRepository.insert(vilkårsvurdering(resultat = Vilkårsresultat.OPPFYLT,
                                                           type = VilkårType.SIVILSTAND,
                                                           behandlingId = behandling.id,
                                                           barnId = barnId,
                                                           delvilkårsvurdering = delvilkårsvurdering))
    }
}