package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.api.Feil
import no.nav.familie.ef.sak.api.dto.NullstillVilkårsvurderingDto
import no.nav.familie.ef.sak.api.dto.OppdaterVilkårsvurderingDto
import no.nav.familie.ef.sak.api.dto.VilkårDto
import no.nav.familie.ef.sak.api.dto.VilkårsvurderingDto
import no.nav.familie.ef.sak.api.dto.tilDto
import no.nav.familie.ef.sak.blankett.BlankettRepository
import no.nav.familie.ef.sak.regler.HovedregelMetadata
import no.nav.familie.ef.sak.regler.Vilkårsregel
import no.nav.familie.ef.sak.regler.alleVilkårsregler
import no.nav.familie.ef.sak.regler.evalutation.OppdaterVilkår
import no.nav.familie.ef.sak.regler.hentVilkårsregel
import no.nav.familie.ef.sak.repository.VilkårsvurderingRepository
import no.nav.familie.ef.sak.repository.domain.*
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.ef.sak.service.steg.StegService
import no.nav.familie.ef.sak.service.steg.StegType
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class VurderingService(private val behandlingService: BehandlingService,
                       private val vilkårsvurderingRepository: VilkårsvurderingRepository,
                       private val grunnlagsdataService: GrunnlagsdataService,
                       private val stegService: StegService,
                       private val blankettRepository: BlankettRepository) {

    @Transactional
    fun oppdaterVilkår(vilkårsvurderingDto: OppdaterVilkårsvurderingDto): VilkårsvurderingDto {
        val vilkårsvurdering = vilkårsvurderingRepository.findByIdOrThrow(vilkårsvurderingDto.id)
        val behandlingId = vilkårsvurdering.behandlingId

        validerLåstForVidereRedigering(behandlingId)
        validerBehandlingIdErLikIRequestOgIVilkåret(behandlingId, vilkårsvurderingDto.behandlingId)

        val nyVilkårsvurdering = OppdaterVilkår.lagNyOppdatertVilkårsvurdering(vilkårsvurdering,
                                                                               vilkårsvurderingDto.delvilkårsvurderinger)
        blankettRepository.deleteById(behandlingId)
        val oppdatertVilkårsvurderingDto = vilkårsvurderingRepository.update(nyVilkårsvurdering).tilDto()
        oppdaterStegPåBehandling(vilkårsvurdering.behandlingId)
        return oppdatertVilkårsvurderingDto
    }

    @Transactional
    fun nullstillVilkår(vilkårsvurderingDto: NullstillVilkårsvurderingDto): VilkårsvurderingDto {
        val vilkårsvurdering = vilkårsvurderingRepository.findByIdOrThrow(vilkårsvurderingDto.id)
        val behandlingId = vilkårsvurdering.behandlingId

        validerBehandlingIdErLikIRequestOgIVilkåret(behandlingId, vilkårsvurderingDto.behandlingId)
        validerLåstForVidereRedigering(behandlingId)

        blankettRepository.deleteById(behandlingId)

        val nullstillVilkårMedNyeHovedregler = nullstillVilkårMedNyeHovedregler(behandlingId, vilkårsvurdering)
        oppdaterStegPåBehandling(behandlingId)
        return nullstillVilkårMedNyeHovedregler
    }

    private fun nullstillVilkårMedNyeHovedregler(behandlingId: UUID,
                                                 vilkårsvurdering: Vilkårsvurdering): VilkårsvurderingDto {
        val søknad = behandlingService.hentOvergangsstønad(behandlingId)
        val grunnlag = grunnlagsdataService.hentGrunnlag(behandlingId, søknad)
        val metadata = HovedregelMetadata(sivilstandstype = grunnlag.sivilstand.registergrunnlag.type,
                                          søknad = søknad)
        val nyeDelvilkår = lagNyeDelvilkår(hentVilkårsregel(vilkårsvurdering.type), metadata)
        val delvilkårsvurdering = DelvilkårsvurderingWrapper(nyeDelvilkår)
        return vilkårsvurderingRepository.update(vilkårsvurdering.copy(resultat = Vilkårsresultat.IKKE_TATT_STILLING_TIL,
                                                                       delvilkårsvurdering = delvilkårsvurdering)).tilDto()
    }

    fun hentVilkår(behandlingId: UUID): VilkårDto {
        val søknad = behandlingService.hentOvergangsstønad(behandlingId)
        val grunnlag = grunnlagsdataService.hentGrunnlag(behandlingId, søknad)
        val metadata = HovedregelMetadata(sivilstandstype = grunnlag.sivilstand.registergrunnlag.type,
                                          søknad = søknad)
        val vurderinger = hentVurderinger(behandlingId, metadata)
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
                                metadata: HovedregelMetadata): List<VilkårsvurderingDto> {
        return hentEllerOpprettVurderingerForVilkår(behandlingId, metadata).map(Vilkårsvurdering::tilDto)
    }

    private fun hentEllerOpprettVurderingerForVilkår(behandlingId: UUID,
                                                     metadata: HovedregelMetadata): List<Vilkårsvurdering> {
        val lagredeVilkårsvurderinger = vilkårsvurderingRepository.findByBehandlingId(behandlingId)

        if (behandlingErLåstForVidereRedigering(behandlingId)) {
            return lagredeVilkårsvurderinger
        }

        if (lagredeVilkårsvurderinger.isEmpty()) {
            return opprettNyeVilkårsvurderinger(behandlingId, metadata)
        } else {
            return lagredeVilkårsvurderinger
        }
    }

    private fun opprettNyeVilkårsvurderinger(behandlingId: UUID,
                                             metadata: HovedregelMetadata): List<Vilkårsvurdering> {
        val søknad = metadata.søknad
        val nyeVilkårsvurderinger: List<Vilkårsvurdering> = alleVilkårsregler
                .flatMap { vilkårsregel ->
                    if (vilkårsregel.vilkårType == VilkårType.ALENEOMSORG) {
                        søknad.barn.map {
                            lagNyVilkårsvurdering(vilkårsregel, metadata, behandlingId, it.id)
                        }
                    } else {
                        listOf(lagNyVilkårsvurdering(vilkårsregel, metadata, behandlingId))
                    }
                }

        return vilkårsvurderingRepository.insertAll(nyeVilkårsvurderinger)
    }

    private fun lagNyVilkårsvurdering(vilkårsregel: Vilkårsregel,
                                      metadata: HovedregelMetadata,
                                      behandlingId: UUID,
                                      barnId: UUID? = null): Vilkårsvurdering {
        val delvilkårsvrdering = lagNyeDelvilkår(vilkårsregel, metadata)
        return Vilkårsvurdering(behandlingId = behandlingId,
                                type = vilkårsregel.vilkårType,
                                barnId = barnId,
                                delvilkårsvurdering = DelvilkårsvurderingWrapper(delvilkårsvrdering))
    }

    private fun lagNyeDelvilkår(vilkårsregel: Vilkårsregel, metadata: HovedregelMetadata): List<Delvilkårsvurdering> {
        return vilkårsregel.initereDelvilkårsvurdering(metadata)
    }


    fun hentVilkårSomManglerVurdering(behandlingId: UUID): List<VilkårType> {
        val lagredeVilkårsvurderinger = vilkårsvurderingRepository.findByBehandlingId(behandlingId)
        val vilkår = VilkårType.hentVilkår()

        return vilkår
                .filter { it != VilkårType.TIDLIGERE_VEDTAKSPERIODER } // TODO: Må håndteres senere
                .filter {
                    lagredeVilkårsvurderinger.any { vurdering ->
                        vurdering.type == it
                        && vurdering.resultat == Vilkårsresultat.IKKE_TATT_STILLING_TIL
                    }
                    || lagredeVilkårsvurderinger.none { vurdering -> vurdering.type == it }
                }
    }

    private fun behandlingErLåstForVidereRedigering(behandlingId: UUID) =
            behandlingService.hentBehandling(behandlingId).status.behandlingErLåstForVidereRedigering()

    private fun oppdaterStegPåBehandling(behandlingId: UUID) {
        val behandling = behandlingService.hentBehandling(behandlingId)
        val vilkårUtenVurdering = hentVilkårSomManglerVurdering(behandlingId)

        if (skalFerdigstilleVilkårSteg(vilkårUtenVurdering, behandling)) {
            stegService.håndterVilkår(behandling)
        } else if (skalTilbakestilleTilVilkårSteg(vilkårUtenVurdering, behandling)) {
            stegService.resetSteg(behandling.id, StegType.VILKÅR)
        }
    }

    private fun skalTilbakestilleTilVilkårSteg(vilkårsvurdering: List<VilkårType>, behandling: Behandling) =
        vilkårsvurdering.isNotEmpty() && behandling.steg != StegType.VILKÅR

    private fun skalFerdigstilleVilkårSteg(vilkårsvurdering: List<VilkårType>, behandling: Behandling) =
        vilkårsvurdering.isEmpty() && behandling.steg == StegType.VILKÅR

}
