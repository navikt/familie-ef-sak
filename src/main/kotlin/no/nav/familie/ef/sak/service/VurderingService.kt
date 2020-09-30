package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.api.dto.Aleneomsorg
import no.nav.familie.ef.sak.api.dto.InngangsvilkårDto
import no.nav.familie.ef.sak.api.dto.VurderingDto
import no.nav.familie.ef.sak.integration.PdlClient
import no.nav.familie.ef.sak.integration.dto.pdl.Familierelasjonsrolle
import no.nav.familie.ef.sak.mapper.AleneomsorgMapper
import no.nav.familie.ef.sak.mapper.MedlemskapMapper
import no.nav.familie.ef.sak.repository.CustomRepository
import no.nav.familie.ef.sak.repository.VilkårVurderingRepository
import no.nav.familie.ef.sak.repository.domain.SakMapper
import no.nav.familie.ef.sak.repository.domain.VilkårType
import no.nav.familie.ef.sak.repository.domain.VilkårVurdering
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.*

@Service
class VurderingService(private val behandlingService: BehandlingService,
                       private val pdlClient: PdlClient,
                       private val customRepository: CustomRepository,
                       private val vilkårVurderingRepository: VilkårVurderingRepository,
                       private val medlemskapMapper: MedlemskapMapper) {

    fun hentInngangsvilkår(behandlingId: UUID): InngangsvilkårDto {
        val søknad = behandlingService.hentOvergangsstønadPåBehandlingId(behandlingId)
        val fnr = søknad.søknad.personalia.verdi.fødselsnummer.verdi.verdi
        val pdlSøker = pdlClient.hentSøker(fnr)

        val medlemskap = medlemskapMapper.tilDto(medlemskapsdetaljer = søknad.søknad.medlemskapsdetaljer.verdi,
                                                 pdlSøker = pdlSøker)

        val vurderinger = hentEllerOpprettVurderingerForInngangsvilkår(behandlingId)
                .map {
                    VurderingDto(id = it.id,
                                 behandlingId = it.behandlingId,
                                 resultat = it.resultat,
                                 vilkårType = it.type,
                                 begrunnelse = it.begrunnelse,
                                 unntak = it.unntak)
                }
        return InngangsvilkårDto(medlemskap = medlemskap, vurderinger = vurderinger)
    }

    private fun hentEllerOpprettVurderingerForInngangsvilkår(behandlingId: UUID): List<VilkårVurdering> {
        //TODO sjekke att behandlingen allerede ikke er avsluttet? Eks burde vi ikke opprette nye vurderinger hvis noen går inn å sjekker en avsluttet sak..
        val lagredeVilkårVurderinger = vilkårVurderingRepository.findByBehandlingId(behandlingId)
        val nyeVilkårVurderinger = VilkårType.hentInngangsvilkår().filter {
            lagredeVilkårVurderinger.find { vurdering -> vurdering.type == it } == null
        }.map { VilkårVurdering(behandlingId = behandlingId, type = it) }

        customRepository.persistAll(nyeVilkårVurderinger)

        return lagredeVilkårVurderinger + nyeVilkårVurderinger
    }

    fun vurderAleneomsorg(behandlingId: UUID): Aleneomsorg {
        val sak = behandlingService.hentOvergangsstønadPåBehandlingId(behandlingId).soknad
        val fnrSøker = "" //TODO
        val pdlSøker = pdlClient.hentSøker(fnrSøker)


        val barn = pdlSøker.familierelasjoner
                .filter { it.relatertPersonsRolle == Familierelasjonsrolle.BARN }
                .map { it.relatertPersonsIdent }
                .let { pdlClient.hentBarn(it) }
                .filter { it.value.fødsel.firstOrNull()?.fødselsdato != null }
                .filter { it.value.fødsel.first().fødselsdato!!.plusYears(18).isAfter(LocalDate.now()) }

        val overgangsstønad = SakMapper.pakkOppOvergangsstønad(sak)
        val barneforeldreFraSøknad =
                overgangsstønad.søknad.barn.verdi.mapNotNull {
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
                                        overgangsstønad.søknad)
    }


}
