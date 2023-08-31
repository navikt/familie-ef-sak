package no.nav.familie.ef.sak.vilkår.dto

import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataPeriodeHistorikk
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.TidligereInnvilgetVedtak
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.TidligereVedtaksperioder
import no.nav.familie.kontrakter.felles.Månedsperiode

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
    fun harTidligereInnvilgetVedtak() = harTidligereOvergangsstønad || harTidligereBarnetilsyn || harTidligereSkolepenger
}

data class GrunnlagsdataPeriodeHistorikkDto(
    val periodeType: String,
    val periode: Månedsperiode,
    val antMnd: Long = periode.lengdeIHeleMåneder(),
    val harUtbetaling: Boolean
) //

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
private fun GrunnlagsdataPeriodeHistorikk.tilDto() = GrunnlagsdataPeriodeHistorikkDto(periodeType = finnType(this), periode = this.periode, harUtbetaling = this.harUtbetaling)

fun finnType(grunnlagsdataPeriodeHistorikk: GrunnlagsdataPeriodeHistorikk): String {
    if(grunnlagsdataPeriodeHistorikk.periodeType != null){
        return grunnlagsdataPeriodeHistorikk.periodeType.toString()
    }
    if(grunnlagsdataPeriodeHistorikk.erOpphør){
        return "OPPHØR"
    }
    return "UKJENT"
}