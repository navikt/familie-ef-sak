package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.api.gui.dto.MedlemskapDto
import no.nav.familie.ef.sak.integration.FamilieIntegrasjonerClient
import no.nav.familie.ef.sak.integration.PdlClient
import no.nav.familie.ef.sak.mapper.MedlemskapMapper
import no.nav.familie.ef.sak.vurdering.medlemskap.MedlemskapRegelsett
import no.nav.familie.ef.sak.vurdering.medlemskap.Medlemskapsgrunnlag
import no.nav.familie.ef.sak.vurdering.medlemskap.Medlemskapshistorikk
import org.springframework.stereotype.Service
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


}
