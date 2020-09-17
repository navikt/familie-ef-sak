package no.nav.familie.ef.sak.mapper

import no.nav.familie.ef.sak.api.dto.MedlemskapDto
import no.nav.familie.ef.sak.integration.dto.pdl.PdlSøker
import no.nav.familie.ef.sak.nare.evaluations.Evaluering
import no.nav.familie.ef.sak.vurdering.medlemskap.Medlemskapshistorikk

object MedlemskapMapper {

    fun tilDto(evaluering: Evaluering,
               pdlSøker: PdlSøker,
               medlemskapshistorikk: Medlemskapshistorikk): MedlemskapDto {


        return MedlemskapDto(evaluering.resultat.toString(),
                             evaluering.children.map { it.begrunnelse },
                             pdlSøker.statsborgerskap,
                             pdlSøker.innflyttingTilNorge,
                             pdlSøker.utflyttingFraNorge,
                             medlemskapshistorikk)


    }

}