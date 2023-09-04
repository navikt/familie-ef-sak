package no.nav.familie.ef.sak.opplysninger.personopplysninger

import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType
import no.nav.familie.kontrakter.felles.Månedsperiode

data class GrunnlagsdataPeriodeHistorikk(
    val periodeType: VedtaksperiodeType?,
    val periode: Månedsperiode,
    val harUtbetaling: Boolean,
)






