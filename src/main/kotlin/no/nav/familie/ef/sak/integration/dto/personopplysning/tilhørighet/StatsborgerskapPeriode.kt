package no.nav.familie.ef.sak.integration.dto.personopplysning.tilhørighet

import no.nav.familie.ef.sak.integration.dto.personopplysning.Periode

data class StatsborgerskapPeriode(val periode: Periode,
                                  val tilhørendeLand: Landkode)

