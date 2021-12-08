package no.nav.familie.ef.sak.vilkår

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.felles.domain.Sporbar
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataService
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadService
import no.nav.familie.ef.sak.vilkår.dto.VilkårDto
import no.nav.familie.ef.sak.vilkår.dto.VilkårGrunnlagDto
import no.nav.familie.ef.sak.vilkår.dto.VilkårsvurderingDto
import no.nav.familie.ef.sak.vilkår.dto.tilDto
import no.nav.familie.ef.sak.vilkår.regler.HovedregelMetadata
import no.nav.familie.ef.sak.vilkår.regler.evalutation.OppdaterVilkår
import no.nav.familie.ef.sak.vilkår.regler.evalutation.OppdaterVilkår.opprettNyeVilkårsvurderinger
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class VurderingService(private val behandlingService: BehandlingService,
                       private val søknadService: SøknadService,
                       private val vilkårsvurderingRepository: VilkårsvurderingRepository,
                       private val vilkårGrunnlagService: VilkårGrunnlagService,
                       private val grunnlagsdataService: GrunnlagsdataService) {

    @Transactional
    fun hentEllerOpprettVurderinger(behandlingId: UUID): VilkårDto {
        val (grunnlag, metadata) = hentGrunnlagOgMetadata(behandlingId)
        val vurderinger = hentEllerOpprettVurderinger(behandlingId, metadata)
        return VilkårDto(vurderinger = vurderinger, grunnlag = grunnlag)
    }

    @Transactional
    fun oppdaterGrunnlagsdataOgHentEllerOpprettVurderinger(behandlingId: UUID): VilkårDto {
        grunnlagsdataService.oppdaterOgHentNyGrunnlagsdata(behandlingId)
        return hentEllerOpprettVurderinger(behandlingId)
    }

    private fun hentGrunnlagOgMetadata(behandlingId: UUID): Pair<VilkårGrunnlagDto, HovedregelMetadata> {
        val søknad = søknadService.hentOvergangsstønad(behandlingId)
        val grunnlag = vilkårGrunnlagService.hentGrunnlag(behandlingId, søknad)
        val metadata = HovedregelMetadata(sivilstandstype = grunnlag.sivilstand.registergrunnlag.type,
                                          søknad = søknad)
        return Pair(grunnlag, metadata)
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

    private fun behandlingErLåstForVidereRedigering(behandlingId: UUID) =
            behandlingService.hentBehandling(behandlingId).status.behandlingErLåstForVidereRedigering()

    fun kopierVurderingerTilNyBehandling(eksisterendeBehandlingId: UUID, nyBehandlingsId: UUID) {
        val vurderinger = vilkårsvurderingRepository.findByBehandlingId(eksisterendeBehandlingId)
        if (vurderinger.isEmpty()) {
            val melding = "Tidligere behandling=$eksisterendeBehandlingId har ikke noen vilkår"
            throw Feil(melding, melding)
        }
        val vurderingerKopi: List<Vilkårsvurdering> = vurderinger.map {
            it.copy(id = UUID.randomUUID(), behandlingId = nyBehandlingsId, sporbar = Sporbar())
        }
        vilkårsvurderingRepository.insertAll(vurderingerKopi)
    }

    fun erAlleVilkårOppfylt(behandlingId: UUID): Boolean {
        val lagredeVilkårsvurderinger: List<Vilkårsvurdering> = vilkårsvurderingRepository.findByBehandlingId(behandlingId)
        return OppdaterVilkår.erAlleVilkårsvurderingerOppfylt(lagredeVilkårsvurderinger)
    }

}