package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.api.gui.dto.Aleneomsorg
import no.nav.familie.ef.sak.api.gui.dto.AleneomsorgDto
import no.nav.familie.ef.sak.api.gui.dto.MedlemskapDto
import no.nav.familie.ef.sak.integration.FamilieIntegrasjonerClient
import no.nav.familie.ef.sak.integration.PdlClient
import no.nav.familie.ef.sak.integration.dto.pdl.Familierelasjonsrolle
import no.nav.familie.ef.sak.mapper.AleneomsorgMapper
import no.nav.familie.ef.sak.mapper.MedlemskapMapper
import no.nav.familie.ef.sak.vurdering.medlemskap.MedlemskapRegelsett
import no.nav.familie.ef.sak.vurdering.medlemskap.Medlemskapsgrunnlag
import no.nav.familie.ef.sak.vurdering.medlemskap.Medlemskapshistorikk
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.*

@Service
class VurderingService(private val sakService: SakService,
                       private val integrasjonerClient: FamilieIntegrasjonerClient,
                       private val pdlClient: PdlClient,
                       private val medlemskapRegelsett: MedlemskapRegelsett) {

    fun vurderMedlemskap(sakId: UUID): MedlemskapDto {
        val sak = sakService.hentSak(sakId)
        val fnr = sak.søknad.personalia.verdi.fødselsnummer.verdi.verdi
        val pdlSøker = pdlClient.hentSøker(fnr)
        val medlemskapsinfo = integrasjonerClient.hentMedlemskapsinfo(fnr)
        val medlemskapshistorikk = Medlemskapshistorikk(pdlSøker, medlemskapsinfo)
        val medlemskapsgrunnlag = Medlemskapsgrunnlag(pdlSøker,
                                                      medlemskapshistorikk,
                                                      sak.søknad)
        val evaluering = medlemskapRegelsett.vurderingMedlemskapSøker.evaluer(medlemskapsgrunnlag)

        return MedlemskapMapper.tilDto(evaluering,
                                       pdlSøker,
                                       medlemskapshistorikk)
    }

    fun vurderAleneomsorg(sakId: UUID): Aleneomsorg {
        val sak = sakService.hentSak(sakId)
        val fnrSøker = sak.søknad.personalia.verdi.fødselsnummer.verdi.verdi
        val pdlSøker = pdlClient.hentSøker(fnrSøker)


        val barn = pdlSøker.familierelasjoner
                .asSequence()
                .filter { it.relatertPersonsRolle == Familierelasjonsrolle.BARN }
                .associateBy({ it.relatertPersonsIdent }, { pdlClient.hentBarn(it.relatertPersonsIdent) })
                .filter { it.value.fødsel.firstOrNull()?.fødselsdato != null }
                .filter { it.value.fødsel.first().fødselsdato!!.plusYears(18).isAfter(LocalDate.now()) }

        val barneforeldre = barn.map { it.value.familierelasjoner }
                .flatten()
                .filter { it.relatertPersonsIdent != fnrSøker && it.relatertPersonsRolle  }
                .associateBy({ it.relatertPersonsIdent }, { pdlClient.hentForelder2(it.relatertPersonsIdent) })

        return AleneomsorgMapper.tilDto(pdlSøker,
                                        barn,
                                        barneforeldre,
                                        sak.søknad)
    }


}
