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
    fun oppdaterVilkår(vilkårsvurderingDto: SvarPåVurderingerDto): VilkårsvurderingDto {
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
    fun nullstillVilkår(vilkårsvurderingDto: OppdaterVilkårsvurderingDto): VilkårsvurderingDto {
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
    fun ikkeVurderVilkår(vilkårsvurderingDto: OppdaterVilkårsvurderingDto): VilkårsvurderingDto {
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

    fun oppdaterStegPåBehandling(behandlingId: UUID) {
        val behandling = behandlingService.hentBehandling(behandlingId)
        val lagredeVilkårsvurderinger = vilkårsvurderingRepository.findByBehandlingId(behandlingId)
                .filter { it.type != VilkårType.TIDLIGERE_VEDTAKSPERIODER } // TODO: Må håndteres senere
        val vilkårstyper = VilkårType.hentVilkår().minus(VilkårType.TIDLIGERE_VEDTAKSPERIODER)
        val vilkårUtenVurdering =
                filtereVilkårMedResultat(behandlingId,
                                         lagredeVilkårsvurderinger,
                                         vilkårstyper,
                                         Vilkårsresultat.IKKE_TATT_STILLING_TIL)

        if (erAlleVilkårVurdert(behandling, lagredeVilkårsvurderinger, vilkårstyper)) {
            stegService.håndterVilkår(behandling).id
        } else if (vilkårUtenVurdering.isNotEmpty() && behandling.steg != StegType.VILKÅR) {
            stegService.resetSteg(behandling.id, StegType.VILKÅR)
        }
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

        return when {
            behandlingErLåstForVidereRedigering(behandlingId) -> lagredeVilkårsvurderinger
            lagredeVilkårsvurderinger.isEmpty() -> lagreNyeVilkårsvurderinger(behandlingId, metadata)
            else -> lagredeVilkårsvurderinger
        }
    }

    private fun lagreNyeVilkårsvurderinger(behandlingId: UUID,
                                           metadata: HovedregelMetadata): List<Vilkårsvurdering> {
        val nyeVilkårsvurderinger: List<Vilkårsvurdering> = opprettNyeVilkårsvurderinger(behandlingId, metadata)
        return vilkårsvurderingRepository.insertAll(nyeVilkårsvurderinger)
    }

    fun opprettNyeVilkårsvurderinger(behandlingId: UUID,
                                     metadata: HovedregelMetadata): List<Vilkårsvurdering> {
        return alleVilkårsregler
                .flatMap { vilkårsregel ->
                    if (vilkårsregel.vilkårType == VilkårType.ALENEOMSORG) {
                        metadata.søknad.barn.map {
                            lagNyVilkårsvurdering(vilkårsregel, metadata, behandlingId, it.id)
                        }
                    } else {
                        listOf(lagNyVilkårsvurdering(vilkårsregel, metadata, behandlingId))
                    }
                }
    }

    private fun lagNyVilkårsvurdering(vilkårsregel: Vilkårsregel,
                                      metadata: HovedregelMetadata,
                                      behandlingId: UUID,
                                      barnId: UUID? = null): Vilkårsvurdering {
        val delvilkårsvurdering = lagNyeDelvilkår(vilkårsregel, metadata)
        return Vilkårsvurdering(behandlingId = behandlingId,
                                type = vilkårsregel.vilkårType,
                                barnId = barnId,
                                delvilkårsvurdering = DelvilkårsvurderingWrapper(delvilkårsvurdering))
    }

    private fun lagNyeDelvilkår(vilkårsregel: Vilkårsregel, metadata: HovedregelMetadata): List<Delvilkårsvurdering> {
        return vilkårsregel.initereDelvilkårsvurderingMedVilkårsresultat(metadata,
                                                                         resultat = Vilkårsresultat.IKKE_TATT_STILLING_TIL)
    }

    /* erAlleVilkårVurdert må vi sjekke;
        Noen som har IKKE_TATT_STILLING_TIL -> false
        Noen som har SKAL_IKKE_VURDERES og resten OPPFYLT -> false
        Ellers -> true?
    */

    private fun erAlleVilkårVurdert(behandling: Behandling,
                                    lagredeVilkårsvurderinger: List<Vilkårsvurdering>,
                                    vilkårstyper: List<VilkårType>): Boolean {

        if (behandling.steg == StegType.VILKÅR) {
            val harNoenVurderingIkkeTattStillingTil = filtereVilkårMedResultat(behandling.id,
                                                                               lagredeVilkårsvurderinger,
                                                                               vilkårstyper,
                                                                               Vilkårsresultat.IKKE_TATT_STILLING_TIL).isNotEmpty()
            val harNoenVurderingSkallIkkeVurderes =
                    filtereVilkårMedResultat(behandling.id,
                                             lagredeVilkårsvurderinger,
                                             vilkårstyper,
                                             Vilkårsresultat.SKAL_IKKE_VURDERES).isNotEmpty()

            return when {
                harNoenVurderingIkkeTattStillingTil -> false
                harNoenVurderingSkallIkkeVurderes -> !harNoenSomSkalIkkeVurderesOgRestenErOppfylt(behandling.id,
                                                                                                  lagredeVilkårsvurderinger)
                else -> true
            }
        }
        return false
    }


    /* Hantering av bakåtkompatibilitet.*/
    private fun erAktuelltVilkårType(vilkårsvurdering: Vilkårsvurdering) = VilkårType.hentVilkår().contains(vilkårsvurdering.type)


    private fun behandlingErLåstForVidereRedigering(behandlingId: UUID) =
            behandlingService.hentBehandling(behandlingId).status.behandlingErLåstForVidereRedigering()

    fun filtereVilkårMedResultat(behandlingId: UUID,
                                 lagredeVilkårsvurderinger: List<Vilkårsvurdering>,
                                 vilkårstyper: List<VilkårType>,
                                 resultat: Vilkårsresultat): List<Vilkårsvurdering> {
        val aktuelleVilkår = lagredeVilkårsvurderinger
                .filter { erAktuelltVilkårType(it) }

        val antallVilkårstyper = aktuelleVilkår.map { v -> v.type }.distinct().size

        require(antallVilkårstyper == vilkårstyper.size)
        { "Forventer att det er like mange vilkår som finnes definert" }

        return aktuelleVilkår.filter { it.resultat == resultat }

    }

    fun harNoenSomSkalIkkeVurderesOgRestenErOppfylt(behandlingId: UUID,
                                                    lagredeVilkårsvurderinger: List<Vilkårsvurdering>): Boolean {

        return lagredeVilkårsvurderinger
                .filter { erAktuelltVilkårType(it) }
                .filter { it.resultat != Vilkårsresultat.SKAL_IKKE_VURDERES }
                .all { it.resultat == Vilkårsresultat.OPPFYLT }
    }


}