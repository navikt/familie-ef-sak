package no.nav.familie.ef.sak.opplysninger.personopplysninger

import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType
import no.nav.familie.kontrakter.felles.Periode
import java.time.LocalDate

data class GrunnlagsdataPeriodeHistorikk(
    val periodeType: VedtaksperiodeType?,
    val periode: Periode<LocalDate>,
    val harUtbetaling: Boolean,
)
