package no.nav.familie.ef.sak.vilkår

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegService
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType
import no.nav.familie.ef.sak.behandlingsflyt.steg.VilkårSteg
import no.nav.familie.ef.sak.behandlingsflyt.task.BehandlingsstatistikkTask
import no.nav.familie.ef.sak.behandlingshistorikk.BehandlingshistorikkService
import no.nav.familie.ef.sak.behandlingshistorikk.domain.StegUtfall
import no.nav.familie.ef.sak.blankett.BlankettRepository
import no.nav.familie.ef.sak.infrastruktur.exception.ApiFeil
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.oppgave.TilordnetRessursService
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.ef.sak.vilkår.dto.OppdaterVilkårsvurderingDto
import no.nav.familie.ef.sak.vilkår.dto.SvarPåVurderingerDto
import no.nav.familie.ef.sak.vilkår.dto.VilkårsvurderingDto
import no.nav.familie.ef.sak.vilkår.dto.tilDto
import no.nav.familie.ef.sak.vilkår.regler.evalutation.OppdaterVilkår
import no.nav.familie.ef.sak.vilkår.regler.evalutation.OppdaterVilkår.utledBehandlingKategori
import no.nav.familie.ef.sak.vilkår.regler.evalutation.OppdaterVilkår.utledResultatForVilkårSomGjelderFlereBarn
import no.nav.familie.ef.sak.vilkår.regler.hentVilkårsregel
import no.nav.familie.prosessering.internal.TaskService
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class VurderingStegService(
    private val behandlingService: BehandlingService,
    private val vurderingService: VurderingService,
    private val vilkårsvurderingRepository: VilkårsvurderingRepository,
    private val vilkårSteg: VilkårSteg,
    private val stegService: StegService,
    private val taskService: TaskService,
    private val blankettRepository: BlankettRepository,
    private val behandlingshistorikkService: BehandlingshistorikkService,
    private val tilordnetRessursService: TilordnetRessursService,
) {
    @Transactional
    fun oppdaterVilkår(vilkårsvurderingDto: SvarPåVurderingerDto): VilkårsvurderingDto {
        val vilkårsvurdering = vilkårsvurderingRepository.findByIdOrThrow(vilkårsvurderingDto.id)
        val behandlingId = vilkårsvurdering.behandlingId

        validerLåstForVidereRedigering(behandlingId)
        validerBehandlingIdErLikIRequestOgIVilkåret(behandlingId, vilkårsvurderingDto.behandlingId)

        val nyVilkårsvurdering =
            OppdaterVilkår.lagNyOppdatertVilkårsvurdering(
                vilkårsvurdering,
                vilkårsvurderingDto.delvilkårsvurderinger,
            )
        blankettRepository.deleteById(behandlingId)
        val oppdatertVilkårsvurderingDto = vilkårsvurderingRepository.update(nyVilkårsvurdering).tilDto()
        oppdaterStegOgKategoriPåBehandling(vilkårsvurdering.behandlingId)
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
        oppdaterStegOgKategoriPåBehandling(behandlingId)
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
        oppdaterStegOgKategoriPåBehandling(behandlingId)
        return oppdatertVilkår
    }

    private fun oppdaterStegOgKategoriPåBehandling(behandlingId: UUID) {
        val saksbehandling = behandlingService.hentSaksbehandling(behandlingId)
        val lagredeVilkårsvurderinger = vilkårsvurderingRepository.findByBehandlingId(behandlingId)

        oppdaterStegPåBehandling(saksbehandling, lagredeVilkårsvurderinger)
        oppdaterKategoriPåBehandling(saksbehandling, lagredeVilkårsvurderinger)
    }

    private fun oppdaterStegPåBehandling(
        saksbehandling: Saksbehandling,
        vilkårsvurderinger: List<Vilkårsvurdering>,
    ) {
        val vilkårsresultat =
            vilkårsvurderinger.groupBy { it.type }.map {
                if (it.key.gjelderFlereBarn()) {
                    utledResultatForVilkårSomGjelderFlereBarn(it.value)
                } else {
                    it.value.single().resultat
                }
            }

        if (saksbehandling.steg == StegType.VILKÅR && OppdaterVilkår.erAlleVilkårTattStillingTil(vilkårsresultat)) {
            stegService.håndterSteg(saksbehandling, vilkårSteg, null).id
        } else if (saksbehandling.steg != StegType.VILKÅR && vilkårsresultat.any { it == Vilkårsresultat.IKKE_TATT_STILLING_TIL }) {
            stegService.resetSteg(saksbehandling.id, StegType.VILKÅR)
        } else if (saksbehandling.harStatusOpprettet) {
            behandlingService.oppdaterStatusPåBehandling(saksbehandling.id, BehandlingStatus.UTREDES)
            behandlingshistorikkService.opprettHistorikkInnslag(behandlingId = saksbehandling.id, stegtype = StegType.VILKÅR, utfall = StegUtfall.UTREDNING_PÅBEGYNT, metadata = null)
            opprettBehandlingsstatistikkTask(saksbehandling)
        }
    }

    private fun oppdaterKategoriPåBehandling(
        saksbehandling: Saksbehandling,
        vilkårsvurderinger: List<Vilkårsvurdering>,
    ) {
        val lagretKategori = saksbehandling.kategori
        val utledetKategori = utledBehandlingKategori(vilkårsvurderinger)

        if (lagretKategori != utledetKategori) {
            behandlingService.oppdaterKategoriPåBehandling(saksbehandling.id, utledetKategori)
        }
    }

    private fun opprettBehandlingsstatistikkTask(saksbehandling: Saksbehandling) {
        taskService.save(BehandlingsstatistikkTask.opprettPåbegyntTask(behandlingId = saksbehandling.id))
    }

    private fun nullstillVilkårMedNyeHovedregler(
        behandlingId: UUID,
        vilkårsvurdering: Vilkårsvurdering,
    ): VilkårsvurderingDto {
        val metadata = hentHovedregelMetadata(behandlingId)
        val nyeDelvilkår = hentVilkårsregel(vilkårsvurdering.type).initiereDelvilkårsvurdering(metadata)
        val delvilkårsvurdering = DelvilkårsvurderingWrapper(nyeDelvilkår)

        return vilkårsvurderingRepository
            .update(
                vilkårsvurdering.copy(
                    resultat = Vilkårsresultat.IKKE_TATT_STILLING_TIL,
                    delvilkårsvurdering = delvilkårsvurdering,
                    opphavsvilkår = null,
                ),
            ).tilDto()
    }

    private fun oppdaterVilkårsvurderingTilSkalIkkeVurderes(
        behandlingId: UUID,
        vilkårsvurdering: Vilkårsvurdering,
    ): VilkårsvurderingDto {
        val metadata = hentHovedregelMetadata(behandlingId)
        val nyeDelvilkår =
            hentVilkårsregel(vilkårsvurdering.type).initiereDelvilkårsvurdering(
                metadata,
                Vilkårsresultat.SKAL_IKKE_VURDERES,
            )
        val delvilkårsvurdering = DelvilkårsvurderingWrapper(nyeDelvilkår)
        return vilkårsvurderingRepository
            .update(
                vilkårsvurdering.copy(
                    resultat = Vilkårsresultat.SKAL_IKKE_VURDERES,
                    delvilkårsvurdering = delvilkårsvurdering,
                    opphavsvilkår = null,
                ),
            ).tilDto()
    }

    private fun hentHovedregelMetadata(behandlingId: UUID) = vurderingService.hentGrunnlagOgMetadata(behandlingId).second

    private fun validerLåstForVidereRedigering(behandlingId: UUID) {
        if (behandlingErLåstForVidereRedigering(behandlingId)) {
            throw ApiFeil("Behandlingen er låst for videre redigering", HttpStatus.BAD_REQUEST)
        }
        if (!tilordnetRessursService.tilordnetRessursErInnloggetSaksbehandler(behandlingId)) {
            throw ApiFeil("Behandlingen eies av en annen saksbehandler", HttpStatus.BAD_REQUEST)
        }
    }

    /**
     * Tilgangskontroll sjekker att man har tilgang til behandlingId som blir sendt inn, men det er mulig å sende inn
     * en annen behandlingId enn den som er på vilkåret
     */
    private fun validerBehandlingIdErLikIRequestOgIVilkåret(
        behandlingId: UUID,
        requestBehandlingId: UUID,
    ) {
        if (behandlingId != requestBehandlingId) {
            throw Feil(
                "BehandlingId=$requestBehandlingId er ikke lik vilkårets sin behandlingId=$behandlingId",
                "BehandlingId er feil, her har noe gått galt",
                httpStatus = HttpStatus.BAD_REQUEST,
            )
        }
    }

    private fun behandlingErLåstForVidereRedigering(behandlingId: UUID) = behandlingService.hentBehandling(behandlingId).status.behandlingErLåstForVidereRedigering()
}
