package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.api.Feil
import no.nav.familie.ef.sak.api.dto.*
import no.nav.familie.ef.sak.integration.PdlClient
import no.nav.familie.ef.sak.repository.VilkårsvurderingRepository
import no.nav.familie.ef.sak.repository.domain.DelvilkårMetadata
import no.nav.familie.ef.sak.repository.domain.Delvilkårsvurdering
import no.nav.familie.ef.sak.repository.domain.DelvilkårsvurderingWrapper
import no.nav.familie.ef.sak.repository.domain.VilkårType
import no.nav.familie.ef.sak.repository.domain.Vilkårsresultat
import no.nav.familie.ef.sak.repository.domain.Vilkårsvurdering
import no.nav.familie.ef.sak.repository.domain.søknad.SøknadsskjemaOvergangsstønad
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.ef.sak.vurdering.utledDelvilkårResultat
import no.nav.familie.ef.sak.vurdering.validerDelvilkår
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class VurderingService(private val behandlingService: BehandlingService,
                       private val pdlClient: PdlClient,
                       private val vilkårsvurderingRepository: VilkårsvurderingRepository,
                       private val grunnlagsdataService: GrunnlagsdataService) {

    fun oppdaterVilkår(vilkårsvurderingDto: VilkårsvurderingDto): UUID {
        val vilkårsvurdering = vilkårsvurderingRepository.findByIdOrThrow(vilkårsvurderingDto.id)

        val behandlingId = vilkårsvurdering.behandlingId
        if (behandlingErLåstForVidereRedigering(behandlingId)) {
            throw Feil("Bruker prøver å oppdatere en vilkårsvurdering der behandling=$behandlingId er låst for videre redigering",
                       "Behandlingen er låst for videre redigering")
        }

        validerDelvilkår(vilkårsvurderingDto, vilkårsvurdering)

        val nyVilkårsvurdering =
                vilkårsvurdering.copy(resultat = vilkårsvurderingDto.resultat,
                                      begrunnelse = vilkårsvurderingDto.begrunnelse,
                                      unntak = vilkårsvurderingDto.unntak,
                                      delvilkårsvurdering =
                                      DelvilkårsvurderingWrapper(vilkårsvurderingDto.delvilkårsvurderinger
                                                                         .map { delvurdering ->
                                                                             Delvilkårsvurdering(delvurdering.type,
                                                                                                 delvurdering.resultat,
                                                                                                 delvurdering.årsak,
                                                                                                 delvurdering.begrunnelse)
                                                                         })
                )
        return vilkårsvurderingRepository.update(nyVilkårsvurdering).id
    }

    fun hentInngangsvilkår(behandlingId: UUID): VilkårDto {
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
                .map {
                    VilkårsvurderingDto(id = it.id,
                                        behandlingId = it.behandlingId,
                                        resultat = it.resultat,
                                        vilkårType = it.type,
                                        begrunnelse = it.begrunnelse,
                                        unntak = it.unntak,
                                        barnId = it.barnId,
                                        endretAv = it.sporbar.endret.endretAv,
                                        endretTid = it.sporbar.endret.endretTid,
                                        delvilkårsvurderinger = it.delvilkårsvurdering.delvilkårsvurderinger.map { delvurdering ->
                                            DelvilkårsvurderingDto(delvurdering.type,
                                                                   delvurdering.resultat,
                                                                   delvurdering.årsak,
                                                                   delvurdering.begrunnelse)
                                        })
                }
    }

    private fun hentEllerOpprettVurderingerForVilkår(behandlingId: UUID,
                                                     søknad: SøknadsskjemaOvergangsstønad,
                                                     delvilkårMetadata: DelvilkårMetadata): List<Vilkårsvurdering> {
        val lagredeVilkårsvurderinger = vilkårsvurderingRepository.findByBehandlingId(behandlingId)

        if (behandlingErLåstForVidereRedigering(behandlingId)) {
            return lagredeVilkårsvurderinger
        }

        val nyeVilkårsvurderinger: List<Vilkårsvurdering> = VilkårType.hentVilkår()
                .filter {
                    lagredeVilkårsvurderinger.find { vurdering -> vurdering.type == it } == null // Sjekk barnId ?
                }
                .map {vilkårType ->
                    if (vilkårType == VilkårType.ALENEOMSORG) {
                        søknad.barn.map {
                            lagNyVilkårsvurdering(vilkårType, søknad, delvilkårMetadata, behandlingId, it.id)
                        }

                    } else {
                        listOf(lagNyVilkårsvurdering(vilkårType, søknad, delvilkårMetadata, behandlingId))
                    }
                }.flatten()

        vilkårsvurderingRepository.insertAll(nyeVilkårsvurderinger)

        return lagredeVilkårsvurderinger + nyeVilkårsvurderinger
    }

    private fun lagNyVilkårsvurdering(it: VilkårType,
                                      søknad: SøknadsskjemaOvergangsstønad,
                                      delvilkårMetadata: DelvilkårMetadata,
                                      behandlingId: UUID,
                                      barnId: UUID? = null): Vilkårsvurdering {
        val delvilkårsvurderinger = it.delvilkår
                .map { delvilkårType ->
                    Delvilkårsvurdering(delvilkårType,
                                        utledDelvilkårResultat(delvilkårType, søknad, delvilkårMetadata))
                }
        return Vilkårsvurdering(behandlingId = behandlingId,
                                type = it,
                                barnId = barnId,
                                delvilkårsvurdering = DelvilkårsvurderingWrapper(delvilkårsvurderinger))
    }


    fun hentInngangsvilkårSomManglerVurdering(behandlingId: UUID): List<VilkårType> {
        val lagredeVilkårsvurderinger = vilkårsvurderingRepository.findByBehandlingId(behandlingId)
        val inngangsvilkår = VilkårType.hentVilkår()

        return inngangsvilkår.filter {
            lagredeVilkårsvurderinger.any { vurdering ->
                vurdering.type == it
                && vurdering.resultat == Vilkårsresultat.IKKE_VURDERT
            }
            || lagredeVilkårsvurderinger.none { vurdering -> vurdering.type == it }
        }
    }

    private fun behandlingErLåstForVidereRedigering(behandlingId: UUID) =
            behandlingService.hentBehandling(behandlingId).status.behandlingErLåstForVidereRedigering()

}
