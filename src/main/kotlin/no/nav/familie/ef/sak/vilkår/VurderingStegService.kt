package no.nav.familie.ef.sak.vilkår

import no.nav.familie.ef.sak.behandling.BehandlingService
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
import no.nav.familie.ef.sak.vilkår.regler.hentVilkårsregel
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class VurderingStegService(
    private val behandlingService: BehandlingService,
    private val vurderingService: VurderingService,
    private val vilkårsvurderingRepository: VilkårsvurderingRepository,
    private val blankettRepository: BlankettRepository,
    private val tilordnetRessursService: TilordnetRessursService,
    private val behandlingStegOppdaterer: BehandlingStegOppdaterer,
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
        behandlingStegOppdaterer.oppdaterStegOgKategoriPåBehandling(vilkårsvurdering.behandlingId)
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
        behandlingStegOppdaterer.oppdaterStegOgKategoriPåBehandling(behandlingId)
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
        behandlingStegOppdaterer.oppdaterStegOgKategoriPåBehandling(behandlingId)
        return oppdatertVilkår
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
