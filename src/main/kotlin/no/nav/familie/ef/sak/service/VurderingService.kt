package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.api.Feil
import no.nav.familie.ef.sak.api.dto.*
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


    @Transactional
    fun ikkeVurderVilkår(vilkårsvurderingDto: NullstillVilkårsvurderingDto): VilkårsvurderingDto {
        val vilkårsvurdering = vilkårsvurderingRepository.findByIdOrThrow(vilkårsvurderingDto.id)
        val behandlingId = vilkårsvurdering.behandlingId

        validerBehandlingIdErLikIRequestOgIVilkåret(behandlingId, vilkårsvurderingDto.behandlingId)
        validerLåstForVidereRedigering(behandlingId)

        blankettRepository.deleteById(behandlingId)

        val oppdatertVilkår = oppdaterVilkårsvurderingTilIkkeVurder(behandlingId, vilkårsvurdering)
        oppdaterStegPåBehandling(behandlingId)
        return oppdatertVilkår
    }

    @Transactional
    fun hentEllerOpprettVurderinger(behandlingId: UUID): VilkårDto {
        val (grunnlag, metadata) = hentGrunnlagOgMetadata(behandlingId)
        val vurderinger = hentEllerOpprettVurderinger(behandlingId, metadata)
        return VilkårDto(vurderinger = vurderinger, grunnlag = grunnlag)
    }

    private fun nullstillVilkårMedNyeHovedregler(behandlingId: UUID,
                                                 vilkårsvurdering: Vilkårsvurdering): VilkårsvurderingDto {
        val metadata = hentHovedregelMetadata(behandlingId)
        val nyeDelvilkår = lagNyeDelvilkår(hentVilkårsregel(vilkårsvurdering.type), metadata)
        val delvilkårsvurdering = DelvilkårsvurderingWrapper(nyeDelvilkår)
        return vilkårsvurderingRepository.update(vilkårsvurdering.copy(resultat = Vilkårsresultat.IKKE_TATT_STILLING_TIL,
                                                                       delvilkårsvurdering = delvilkårsvurdering)).tilDto()
    }

    private fun oppdaterVilkårsvurderingTilIkkeVurder(behandlingId: UUID,
                                                      vilkårsvurdering: Vilkårsvurdering): VilkårsvurderingDto {
        val metadata = hentHovedregelMetadata(behandlingId)
        val nyeDelvilkår = hentVilkårsregel(vilkårsvurdering.type).initereDelvilkårsvurderingMedVilkårsresultat(metadata,
                                                                                                                Vilkårsresultat.SKAL_IKKE_VURDERES)
        val delvilkårsvurdering = DelvilkårsvurderingWrapper(nyeDelvilkår)
        return vilkårsvurderingRepository.update(vilkårsvurdering.copy(resultat = Vilkårsresultat.SKAL_IKKE_VURDERES,
                                                                       delvilkårsvurdering = delvilkårsvurdering)).tilDto()
    }


    private fun hentGrunnlagOgMetadata(behandlingId: UUID): Pair<VilkårGrunnlagDto, HovedregelMetadata> {
        val søknad = behandlingService.hentOvergangsstønad(behandlingId)
        val grunnlag = grunnlagsdataService.hentGrunnlag(behandlingId, søknad)
        val metadata = HovedregelMetadata(sivilstandstype = grunnlag.sivilstand.registergrunnlag.type,
                                          søknad = søknad)
        return Pair(grunnlag, metadata)
    }

    private fun hentHovedregelMetadata(behandlingId: UUID) = hentGrunnlagOgMetadata(behandlingId).second

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

    private fun hentEllerOpprettVurderinger(behandlingId: UUID,
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
        return vilkårsregel.initereDelvilkårsvurderingMedVilkårsresultat(metadata,
                                                                         resultat = Vilkårsresultat.IKKE_TATT_STILLING_TIL)
    }


    fun hentVilkåMedResultattypen(behandlingId: UUID,
                                  lagredeVilkårsvurderinger: List<Vilkårsvurdering>,
                                  resultat: Vilkårsresultat): List<VilkårType> {
        return VilkårType.hentVilkår()
                .filter { it != VilkårType.TIDLIGERE_VEDTAKSPERIODER } // TODO: Må håndteres senere
                .filter { erAktuellVilkårType(lagredeVilkårsvurderinger, it) }
                .filter { finnesEttVilkårMedResultat(lagredeVilkårsvurderinger, it, resultat) }

    }

    fun harNoenSomSkalIkkeVurderesOgRestenErOppfylt(behandlingId: UUID,
                                                    lagredeVilkårsvurderinger: List<Vilkårsvurdering>): Boolean {
        val tr = VilkårType.hentVilkår();
        return lagredeVilkårsvurderinger
                .filter { it.type != VilkårType.TIDLIGERE_VEDTAKSPERIODER }
                .filter { it.resultat != Vilkårsresultat.SKAL_IKKE_VURDERES }
                .filter { tr.contains(it.type) }
                .all { it.resultat == Vilkårsresultat.SKAL_IKKE_VURDERES }
    }

    private fun oppdaterStegPåBehandling(behandlingId: UUID) {
        val behandling = behandlingService.hentBehandling(behandlingId)
        val lagredeVilkårsvurderinger = vilkårsvurderingRepository.findByBehandlingId(behandlingId)
        val vilkårUtenVurdering =
                hentVilkåMedResultattypen(behandlingId, lagredeVilkårsvurderinger, Vilkårsresultat.IKKE_TATT_STILLING_TIL)

        if (erAlleVilkårVurdert(behandling, lagredeVilkårsvurderinger)) {
            stegService.håndterVilkår(behandling).id
        } else if (vilkårUtenVurdering.isNotEmpty() && behandling.steg != StegType.VILKÅR) {
            stegService.resetSteg(behandling.id, StegType.VILKÅR)
        }
    }

    /* erAlleVilkårVurdert må vi sjekke;
        Noen som har IKKE_TATT_STILLING_TIL -> false
        Noen som har SKAL_IKKE_VURDERES og resten OPPFYLT -> false
        Ellers -> true?
    */

    private fun erAlleVilkårVurdert(behandling: Behandling, lagredeVilkårsvurderinger: List<Vilkårsvurdering>): Boolean {

        if (behandling.steg == StegType.VILKÅR) {
            val harNoenVurderingIkkeTattStillingTil = hentVilkåMedResultattypen(behandling.id,
                                                                                lagredeVilkårsvurderinger,
                                                                                Vilkårsresultat.IKKE_TATT_STILLING_TIL).isNotEmpty()
            val harNoenVurderingSkallIkkeVurderes =
                    hentVilkåMedResultattypen(behandling.id,
                                              lagredeVilkårsvurderinger,
                                              Vilkårsresultat.SKAL_IKKE_VURDERES).isNotEmpty()

            if (harNoenVurderingIkkeTattStillingTil) {
                return false
            }

            if (harNoenVurderingSkallIkkeVurderes) {
                return !harNoenSomSkalIkkeVurderesOgRestenErOppfylt(behandling.id, lagredeVilkårsvurderinger)
            }
            return true;
        }


        return false
    }


    /* Hantering av bakåtkompatibilitet.*/
    private fun erAktuellVilkårType(lagredeVilkårsvurderinger: List<Vilkårsvurdering>,
                                    it: VilkårType) =
            lagredeVilkårsvurderinger.none { vurdering -> vurdering.type == it }

    private fun finnesEttVilkårMedResultat(lagredeVilkårsvurderinger: List<Vilkårsvurdering>,
                                           vilkårType: VilkårType, resultat: Vilkårsresultat) =
            lagredeVilkårsvurderinger.any { vurdering ->
                vurdering.type == vilkårType
                && vurdering.resultat == resultat
            }

    private fun behandlingErLåstForVidereRedigering(behandlingId: UUID) =
            behandlingService.hentBehandling(behandlingId).status.behandlingErLåstForVidereRedigering()
}
