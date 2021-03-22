package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.api.Feil
import no.nav.familie.ef.sak.api.dto.NullstillVilkårsvurderingDto
import no.nav.familie.ef.sak.api.dto.OppdaterVilkårsvurderingDto
import no.nav.familie.ef.sak.api.dto.VilkårDto
import no.nav.familie.ef.sak.api.dto.VilkårGrunnlagDto
import no.nav.familie.ef.sak.api.dto.VilkårsvurderingDto
import no.nav.familie.ef.sak.api.dto.tilDto
import no.nav.familie.ef.sak.blankett.BlankettRepository
import no.nav.familie.ef.sak.regler.Vilkårsregel
import no.nav.familie.ef.sak.regler.alleVilkårsregler
import no.nav.familie.ef.sak.regler.evalutation.OppdaterVilkår
import no.nav.familie.ef.sak.regler.hentVilkårsregel
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
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class VurderingService(private val behandlingService: BehandlingService,
                       private val vilkårsvurderingRepository: VilkårsvurderingRepository,
                       private val grunnlagsdataService: GrunnlagsdataService,
                       private val blankettRepository: BlankettRepository) {

    fun oppdaterVilkår(vilkårsvurderingDto: OppdaterVilkårsvurderingDto): VilkårsvurderingDto {
        val vilkårsvurdering = vilkårsvurderingRepository.findByIdOrThrow(vilkårsvurderingDto.id)
        val behandlingId = vilkårsvurdering.behandlingId

        validerBehandlingIdErLikIRequestOgIVilkåret(behandlingId, vilkårsvurderingDto.behandlingId)
        validerLåstForVidereRedigering(behandlingId)

        val nyVilkårsvurdering = OppdaterVilkår.lagNyOppdatertVilkårsvurdering(vilkårsvurdering,
                                                                               vilkårsvurderingDto.delvilkårsvurderinger)
        blankettRepository.deleteById(behandlingId)
        return vilkårsvurderingRepository.update(nyVilkårsvurdering).tilDto()
    }

    fun nullstillVilkår(vilkårsvurderingDto: NullstillVilkårsvurderingDto): VilkårsvurderingDto {
        val vilkårsvurdering = vilkårsvurderingRepository.findByIdOrThrow(vilkårsvurderingDto.id)
        val behandlingId = vilkårsvurdering.behandlingId

        validerBehandlingIdErLikIRequestOgIVilkåret(behandlingId, vilkårsvurderingDto.behandlingId)
        validerLåstForVidereRedigering(behandlingId)

        blankettRepository.deleteById(behandlingId)

        if (vilkårsvurdering.resultat.oppfyltEllerIkkeOppfylt()) {
            val nyeDelvilkår = lagNyeDelvilkår(hentVilkårsregel(vilkårsvurdering.type))
            val delvilkårsvurdering = DelvilkårsvurderingWrapper(nyeDelvilkår)
            return vilkårsvurderingRepository.update(vilkårsvurdering.copy(resultat = Vilkårsresultat.IKKE_TATT_STILLING_TIL,
                                                                           delvilkårsvurdering = delvilkårsvurdering))
                    .tilDto()
        }
        return vilkårsvurdering.tilDto();
    }

    fun hentVilkår(behandlingId: UUID): VilkårDto {
        val søknad = behandlingService.hentOvergangsstønad(behandlingId)
        val grunnlag = grunnlagsdataService.hentGrunnlag(behandlingId, søknad)
        val vurderinger = hentVurderinger(behandlingId, søknad, grunnlag)
        return VilkårDto(vurderinger = vurderinger, grunnlag = grunnlag)
    }

    private fun validerLåstForVidereRedigering(behandlingId: UUID) {
        if (behandlingErLåstForVidereRedigering(behandlingId)) {
            throw Feil("Bruker prøver å oppdatere en vilkårsvurdering der behandling=$behandlingId er låst for videre redigering",
                       "Behandlingen er låst for videre redigering")
        }
    }

    /**
     * Tilgangskontroll sjekker att man har tilgang til behandlingId som blir sendt inn, men det er mulig å sende inn
     * en annen behandlingId enn den som er på vilkåret
     */
    private fun validerBehandlingIdErLikIRequestOgIVilkåret(behandlingId: UUID, requestBehandlingId: UUID) {
        if (behandlingId != requestBehandlingId) {
            throw Feil("BehandlingId=$requestBehandlingId er ikke lik vilkårets sin behandlingId=${behandlingId}",
                       "BehandlingId er feil, her har noe gått galt",
                       httpStatus = HttpStatus.BAD_REQUEST)
        }
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
        val delvilkårsvrdering = lagNyeDelvilkår(vilkårsregel)
        return Vilkårsvurdering(behandlingId = behandlingId,
                                type = vilkårsregel.vilkårType,
                                barnId = barnId,
                                delvilkårsvurdering = DelvilkårsvurderingWrapper(delvilkårsvrdering))
    }

    private fun lagNyeDelvilkår(vilkårsregel: Vilkårsregel): List<Delvilkårsvurdering> {
        return vilkårsregel.hovedregler.map {
            Delvilkårsvurdering(vurderinger = listOf(Vurdering(regelId = it)))
        }
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
