package no.nav.familie.ef.sak.api.gui.dto

import no.nav.familie.ef.sak.integration.dto.pdl.InnflyttingTilNorge
import no.nav.familie.ef.sak.integration.dto.pdl.Statsborgerskap
import no.nav.familie.ef.sak.integration.dto.pdl.UtflyttingFraNorge
import no.nav.familie.ef.sak.vurdering.medlemskap.Medlemskapshistorikk
import no.nav.familie.ef.sak.nare.evaluations.Evaluering


data class MedlemskapDto(val totalvurdering: Evaluering,
                         val statborgerskap: List<Statsborgerskap>,
                         val innvandring: List<InnflyttingTilNorge>,
                         val utvandring: List<UtflyttingFraNorge>,
                         val medlemskapshistorikk: Medlemskapshistorikk)
