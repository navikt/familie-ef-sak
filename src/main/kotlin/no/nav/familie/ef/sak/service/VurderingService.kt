package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.api.Feil
import no.nav.familie.ef.sak.api.dto.Aleneomsorg
import no.nav.familie.ef.sak.api.dto.InngangsvilkårDto
import no.nav.familie.ef.sak.api.dto.VurderingDto
import no.nav.familie.ef.sak.integration.PdlClient
import no.nav.familie.ef.sak.integration.dto.pdl.Familierelasjonsrolle
import no.nav.familie.ef.sak.mapper.AleneomsorgMapper
import no.nav.familie.ef.sak.mapper.MedlemskapMapper
import no.nav.familie.ef.sak.repository.CustomRepository
import no.nav.familie.ef.sak.repository.VilkårVurderingRepository
import no.nav.familie.ef.sak.repository.domain.VilkårType
import no.nav.familie.ef.sak.repository.domain.VilkårVurdering
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.*

@Service
class VurderingService(private val behandlingService: BehandlingService,
                       private val pdlClient: PdlClient,
                       private val customRepository: CustomRepository,
                       private val vilkårVurderingRepository: VilkårVurderingRepository,
                       private val medlemskapMapper: MedlemskapMapper) {

    fun oppdaterVilkår(vurdering: VurderingDto): UUID {
        val vilkårVurdering = vilkårVurderingRepository.findByIdOrThrow(vurdering.id)

        val behandlingId = vilkårVurdering.behandlingId
        if (behandlingErLåstForVidereRedigering(behandlingId)) {
            throw Feil(message = "Bruker prøver å oppdatere en vilkårsvurdering der behandling=$behandlingId er låst for videre redigering",
                       frontendFeilmelding = "Behandlingen er låst for videre redigering")
        }

        val nyVilkårsVurdering = vilkårVurdering.copy(resultat = vurdering.resultat,
                                                      begrunnelse = vurdering.begrunnelse,
                                                      unntak = vurdering.unntak)
        return vilkårVurderingRepository.save(nyVilkårsVurdering).id
    }

    fun hentInngangsvilkår(behandlingId: UUID): InngangsvilkårDto {
        val søknad = behandlingService.hentOvergangsstønad(behandlingId)
        val fnr = søknad.personalia.verdi.fødselsnummer.verdi.verdi
        val pdlSøker = pdlClient.hentSøker(fnr)

        val medlemskap = medlemskapMapper.tilDto(medlemskapsdetaljer = søknad.medlemskapsdetaljer.verdi,
                                                 pdlSøker = pdlSøker)

        val vurderinger = hentEllerOpprettVurderingerForInngangsvilkår(behandlingId)
                .map {
                    VurderingDto(id = it.id,
                                 behandlingId = it.behandlingId,
                                 resultat = it.resultat,
                                 vilkårType = it.type,
                                 begrunnelse = it.begrunnelse,
                                 unntak = it.unntak,
                                 endretAv = it.sporbar.endret.endretAv,
                                 endretTid = it.sporbar.endret.endretTid)
                }
        return InngangsvilkårDto(medlemskap = medlemskap, vurderinger = vurderinger)
    }

    private fun hentEllerOpprettVurderingerForInngangsvilkår(behandlingId: UUID): List<VilkårVurdering> {
        val lagredeVilkårVurderinger = vilkårVurderingRepository.findByBehandlingId(behandlingId)

        if (behandlingErLåstForVidereRedigering(behandlingId)) {
            return lagredeVilkårVurderinger
        }

        val nyeVilkårVurderinger = VilkårType.hentInngangsvilkår().filter {
            lagredeVilkårVurderinger.find { vurdering -> vurdering.type == it } == null
        }.map { VilkårVurdering(behandlingId = behandlingId, type = it) }

        customRepository.persistAll(nyeVilkårVurderinger)

        return lagredeVilkårVurderinger + nyeVilkårVurderinger
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
