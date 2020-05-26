package no.nav.familie.ef.sak.api.gui.dto

import no.nav.familie.ef.sak.integration.dto.pdl.InnflyttingTilNorge
import no.nav.familie.ef.sak.integration.dto.pdl.Statsborgerskap
import no.nav.familie.ef.sak.integration.dto.pdl.UtflyttingFraNorge
import no.nav.familie.ef.sak.vurdering.medlemskap.Medlemskapshistorikk


data class MedlemskapDto(val totalvurdering: String,
                         val delvurderinger: List<String>,
                         val statsborgerskap: List<Statsborgerskap>,
                         val innvandring: List<InnflyttingTilNorge>,
                         val utvandring: List<UtflyttingFraNorge>,
                         val medlemskapshistorikk: Medlemskapshistorikk)
