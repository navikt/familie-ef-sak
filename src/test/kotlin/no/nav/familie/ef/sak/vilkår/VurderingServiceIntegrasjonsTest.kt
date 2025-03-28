package no.nav.familie.ef.sak.vilkår

import io.mockk.mockk
import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.barn.BarnRepository
import no.nav.familie.ef.sak.barn.BehandlingBarn
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.Sivilstandstype
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadService
import no.nav.familie.ef.sak.opplysninger.søknad.domain.SøknadsskjemaOvergangsstønad
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.vilkårsvurdering
import no.nav.familie.ef.sak.testutil.søknadBarnTilBehandlingBarn
import no.nav.familie.ef.sak.vilkår.regler.HovedregelMetadata
import no.nav.familie.ef.sak.vilkår.regler.vilkår.SivilstandRegel
import no.nav.familie.kontrakter.ef.søknad.Testsøknad
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

internal class VurderingServiceIntegrasjonsTest : OppslagSpringRunnerTest() {
    @Autowired
    lateinit var vilkårsvurderingRepository: VilkårsvurderingRepository

    @Autowired
    lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    lateinit var vurderingService: VurderingService

    @Autowired
    lateinit var søknadService: SøknadService

    @Autowired
    lateinit var barnRepository: BarnRepository

    @Test
    internal fun `kopierVurderingerTilNyBehandling - skal kopiere vurderinger til ny behandling`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak, status = BehandlingStatus.FERDIGSTILT))
        val revurdering = behandlingRepository.insert(behandling(fagsak))
        val søknadskjema = lagreSøknad(behandling, fagsak)
        val barnPåFørsteSøknad = barnRepository.insertAll(søknadBarnTilBehandlingBarn(søknadskjema.barn, behandling.id))
        val barnPåRevurdering = barnRepository.insertAll(søknadBarnTilBehandlingBarn(søknadskjema.barn, revurdering.id))

        val vilkårForBehandling = opprettVilkårsvurderinger(søknadskjema, behandling, barnPåFørsteSøknad).first()
        val metadata =
            HovedregelMetadata(
                søknadskjema.sivilstand,
                Sivilstandstype.SKILT,
                false,
                barnPåRevurdering,
                emptyList(),
                listOf(),
                mockk(),
                mockk(),
            )
        vurderingService.kopierVurderingerOgSamværsavtalerTilNyBehandling(revurdering.id, behandling.id, metadata, StønadType.OVERGANGSSTØNAD)

        val vilkårForRevurdering = vilkårsvurderingRepository.findByBehandlingId(revurdering.id).first()

        assertThat(vilkårForBehandling.id).isNotEqualTo(vilkårForRevurdering.id)
        assertThat(vilkårForBehandling.behandlingId).isNotEqualTo(vilkårForRevurdering.behandlingId)
        assertThat(vilkårForBehandling.sporbar.opprettetTid).isNotEqualTo(vilkårForRevurdering.sporbar.opprettetTid)
        assertThat(vilkårForBehandling.sporbar.endret).isNotEqualTo(vilkårForRevurdering.sporbar.endret)
        assertThat(vilkårForBehandling.barnId).isNotEqualTo(vilkårForRevurdering.barnId)
        assertThat(vilkårForBehandling.barnId).isEqualTo(barnPåFørsteSøknad.first().id)
        assertThat(vilkårForBehandling.opphavsvilkår).isNull()
        assertThat(vilkårForRevurdering.barnId).isEqualTo(barnPåRevurdering.first().id)
        assertThat(vilkårForRevurdering.opphavsvilkår)
            .isEqualTo(Opphavsvilkår(behandling.id, vilkårForBehandling.sporbar.endret.endretTid))

        assertThat(vilkårForBehandling)
            .usingRecursiveComparison()
            .ignoringFields("id", "sporbar", "behandlingId", "barnId", "opphavsvilkår")
            .isEqualTo(vilkårForRevurdering)
    }

    @Test
    internal fun `oppdaterGrunnlagsdataOgHentEllerOpprettVurderinger - skal kaste feil dersom behandlingen er låst for videre behandling`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak, status = BehandlingStatus.FERDIGSTILT))
        assertThat(catchThrowable { vurderingService.oppdaterGrunnlagsdataOgHentEllerOpprettVurderinger(behandling.id) })
            .hasMessage("Kan ikke laste inn nye grunnlagsdata for behandling med status ${behandling.status}")
    }

    @Test
    internal fun `kopierVurderingerTilNyBehandling - skal kaste feil hvis det ikke finnes noen vurderinger`() {
        val tidligereBehandlingId = UUID.randomUUID()
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val revurdering = behandlingRepository.insert(behandling(fagsak))
        val metadata =
            HovedregelMetadata(
                null,
                Sivilstandstype.SKILT,
                false,
                emptyList(),
                emptyList(),
                emptyList(),
                mockk(),
                mockk(),
            )
        assertThat(
            catchThrowable {
                vurderingService.kopierVurderingerOgSamværsavtalerTilNyBehandling(
                    revurdering.id,
                    tidligereBehandlingId,
                    metadata,
                    StønadType.OVERGANGSSTØNAD,
                )
            },
        ).hasMessage("Tidligere behandling=$tidligereBehandlingId har ikke noen vilkår")
    }

    private fun opprettVilkårsvurderinger(
        søknadskjema: SøknadsskjemaOvergangsstønad,
        behandling: Behandling,
        barn: List<BehandlingBarn>,
    ): List<Vilkårsvurdering> {
        val hovedregelMetadata =
            HovedregelMetadata(
                søknadskjema.sivilstand,
                Sivilstandstype.ENKE_ELLER_ENKEMANN,
                barn = barn,
                søktOmBarnetilsyn = emptyList(),
                vilkårgrunnlagDto = mockk(),
                behandling = behandling,
            )
        val delvilkårsvurdering = SivilstandRegel().initiereDelvilkårsvurdering(hovedregelMetadata)
        val vilkårsvurderinger =
            listOf(
                vilkårsvurdering(
                    resultat = Vilkårsresultat.OPPFYLT,
                    type = VilkårType.SIVILSTAND,
                    behandlingId = behandling.id,
                    barnId = barn.first().id,
                    delvilkårsvurdering = delvilkårsvurdering,
                ),
            )
        return vilkårsvurderingRepository.insertAll(vilkårsvurderinger)
    }

    private fun lagreSøknad(
        behandling: Behandling,
        fagsak: Fagsak,
    ): SøknadsskjemaOvergangsstønad {
        søknadService.lagreSøknadForOvergangsstønad(Testsøknad.søknadOvergangsstønad, behandling.id, fagsak.id, "1L")
        return søknadService.hentOvergangsstønad(behandling.id)!!
    }
}
