package no.nav.familie.ef.sak.behandling

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.barn.BarnRepository
import no.nav.familie.ef.sak.barn.BehandlingBarn
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.felles.util.BrukerContextUtil.testWithBrukerContext
import no.nav.familie.ef.sak.infrastruktur.config.RolleConfig
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataService
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.opprettAlleVilkårsvurderinger
import no.nav.familie.ef.sak.vilkår.VilkårType
import no.nav.familie.ef.sak.vilkår.Vilkårsresultat
import no.nav.familie.ef.sak.vilkår.Vilkårsvurdering
import no.nav.familie.ef.sak.vilkår.VilkårsvurderingRepository
import no.nav.familie.ef.sak.vilkår.VurderingService
import no.nav.familie.ef.sak.vilkår.VurderingStegService
import no.nav.familie.ef.sak.vilkår.dto.DelvilkårsvurderingDto
import no.nav.familie.ef.sak.vilkår.dto.SvarPåVurderingerDto
import no.nav.familie.ef.sak.vilkår.dto.VilkårsvurderingDto
import no.nav.familie.ef.sak.vilkår.dto.VurderingDto
import no.nav.familie.ef.sak.vilkår.dto.tilDto
import no.nav.familie.ef.sak.vilkår.gjenbruk.GjenbrukVilkårService
import no.nav.familie.ef.sak.vilkår.regler.RegelId
import no.nav.familie.ef.sak.vilkår.regler.SvarId
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.util.UUID

internal class TestSaksbehandlingControllerTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var vurderingService: VurderingService

    @Autowired
    private lateinit var vurderingStegService: VurderingStegService

    @Autowired
    private lateinit var gjenbrukVilkårService: GjenbrukVilkårService

    @Autowired
    private lateinit var barnRepository: BarnRepository

    @Autowired
    private lateinit var vilkårsvurderingRepository: VilkårsvurderingRepository

    @Autowired
    private lateinit var testSaksbehandlingController: TestSaksbehandlingController

    @Autowired
    private lateinit var grunnlagsdataService: GrunnlagsdataService

    @Autowired
    private lateinit var rolleConfig: RolleConfig

    @ParameterizedTest
    @EnumSource(StønadType::class)
    internal fun `skal fylle ut vilkår automatisk`(stønadType: StønadType) {
        val fagsak = testoppsettService.lagreFagsak(fagsak(stønadstype = stønadType))
        val behandling = behandlingRepository.insert(behandling(fagsak))

        grunnlagsdataService.opprettGrunnlagsdata(behandling.id)

        testWithBrukerContext(preferredUsername = "Z999999", groups = listOf(rolleConfig.saksbehandlerRolle)) {
            vurderingService.hentEllerOpprettVurderinger(behandling.id)
            testSaksbehandlingController.utfyllVilkår(behandling.id)
        }

        val oppdaterteVurderinger = vurderingService.hentAlleVurderinger(behandling.id)
        val regelIdsForOppdaterteVurderinger = tilRegelIds(oppdaterteVurderinger)

        // Skal hverken ha opprettet eller vurdert delvilkår som er historiske
        assertThat(regelIdsForOppdaterteVurderinger).doesNotContain(RegelId.SKRIFTLIG_AVTALE_OM_DELT_BOSTED)
        assertThat(oppdaterteVurderinger.map { it.resultat }.distinct()).containsExactly(Vilkårsresultat.OPPFYLT)
    }

    @ParameterizedTest
    @EnumSource(StønadType::class)
    internal fun `skal kunne endre delvilkårsvurderinger på vilkår som inneholder historiske delvilkår`(stønadType: StønadType) {
        val fagsak = testoppsettService.lagreFagsak(fagsak(stønadstype = stønadType))
        val behandling = behandlingRepository.insert(behandling(fagsak))
        grunnlagsdataService.opprettGrunnlagsdata(behandling.id)
        val opprettedeVurderinger = vilkårsvurderingRepository.insertAll(opprettAlleVurderinger(behandling.id, stønadType))

        // Skal ha opprettet delvilkår som er historiske
        assertThat(tilRegelIds(opprettedeVurderinger.map { it.tilDto() })).contains(RegelId.SKRIFTLIG_AVTALE_OM_DELT_BOSTED)

        val aleneomsorgVilkår = opprettedeVurderinger.first { it.type == VilkårType.ALENEOMSORG }
        val nyeDelvilkårsvurderinger =
            listOf(
                DelvilkårsvurderingDto(
                    resultat = Vilkårsresultat.OPPFYLT,
                    vurderinger =
                        listOf(
                            VurderingDto(
                                RegelId.NÆRE_BOFORHOLD,
                                SvarId.NEI,
                                "Begrunnelse",
                            ),
                        ),
                ),
                DelvilkårsvurderingDto(
                    resultat = Vilkårsresultat.OPPFYLT,
                    vurderinger =
                        listOf(
                            VurderingDto(
                                RegelId.MER_AV_DAGLIG_OMSORG,
                                SvarId.JA,
                                "Begrunnelse",
                            ),
                        ),
                ),
            )

        val vilkårsVurderingDto = SvarPåVurderingerDto(aleneomsorgVilkår.id, behandling.id, nyeDelvilkårsvurderinger)

        testWithBrukerContext(preferredUsername = "Z999999", groups = listOf(rolleConfig.saksbehandlerRolle)) {
            vurderingStegService.oppdaterVilkår(vilkårsVurderingDto)
        }

        val endredeVurderinger = vurderingService.hentAlleVurderinger(behandling.id)

        // Skal ha fjernet delvilkår som er historiske
        assertThat(tilRegelIds(endredeVurderinger)).doesNotContain(RegelId.SKRIFTLIG_AVTALE_OM_DELT_BOSTED)
        assertThat(endredeVurderinger.first { it.id == aleneomsorgVilkår.id }.resultat).isEqualTo(Vilkårsresultat.OPPFYLT)
    }

    @ParameterizedTest
    @EnumSource(StønadType::class)
    internal fun `skal gjenbruke vilkårsvurderinger fra tidligere behandling uten å ta med historiske delvilkår`(stønadType: StønadType) {
        val fagsak = testoppsettService.lagreFagsak(fagsak(stønadstype = stønadType))

        // Opprett behandlinger
        val førstegangsbehandling =
            behandlingRepository.insert(
                behandling(
                    fagsak = fagsak,
                    resultat = BehandlingResultat.INNVILGET,
                    status = BehandlingStatus.FERDIGSTILT,
                    årsak = BehandlingÅrsak.SØKNAD,
                ),
            )
        val revurdering =
            behandlingRepository.insert(
                behandling(
                    fagsak = fagsak,
                    resultat = BehandlingResultat.IKKE_SATT,
                    status = BehandlingStatus.UTREDES,
                    årsak = BehandlingÅrsak.SØKNAD,
                ),
            )

        // Opprett behandlingsbarn
        val fødselTerminDato = LocalDate.now().minusYears(2)
        barnRepository.insert(
            BehandlingBarn(
                behandlingId = førstegangsbehandling.id,
                personIdent = "18411577259",
                fødselTermindato = fødselTerminDato,
            ),
        )
        barnRepository.insert(
            BehandlingBarn(
                behandlingId = revurdering.id,
                personIdent = "18411577259",
                fødselTermindato = fødselTerminDato,
            ),
        )

        // Opprett grunnlagsdata
        grunnlagsdataService.opprettGrunnlagsdata(førstegangsbehandling.id)
        grunnlagsdataService.opprettGrunnlagsdata(revurdering.id)

        // Opprett alle vurderinger (med historiske vilkår) for førstegangsbehandling
        val vurderingerForFørstegangsbehandling = vilkårsvurderingRepository.insertAll(opprettAlleVurderinger(førstegangsbehandling.id, stønadType))

        // Opprett vurderinger (uten historiske vilkår) for revurdering
        testWithBrukerContext(preferredUsername = "Z999999", groups = listOf(rolleConfig.saksbehandlerRolle)) {
            // Initier vilkår for revurdering
            vurderingService.hentEllerOpprettVurderinger(revurdering.id)
        }

        val initielleVurderingerForRevurdering = vurderingService.hentAlleVurderinger(revurdering.id)

        // Skal ha opprettet delvilkår som er historiske for førstegangsbehandling
        assertThat(tilRegelIds(vurderingerForFørstegangsbehandling.map { it.tilDto() })).contains(RegelId.SKRIFTLIG_AVTALE_OM_DELT_BOSTED)
        // Skal ikke ha opprettet delvilkår som er historiske for revurdering
        assertThat(initielleVurderingerForRevurdering.flatMap { vilkårsvurdering -> vilkårsvurdering.delvilkårsvurderinger.flatMap { deilvilkårsvurdering -> deilvilkårsvurdering.vurderinger.map { it.regelId } } }).doesNotContain(RegelId.SKRIFTLIG_AVTALE_OM_DELT_BOSTED)

        testWithBrukerContext(preferredUsername = "Z999999", groups = listOf(rolleConfig.saksbehandlerRolle)) {
            // Kopier over vilkår fra førstegangsbehandling til revurdering
            gjenbrukVilkårService.gjenbrukInngangsvilkårVurderinger(revurdering.id, førstegangsbehandling.id)
        }

        // Hent vurderinger for revurdering etter gjenbruk
        val endredeVurderingerForRevurdering = vurderingService.hentAlleVurderinger(revurdering.id)

        // Skal ikke ha gjenbrukt delvilkår som er historiske
        assertThat(tilRegelIds(endredeVurderingerForRevurdering)).doesNotContain(RegelId.SKRIFTLIG_AVTALE_OM_DELT_BOSTED)
    }

    private fun opprettAlleVurderinger(
        behandlingId: UUID,
        stønadType: StønadType,
    ): List<Vilkårsvurdering> {
        val (_, metadata) = vurderingService.hentGrunnlagOgMetadata(behandlingId)
        return opprettAlleVilkårsvurderinger(behandlingId, metadata, stønadType)
    }

    private fun tilRegelIds(vilkårsvurderinger: List<VilkårsvurderingDto>): List<RegelId> =
        vilkårsvurderinger.flatMap { vilkårsvurdering ->
            vilkårsvurdering.delvilkårsvurderinger.flatMap { delvilkårsvurdering ->
                delvilkårsvurdering.vurderinger.map { it.regelId }.distinct()
            }
        }
}
