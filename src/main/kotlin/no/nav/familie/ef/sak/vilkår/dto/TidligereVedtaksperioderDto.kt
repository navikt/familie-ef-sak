package no.nav.familie.ef.sak.vilkår.dto

import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataPeriodeHistorikk
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.TidligereInnvilgetVedtak
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.TidligereVedtaksperioder
import no.nav.familie.kontrakter.felles.Månedsperiode
import java.time.LocalDate

data class TidligereVedtaksperioderDto(
    val infotrygd: TidligereInnvilgetVedtakDto?,
    val sak: TidligereInnvilgetVedtakDto?,
    val historiskPensjon: Boolean?,
) {
    fun harTidligereVedtaksperioder() =
        infotrygd?.harTidligereInnvilgetVedtak() ?: false || sak?.harTidligereInnvilgetVedtak() ?: false || historiskPensjon ?: true
}

data class TidligereInnvilgetVedtakDto(
    val harTidligereOvergangsstønad: Boolean,
    val harTidligereBarnetilsyn: Boolean,
    val harTidligereSkolepenger: Boolean,
    val periodeHistorikkOvergangsstønad: List<GrunnlagsdataPeriodeHistorikkDto> = emptyList(),
) {
    fun harTidligereInnvilgetVedtak() =
        harTidligereOvergangsstønad || harTidligereBarnetilsyn || harTidligereSkolepenger
}

data class GrunnlagsdataPeriodeHistorikkDto(
    val periodeType: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val antMnd: Long = Månedsperiode(fom, tom).lengdeIHeleMåneder(),
    val harPeriodeUtenUtbetaling: Boolean,
)

fun TidligereVedtaksperioder?.tilDto(): TidligereVedtaksperioderDto = this?.let {
    TidligereVedtaksperioderDto(
        infotrygd = it.infotrygd.tilDto(),
        sak = it.sak?.tilDto(),
        historiskPensjon = it.historiskPensjon,
    )
} ?: TidligereVedtaksperioderDto(null, null, null)

fun TidligereInnvilgetVedtak.tilDto() =
    TidligereInnvilgetVedtakDto(
        harTidligereOvergangsstønad = this.harTidligereOvergangsstønad,
        harTidligereBarnetilsyn = this.harTidligereBarnetilsyn,
        harTidligereSkolepenger = this.harTidligereSkolepenger,
        periodeHistorikkOvergangsstønad = this.periodeHistorikkOvergangsstønad.tilDto(),
    )

fun List<GrunnlagsdataPeriodeHistorikk>.tilDto() = this.map { it.tilDto() }
private fun GrunnlagsdataPeriodeHistorikk.tilDto() = GrunnlagsdataPeriodeHistorikkDto(
    periodeType = finnType(this),
    fom = this.fom,
    tom = this.tom,
    harPeriodeUtenUtbetaling = this.harPeriodeUtenUtbetaling
)

fun finnType(grunnlagsdataPeriodeHistorikk: GrunnlagsdataPeriodeHistorikk): String {
    if (grunnlagsdataPeriodeHistorikk.periodeType != null) {
        return grunnlagsdataPeriodeHistorikk.periodeType.toString()
    }
    return "UKJENT"// TODO throw exception?
}
