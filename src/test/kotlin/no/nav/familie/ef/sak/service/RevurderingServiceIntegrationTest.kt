package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.RevurderingService
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.fagsak.Fagsak
import no.nav.familie.ef.sak.fagsak.FagsakRepository
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.fagsakpersoner
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.vilkårsvurdering
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.util.BrukerContextUtil
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.Sivilstandstype
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadService
import no.nav.familie.ef.sak.opplysninger.søknad.domain.søknad.SøknadsskjemaOvergangsstønad
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.ef.sak.vilkår.VilkårType
import no.nav.familie.ef.sak.vilkår.Vilkårsresultat
import no.nav.familie.ef.sak.vilkår.VilkårsvurderingRepository
import no.nav.familie.ef.sak.vilkår.regler.HovedregelMetadata
import no.nav.familie.ef.sak.vilkår.regler.vilkår.SivilstandRegel
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
        val søknad = lagreSøknad(behandling, fagsak)
        opprettVilkår(behandling, søknad)

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

        assertThat(vilkårForBehandling).usingRecursiveComparison().ignoringFields("id", "sporbar", "behandlingId")
                .isEqualTo(vilkårForRevurdering)
    }

    @Test
    internal fun `skal ikke være mulig å opprette fagsak hvis siste behandling ikke er ferdig`() {
        behandlingRepository.insert(behandling(fagsak = fagsak, status = BehandlingStatus.UTREDES))

        assertThat(catchThrowable { revurderingService.opprettRevurderingManuelt(fagsak.id) })
                .hasMessageContaining("Det finnes en behandling på fagsaken som ikke er ferdigstilt")
    }

    @Test
    internal fun `skal ikke være mulig å opprette fagsak hvis det ikke finnes en behandling fra før`() {
        assertThat(catchThrowable { revurderingService.opprettRevurderingManuelt(fagsak.id) })
                .hasMessageContaining("Det finnes ikke en tidligere behandling på fagsaken")
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