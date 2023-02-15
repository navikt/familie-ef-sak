package no.nav.familie.ef.sak.vilkår.dto

import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.TidligereInnvilgetVedtak
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.TidligereVedtaksperioder

data class TidligereVedtaksperioderDto(
    val infotrygd: TidligereInnvilgetVedtakDto?,
    val sak: TidligereInnvilgetVedtakDto?,
    val historiskPensjon: Boolean?,
)

data class TidligereInnvilgetVedtakDto(
    val harTidligereOvergangsstønad: Boolean,
    val harTidligereBarnetilsyn: Boolean,
    val harTidligereSkolepenger: Boolean,
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
    )
