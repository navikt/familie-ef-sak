package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.api.Feil
import no.nav.familie.ef.sak.api.dto.Aleneomsorg
import no.nav.familie.ef.sak.api.dto.DelvilkårVurderingDto
import no.nav.familie.ef.sak.api.dto.InngangsvilkårDto
import no.nav.familie.ef.sak.api.dto.VilkårVurderingDto
import no.nav.familie.ef.sak.integration.PdlClient
import no.nav.familie.ef.sak.integration.dto.pdl.Familierelasjonsrolle
import no.nav.familie.ef.sak.mapper.AleneomsorgMapper
import no.nav.familie.ef.sak.mapper.MedlemskapMapper
import no.nav.familie.ef.sak.repository.VilkårVurderingRepository
import no.nav.familie.ef.sak.repository.domain.*
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.*

@Service
class VurderingService(private val behandlingService: BehandlingService,
                       private val pdlClient: PdlClient,
                       private val vilkårVurderingRepository: VilkårVurderingRepository,
                       private val medlemskapMapper: MedlemskapMapper) {

    fun oppdaterVilkår(vilkårVurderingDto: VilkårVurderingDto): UUID {
        val vilkårVurdering = vilkårVurderingRepository.findByIdOrThrow(vilkårVurderingDto.id)

        val behandlingId = vilkårVurdering.behandlingId
        if (behandlingErLåstForVidereRedigering(behandlingId)) {
            throw Feil(message = "Bruker prøver å oppdatere en vilkårsvurdering der behandling=$behandlingId er låst for videre redigering",
                       frontendFeilmelding = "Behandlingen er låst for videre redigering")
        }

        validerDelvilkår(vilkårVurderingDto, vilkårVurdering)

        val nyVilkårsVurdering = vilkårVurdering.copy(resultat = vilkårVurderingDto.resultat,
                                                      begrunnelse = vilkårVurderingDto.begrunnelse,
                                                      unntak = vilkårVurderingDto.unntak,
                                                      delvilkårVurdering = DelvilkårVurderingWrapper(vilkårVurderingDto.delvilkårVurderinger.map { delvurdering ->
                                                          DelvilkårVurdering(delvurdering.type,
                                                                             delvurdering.resultat)
                                                      })
        )
        return vilkårVurderingRepository.update(nyVilkårsVurdering).id
    }

    private fun validerDelvilkår(vurdering: VilkårVurderingDto,
                                 vilkårVurdering: VilkårVurdering) {
        val innkommendeDelvurderinger = vurdering.delvilkårVurderinger.map { it.type }.toSet()
        val lagredeDelvurderinger = vilkårVurdering.delvilkårVurdering.delvilkårVurderinger.map { it.type }.toSet()

        if (innkommendeDelvurderinger.size != lagredeDelvurderinger.size
            || !innkommendeDelvurderinger.containsAll(lagredeDelvurderinger)) {
            error("Delvilkårstyper motsvarer ikke de som finnes lagrede på vilkåret")
        }
    }

    fun hentInngangsvilkår(behandlingId: UUID): InngangsvilkårDto {
        val søknad = behandlingService.hentOvergangsstønad(behandlingId)
        val fnr = søknad.personalia.verdi.fødselsnummer.verdi.verdi
        val pdlSøker = pdlClient.hentSøker(fnr)

        val medlemskap = medlemskapMapper.tilDto(medlemskapsdetaljer = søknad.medlemskapsdetaljer.verdi,
                                                 pdlSøker = pdlSøker)

        val vurderinger = hentEllerOpprettVurderingerForInngangsvilkår(behandlingId)
                .map {
                    VilkårVurderingDto(id = it.id,
                                       behandlingId = it.behandlingId,
                                       resultat = it.resultat,
                                       vilkårType = it.type,
                                       begrunnelse = it.begrunnelse,
                                       unntak = it.unntak,
                                       endretAv = it.sporbar.endret.endretAv,
                                       endretTid = it.sporbar.endret.endretTid,
                                       delvilkårVurderinger = it.delvilkårVurdering.delvilkårVurderinger.map { delvurdering ->
                                     DelvilkårVurderingDto(delvurdering.type,
                                                           delvurdering.resultat)
                                 })
                }
        return InngangsvilkårDto(medlemskap = medlemskap, vurderinger = vurderinger)
    }

    private fun hentEllerOpprettVurderingerForInngangsvilkår(behandlingId: UUID): List<VilkårVurdering> {
        val lagredeVilkårVurderinger = vilkårVurderingRepository.findByBehandlingId(behandlingId)

        if (behandlingErLåstForVidereRedigering(behandlingId)) {
            return lagredeVilkårVurderinger
        }

        val nyeVilkårVurderinger = VilkårType.hentInngangsvilkår()
                .filter {
                    lagredeVilkårVurderinger.find { vurdering -> vurdering.type == it } == null
                }
                .map {
                    VilkårVurdering(behandlingId = behandlingId,
                                    type = it,
                                    delvilkårVurdering = DelvilkårVurderingWrapper(it.delvilkår.map { delvilkårType -> DelvilkårVurdering(delvilkårType) }))
                }

        vilkårVurderingRepository.insertAll(nyeVilkårVurderinger)

        return lagredeVilkårVurderinger + nyeVilkårVurderinger
    }

    fun hentInngangsvilkårSomManglerVurdering(behandlingId: UUID): List<VilkårType> {
        val lagredeVilkårVurderinger = vilkårVurderingRepository.findByBehandlingId(behandlingId)
        val inngangsvilkår = VilkårType.hentInngangsvilkår()

        return inngangsvilkår.filter {
            lagredeVilkårVurderinger.any { vurdering -> vurdering.type == it && vurdering.resultat == VilkårResultat.IKKE_VURDERT }
            || lagredeVilkårVurderinger.none { vurdering -> vurdering.type == it }
        }
    }

    private fun behandlingErLåstForVidereRedigering(behandlingId: UUID) =
            behandlingService.hentBehandling(behandlingId).status.behandlingErLåstForVidereRedigering()

    fun vurderAleneomsorg(behandlingId: UUID): Aleneomsorg {
        val søknad = behandlingService.hentOvergangsstønad(behandlingId)
        val fnrSøker = "" //TODO
        val pdlSøker = pdlClient.hentSøker(fnrSøker)


        val barn = pdlSøker.familierelasjoner
                .filter { it.relatertPersonsRolle == Familierelasjonsrolle.BARN }
                .map { it.relatertPersonsIdent }
                .let { pdlClient.hentBarn(it) }
                .filter { it.value.fødsel.firstOrNull()?.fødselsdato != null }
                .filter { it.value.fødsel.first().fødselsdato!!.plusYears(18).isAfter(LocalDate.now()) }

        val barneforeldreFraSøknad =
                søknad.barn.verdi.mapNotNull {
                    it.annenForelder?.verdi?.person?.verdi?.fødselsnummer?.verdi?.verdi
                }

        val barneforeldre = barn.map { it.value.familierelasjoner }
                .flatten()
                .filter { it.relatertPersonsIdent != fnrSøker && it.relatertPersonsRolle != Familierelasjonsrolle.BARN }
                .map { it.relatertPersonsIdent }
                .plus(barneforeldreFraSøknad)
                .distinct()
                .let { pdlClient.hentAndreForeldre(it) }

        return AleneomsorgMapper.tilDto(pdlSøker,
                                        barn,
                                        barneforeldre,
                                        søknad)
    }


}
