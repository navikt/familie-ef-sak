package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.RevurderingService
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.behandling.dto.RevurderingDto
import no.nav.familie.ef.sak.fagsak.FagsakRepository
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.felles.util.BrukerContextUtil
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.Sivilstandstype
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadService
import no.nav.familie.ef.sak.opplysninger.søknad.domain.SøknadsskjemaOvergangsstønad
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.fagsakpersoner
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.ef.sak.repository.vilkårsvurdering
import no.nav.familie.ef.sak.vilkår.VilkårType
import no.nav.familie.ef.sak.vilkår.Vilkårsresultat
import no.nav.familie.ef.sak.vilkår.VilkårsvurderingRepository
import no.nav.familie.ef.sak.vilkår.regler.HovedregelMetadata
import no.nav.familie.ef.sak.vilkår.regler.vilkår.SivilstandRegel
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.ef.søknad.Testsøknad
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

internal class RevurderingServiceIntegrationTest : OppslagSpringRunnerTest() {

    @Autowired lateinit var revurderingService: RevurderingService
    @Autowired lateinit var behandlingRepository: BehandlingRepository
    @Autowired lateinit var fagsakRepository: FagsakRepository
    @Autowired lateinit var vilkårsvurderingRepository: VilkårsvurderingRepository
    @Autowired lateinit var søknadService: SøknadService

    private lateinit var fagsak: Fagsak
    private val personIdent = "123456789012"
    private val behandlingsårsak = BehandlingÅrsak.NYE_OPPLYSNINGER
    private val kravMottatt = LocalDate.of(2021, 9, 9)
    private val revurderingDto = RevurderingDto(fagsak.id, behandlingsårsak, kravMottatt)


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
        val behandling = behandlingRepository.insert(behandling(fagsak = fagsak,
                                                                status = BehandlingStatus.FERDIGSTILT,
                                                                resultat = BehandlingResultat.INNVILGET))
        val søknad = lagreSøknad(behandling, fagsak)
        opprettVilkår(behandling, søknad)

        val opprettRevurderingManuelt = revurderingService.opprettRevurderingManuelt(revurderingDto)

        val revurdering = behandlingRepository.findByIdOrThrow(opprettRevurderingManuelt.id)
        assertThat(revurdering.type).isEqualTo(BehandlingType.REVURDERING)
    }

    @Test
    internal fun `revurdering - skal kopiere vilkår`() {
        val behandling = behandlingRepository.insert(behandling(fagsak = fagsak,
                                                                status = BehandlingStatus.FERDIGSTILT,
                                                                resultat = BehandlingResultat.INNVILGET))
        val søknad = lagreSøknad(behandling, fagsak)
        opprettVilkår(behandling, søknad)

        val revurdering = revurderingService.opprettRevurderingManuelt(revurderingDto)
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

        assertThat(catchThrowable { revurderingService.opprettRevurderingManuelt(revurderingDto) })
                .hasMessageContaining("Det finnes en behandling på fagsaken som ikke er ferdigstilt")
    }

    @Test
    internal fun `skal ikke være mulig å opprette fagsak hvis det ikke finnes en behandling fra før`() {
        assertThat(catchThrowable { revurderingService.opprettRevurderingManuelt(revurderingDto) })
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