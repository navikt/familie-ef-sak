package no.nav.familie.ef.sak.vilkår

import no.nav.familie.ef.sak.api.Feil
import no.nav.familie.ef.sak.api.dto.OppdaterVilkårsvurderingDto
import no.nav.familie.ef.sak.api.dto.SvarPåVurderingerDto
import no.nav.familie.ef.sak.api.dto.VilkårGrunnlagDto
import no.nav.familie.ef.sak.api.dto.VilkårsvurderingDto
import no.nav.familie.ef.sak.api.dto.tilDto
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.blankett.BlankettRepository
import no.nav.familie.ef.sak.vilkår.regler.HovedregelMetadata
import no.nav.familie.ef.sak.vilkår.regler.evalutation.OppdaterVilkår
import no.nav.familie.ef.sak.vilkår.regler.evalutation.OppdaterVilkår.utledResultatForAleneomsorg
import no.nav.familie.ef.sak.vilkår.regler.hentVilkårsregel
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.ef.sak.service.SøknadService
import no.nav.familie.ef.sak.service.steg.StegService
import no.nav.familie.ef.sak.service.steg.StegType
import no.nav.familie.ef.sak.task.BehandlingsstatistikkTask
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class VurderingStegService(private val behandlingService: BehandlingService,
                           private val søknadService: SøknadService,
                           private val vilkårsvurderingRepository: VilkårsvurderingRepository,
                           private val vilkårGrunnlagService: VilkårGrunnlagService,
                           private val stegService: StegService,
                           private val taskRepository: TaskRepository,
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
    fun settVilkårTilSkalIkkeVurderes(vilkårsvurderingDto: OppdaterVilkårsvurderingDto): VilkårsvurderingDto {
        val vilkårsvurdering = vilkårsvurderingRepository.findByIdOrThrow(vilkårsvurderingDto.id)
        val behandlingId = vilkårsvurdering.behandlingId

        validerBehandlingIdErLikIRequestOgIVilkåret(behandlingId, vilkårsvurderingDto.behandlingId)
        validerLåstForVidereRedigering(behandlingId)

        blankettRepository.deleteById(behandlingId)

        val oppdatertVilkår = oppdaterVilkårsvurderingTilSkalIkkeVurderes(behandlingId, vilkårsvurdering)
        oppdaterStegPåBehandling(behandlingId)
        return oppdatertVilkår
    }

    private fun oppdaterStegPåBehandling(behandlingId: UUID) {
        val behandling = behandlingService.hentBehandling(behandlingId)
        val lagredeVilkårsvurderinger = vilkårsvurderingRepository.findByBehandlingId(behandlingId)
                .filter { it.type != VilkårType.TIDLIGERE_VEDTAKSPERIODER } // TODO: Må håndteres senere
        val vilkårsresultat = lagredeVilkårsvurderinger.groupBy { it.type }.map {
            if (it.key == VilkårType.ALENEOMSORG) {
                utledResultatForAleneomsorg(it.value)
            } else {
                it.value.single().resultat
            }
        }

        if (behandling.steg == StegType.VILKÅR && OppdaterVilkår.erAlleVilkårVurdert(vilkårsresultat)) {
            stegService.håndterVilkår(behandling).id
        } else if (behandling.steg != StegType.VILKÅR && vilkårsresultat.any { it == Vilkårsresultat.IKKE_TATT_STILLING_TIL }) {
            stegService.resetSteg(behandling.id, StegType.VILKÅR)
        } else if (erInitiellVurderingAvVilkår(behandling)) {
            behandlingService.oppdaterStatusPåBehandling(behandling.id, BehandlingStatus.UTREDES)
            opprettBehandlingsstatistikkTask(behandling)
        }
    }

    private fun opprettBehandlingsstatistikkTask(behandling: Behandling) {
        if (behandling.type != BehandlingType.BLANKETT) {
            taskRepository.save(BehandlingsstatistikkTask.opprettPåbegyntTask(behandlingId = behandling.id))
        }

    }

    private fun erInitiellVurderingAvVilkår(behandling: Behandling): Boolean {
        return behandling.status == BehandlingStatus.OPPRETTET
    }

    private fun nullstillVilkårMedNyeHovedregler(behandlingId: UUID,
                                                 vilkårsvurdering: Vilkårsvurdering): VilkårsvurderingDto {
        val metadata = hentHovedregelMetadata(behandlingId)
        val nyeDelvilkår = hentVilkårsregel(vilkårsvurdering.type).initereDelvilkårsvurdering(metadata)
        val delvilkårsvurdering = DelvilkårsvurderingWrapper(nyeDelvilkår)
        return vilkårsvurderingRepository.update(vilkårsvurdering.copy(resultat = Vilkårsresultat.IKKE_TATT_STILLING_TIL,
                                                                       delvilkårsvurdering = delvilkårsvurdering)).tilDto()
    }

    private fun oppdaterVilkårsvurderingTilSkalIkkeVurderes(behandlingId: UUID,
                                                            vilkårsvurdering: Vilkårsvurdering): VilkårsvurderingDto {
        val metadata = hentHovedregelMetadata(behandlingId)
        val nyeDelvilkår = hentVilkårsregel(vilkårsvurdering.type).initereDelvilkårsvurdering(metadata,
                                                                                              Vilkårsresultat.SKAL_IKKE_VURDERES)
        val delvilkårsvurdering = DelvilkårsvurderingWrapper(nyeDelvilkår)
        return vilkårsvurderingRepository.update(vilkårsvurdering.copy(resultat = Vilkårsresultat.SKAL_IKKE_VURDERES,
                                                                       delvilkårsvurdering = delvilkårsvurdering)).tilDto()
    }

    private fun hentGrunnlagOgMetadata(behandlingId: UUID): Pair<VilkårGrunnlagDto, HovedregelMetadata> {
        val søknad = søknadService.hentOvergangsstønad(behandlingId)
        val grunnlag = vilkårGrunnlagService.hentGrunnlag(behandlingId, søknad)
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

    private fun behandlingErLåstForVidereRedigering(behandlingId: UUID) =
            behandlingService.hentBehandling(behandlingId).status.behandlingErLåstForVidereRedigering()


}