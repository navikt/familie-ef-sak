package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.api.Feil
import no.nav.familie.ef.sak.api.dto.OppdaterVilkårsvurderingDto
import no.nav.familie.ef.sak.api.dto.OppdatertVilkårsvurderingResponseDto
import no.nav.familie.ef.sak.api.dto.VilkårDto
import no.nav.familie.ef.sak.api.dto.VilkårGrunnlagDto
import no.nav.familie.ef.sak.api.dto.VilkårsvurderingDto
import no.nav.familie.ef.sak.api.dto.tilDto
import no.nav.familie.ef.sak.regler.Vilkårsregel
import no.nav.familie.ef.sak.regler.alleVilkårsregler
import no.nav.familie.ef.sak.regler.evalutation.OppdaterVilkår
import no.nav.familie.ef.sak.repository.VilkårsvurderingRepository
import no.nav.familie.ef.sak.repository.domain.DelvilkårMetadata
import no.nav.familie.ef.sak.repository.domain.Delvilkårsvurdering
import no.nav.familie.ef.sak.repository.domain.DelvilkårsvurderingWrapper
import no.nav.familie.ef.sak.repository.domain.VilkårType
import no.nav.familie.ef.sak.repository.domain.Vilkårsresultat
import no.nav.familie.ef.sak.repository.domain.Vilkårsvurdering
import no.nav.familie.ef.sak.repository.domain.Vurdering
import no.nav.familie.ef.sak.repository.domain.søknad.SøknadsskjemaOvergangsstønad
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class VurderingService(private val behandlingService: BehandlingService,
                       private val vilkårsvurderingRepository: VilkårsvurderingRepository,
                       private val grunnlagsdataService: GrunnlagsdataService) {

    fun oppdaterVilkår(vilkårsvurderingDto: OppdaterVilkårsvurderingDto): VilkårsvurderingDto {
        val vilkårsvurdering = vilkårsvurderingRepository.findByIdOrThrow(vilkårsvurderingDto.id)

        val behandlingId = vilkårsvurdering.behandlingId
        if (behandlingErLåstForVidereRedigering(behandlingId)) {
            throw Feil("Bruker prøver å oppdatere en vilkårsvurdering der behandling=$behandlingId er låst for videre redigering",
                       "Behandlingen er låst for videre redigering")
        }

        val nyVilkårsvurdering = OppdaterVilkår.lagNyOppdatertVilkårsvurdering(vilkårsvurdering,
                                                                               vilkårsvurderingDto.delvilkårsvurderinger)
        return vilkårsvurderingRepository.update(nyVilkårsvurdering).tilDto()
    }

    fun hentVilkår(behandlingId: UUID): VilkårDto {
        val søknad = behandlingService.hentOvergangsstønad(behandlingId)
        val grunnlag = grunnlagsdataService.hentGrunnlag(behandlingId, søknad)
        val vurderinger = hentVurderinger(behandlingId, søknad, grunnlag)
        return VilkårDto(vurderinger = vurderinger, grunnlag = grunnlag)
    }

    private fun hentVurderinger(behandlingId: UUID,
                                søknad: SøknadsskjemaOvergangsstønad,
                                registergrunnlag: VilkårGrunnlagDto): List<VilkårsvurderingDto> {
        val delvilkårMetadata = DelvilkårMetadata(sivilstandstype = registergrunnlag.sivilstand.registergrunnlag.type)
        return hentEllerOpprettVurderingerForVilkår(behandlingId, søknad, delvilkårMetadata)
                .map(Vilkårsvurdering::tilDto)
    }

    private fun hentEllerOpprettVurderingerForVilkår(behandlingId: UUID,
                                                     søknad: SøknadsskjemaOvergangsstønad,
                                                     delvilkårMetadata: DelvilkårMetadata): List<Vilkårsvurdering> {
        val lagredeVilkårsvurderinger = vilkårsvurderingRepository.findByBehandlingId(behandlingId)

        if (behandlingErLåstForVidereRedigering(behandlingId)) {
            return lagredeVilkårsvurderinger
        }

        // todo håndtere vilkår som allerede finnes eller barn som allerede finnes, men hvordan håndtere barn som blir slettede?
        // alleVilkårsregler skal kanskje ikke være en funksjon?
        val nyeVilkårsvurderinger: List<Vilkårsvurdering> = alleVilkårsregler
                .filter { lagredeVilkårsvurderinger.find { vurdering -> vurdering.type === it.vilkårType } == null }
                .flatMap { vilkårsregel ->

                    if (vilkårsregel.vilkårType == VilkårType.ALENEOMSORG) {
                        søknad.barn.map { lagNyVilkårsvurdering(vilkårsregel, søknad, delvilkårMetadata, behandlingId, it.id) }
                    } else {
                        listOf(lagNyVilkårsvurdering(vilkårsregel, søknad, delvilkårMetadata, behandlingId))
                    }
                }

        vilkårsvurderingRepository.insertAll(nyeVilkårsvurderinger)

        return lagredeVilkårsvurderinger + nyeVilkårsvurderinger
    }

    private fun lagNyVilkårsvurdering(vilkårsregel: Vilkårsregel,
                                      søknad: SøknadsskjemaOvergangsstønad,
                                      delvilkårMetadata: DelvilkårMetadata,
                                      behandlingId: UUID,
                                      barnId: UUID? = null): Vilkårsvurdering {
        val delvilkårsvrdering = vilkårsregel.hovedregler.map {
            Delvilkårsvurdering(vurderinger = listOf(Vurdering(regelId = it)))
        }
        return Vilkårsvurdering(behandlingId = behandlingId,
                                type = vilkårsregel.vilkårType,
                                barnId = barnId,
                                delvilkårsvurdering = DelvilkårsvurderingWrapper(delvilkårsvrdering))
    }


    fun hentVilkårSomManglerVurdering(behandlingId: UUID): List<VilkårType> {
        val lagredeVilkårsvurderinger = vilkårsvurderingRepository.findByBehandlingId(behandlingId)
        val vilkår = VilkårType.hentVilkår()

        return vilkår.filter {
            lagredeVilkårsvurderinger.any { vurdering ->
                vurdering.type == it
                && vurdering.resultat == Vilkårsresultat.IKKE_TATT_STILLING_TIL
            }
            || lagredeVilkårsvurderinger.none { vurdering -> vurdering.type == it }
        }
    }

    private fun behandlingErLåstForVidereRedigering(behandlingId: UUID) =
            behandlingService.hentBehandling(behandlingId).status.behandlingErLåstForVidereRedigering()

}
