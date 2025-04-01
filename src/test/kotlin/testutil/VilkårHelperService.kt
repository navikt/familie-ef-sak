package no.nav.familie.ef.sak.testutil

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.barn.BarnRepository
import no.nav.familie.ef.sak.barn.BehandlingBarn
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.fagsak.FagsakRepository
import no.nav.familie.ef.sak.infrastruktur.config.PdlClientConfig
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.vilkår.VilkårTestUtil
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.Sivilstandstype
import no.nav.familie.ef.sak.opplysninger.søknad.domain.Sivilstand
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.ef.sak.repository.vilkårsvurdering
import no.nav.familie.ef.sak.vilkår.Delvilkårsvurdering
import no.nav.familie.ef.sak.vilkår.VilkårType
import no.nav.familie.ef.sak.vilkår.Vilkårsresultat
import no.nav.familie.ef.sak.vilkår.VilkårsvurderingRepository
import no.nav.familie.ef.sak.vilkår.regler.HovedregelMetadata
import no.nav.familie.ef.sak.vilkår.regler.vilkår.AleneomsorgRegel
import no.nav.familie.ef.sak.vilkår.regler.vilkår.SivilstandRegel
import no.nav.familie.kontrakter.ef.søknad.Barn
import no.nav.familie.kontrakter.ef.søknad.TestsøknadBuilder
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

@Profile("integrasjonstest")
@Service
class VilkårHelperService {
    @Autowired
    private lateinit var barnRepository: BarnRepository

    @Autowired
    private lateinit var vilkårsvurderingRepository: VilkårsvurderingRepository

    @Autowired
    private lateinit var søknadHelperService: SøknadHelperService

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    fun opprettVilkår(
        behandling: Behandling,
        barn: List<Barn> =
            listOf(
                TestsøknadBuilder.Builder().defaultBarn("Barn Barnesen", PdlClientConfig.BARN_FNR),
                TestsøknadBuilder.Builder().defaultBarn("Barn2 Barnesen", PdlClientConfig.BARN2_FNR),
            ),
    ) {
        val fagsak = fagsakRepository.findByIdOrThrow(behandling.fagsakId)

        val sivilstand =
            when (fagsak.stønadstype) {
                StønadType.OVERGANGSSTØNAD,
                StønadType.SKOLEPENGER,
                -> søknadHelperService.lagreSøknad(behandling, barn).sivilstand
                StønadType.BARNETILSYN -> søknadHelperService.lagreSøknadForBarnetilsyn(behandling, barn).sivilstand
            }

        val barn = barnRepository.findByBehandlingId(behandling.id)
        val delvilkårsvurdering =
            lagSivilstandDelvilkår(sivilstand)

        val delvilkårsvurderingAleneomsorg = lagDelvilkårsvurderingAleneomsorg(barn, sivilstand, behandling)
        lagreVilkår(behandling, delvilkårsvurdering, barn, delvilkårsvurderingAleneomsorg)
    }

    fun lagSivilstandDelvilkår(sivilstand: Sivilstand?): List<Delvilkårsvurdering> {
        val behandlingMock = mockk<Behandling>()
        every { behandlingMock.erDigitalSøknad() } returns true
        val delvilkårsvurdering =
            SivilstandRegel().initiereDelvilkårsvurdering(
                HovedregelMetadata(
                    sivilstand,
                    Sivilstandstype.ENKE_ELLER_ENKEMANN,
                    barn = emptyList(),
                    søktOmBarnetilsyn = emptyList(),
                    vilkårgrunnlagDto = mockk(),
                    behandling = behandlingMock,
                ),
            )
        return delvilkårsvurdering
    }

    fun lagDelvilkårsvurderingAleneomsorg(
        barn: List<BehandlingBarn>,
        sivilstand: Sivilstand?,
        behandling: Behandling,
    ): List<Delvilkårsvurdering> {
        val delvilkårsvurderingAleneomsorg =
            AleneomsorgRegel().initiereDelvilkårsvurdering(
                HovedregelMetadata(
                    sivilstand,
                    Sivilstandstype.ENKE_ELLER_ENKEMANN,
                    barn = barn,
                    søktOmBarnetilsyn = emptyList(),
                    vilkårgrunnlagDto = VilkårTestUtil.mockVilkårGrunnlagDto(),
                    behandling = behandling,
                ),
            )
        return delvilkårsvurderingAleneomsorg
    }

    fun lagreVilkår(
        behandling: Behandling,
        delvilkårsvurdering: List<Delvilkårsvurdering>,
        barn: List<BehandlingBarn>,
        delvilkårsvurderingAleneomsorg: List<Delvilkårsvurdering>,
    ) {
        val vilkårForBarn =
            barn.map {
                vilkårsvurdering(
                    resultat = Vilkårsresultat.OPPFYLT,
                    type = VilkårType.ALENEOMSORG,
                    behandlingId = behandling.id,
                    barnId = it.id,
                    delvilkårsvurdering = delvilkårsvurderingAleneomsorg,
                )
            }
        vilkårsvurderingRepository.insertAll(
            vilkårForBarn +
                vilkårsvurdering(
                    resultat = Vilkårsresultat.OPPFYLT,
                    type = VilkårType.SIVILSTAND,
                    behandlingId = behandling.id,
                    delvilkårsvurdering = delvilkårsvurdering,
                ),
        )
    }
}
