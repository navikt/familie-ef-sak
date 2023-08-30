package no.nav.familie.ef.sak.vilkår.dto

import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataPeriodeHistorikk
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.TidligereInnvilgetVedtak
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.TidligereVedtaksperioder

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
    val øyeblikksbildeAvPerioderOgPeriodetype: List<GrunnlagsdataPeriodeHistorikk> = emptyList()
) {
    fun harTidligereInnvilgetVedtak() = harTidligereOvergangsstønad || harTidligereBarnetilsyn || harTidligereSkolepenger
}

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
        øyeblikksbildeAvPerioderOgPeriodetype = this.øyeblikksbildeAvPerioderOgPeriodetype
)
