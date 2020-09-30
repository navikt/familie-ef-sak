package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.api.dto.Aleneomsorg
import no.nav.familie.ef.sak.api.dto.InngangsvilkårDto
import no.nav.familie.ef.sak.integration.FamilieIntegrasjonerClient
import no.nav.familie.ef.sak.integration.PdlClient
import no.nav.familie.ef.sak.integration.dto.pdl.Familierelasjonsrolle
import no.nav.familie.ef.sak.mapper.AleneomsorgMapper
import no.nav.familie.ef.sak.mapper.MedlemskapMapper
import no.nav.familie.ef.sak.repository.VilkårVurderingRepository
import no.nav.familie.ef.sak.repository.domain.Behandling
import no.nav.familie.ef.sak.repository.domain.SakMapper
import no.nav.familie.ef.sak.repository.domain.VilkårType
import no.nav.familie.ef.sak.repository.domain.VilkårVurdering
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.*

@Service
class VurderingService(private val sakService: SakService,
                       private val pdlClient: PdlClient,
                       private val vilkårVurderingRepository: VilkårVurderingRepository,
                       private val medlemskapMapper: MedlemskapMapper) {

    //TODO denne må opprette vurderinger dersom det ikke finnes fra før på behandling. Dersom det finnes vurdering, hent denne.
    fun hentInngangsvilkår(sakId: UUID): InngangsvilkårDto {
        val sak = sakService.hentOvergangsstønad(sakId)
        val fnr = sak.søknad.personalia.verdi.fødselsnummer.verdi.verdi
        val pdlSøker = pdlClient.hentSøker(fnr)

        val medlemskap = medlemskapMapper.tilDto(medlemskapsdetaljer = sak.søknad.medlemskapsdetaljer.verdi,
                                                 pdlSøker = pdlSøker)

        //TODO: Dette må kobles til behandling
        val vurderinger = hentEllerOpprettVurderingerForInngangsvilkår()
        return InngangsvilkårDto(medlemskap = medlemskap, vurderinger = emptyList())
    }

    private fun hentEllerOpprettVurderingerForInngangsvilkår(behandling: Behandling): List<VilkårVurdering> {

        val lagredeVilkårVurderinger = vilkårVurderingRepository.findByBehandlingId(behandling.id)
        val nyeVilkårVurderinger = VilkårType.hentInngangsvilkår().filter {
            lagredeVilkårVurderinger.find { vurdering -> vurdering.type == it } == null
        }.map { VilkårVurdering(behandlingId = behandling.id, type = it) }

        vilkårVurderingRepository.saveAll(nyeVilkårVurderinger)

        return lagredeVilkårVurderinger + nyeVilkårVurderinger
    }

    fun vurderAleneomsorg(sakId: UUID): Aleneomsorg {
        val sak = sakService.hentSak(sakId)
        val fnrSøker = sak.søker.fødselsnummer
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
