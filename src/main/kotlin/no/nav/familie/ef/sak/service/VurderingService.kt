package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.api.dto.Aleneomsorg
import no.nav.familie.ef.sak.api.dto.MedlemskapDto
import no.nav.familie.ef.sak.integration.FamilieIntegrasjonerClient
import no.nav.familie.ef.sak.integration.PdlClient
import no.nav.familie.ef.sak.integration.dto.pdl.Familierelasjonsrolle
import no.nav.familie.ef.sak.mapper.AleneomsorgMapper
import no.nav.familie.ef.sak.mapper.MedlemskapMapper
import no.nav.familie.ef.sak.repository.domain.SakMapper
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.*

@Service
class VurderingService(private val sakService: SakService,
                       private val integrasjonerClient: FamilieIntegrasjonerClient,
                       private val pdlClient: PdlClient,
                       private val medlemskapMapper: MedlemskapMapper) {

    fun vurderMedlemskap(sakId: UUID): MedlemskapDto {
        val sak = sakService.hentOvergangsstønad(sakId)
        val fnr = sak.søknad.personalia.verdi.fødselsnummer.verdi.verdi
        val pdlSøker = pdlClient.hentSøker(fnr)
        val medlemskapsinfo = integrasjonerClient.hentMedlemskapsinfo(fnr)

        return medlemskapMapper.tilDto(medlemskapsdetaljer = sak.søknad.medlemskapsdetaljer.verdi,
                                       pdlSøker = pdlSøker,
                                       medlemskapsinfo = medlemskapsinfo)
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
