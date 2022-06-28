package no.nav.familie.ef.sak.vilkår.dto

import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.TidligereInnvilgetVedtak
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.TidligereVedtaksperioder
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.TidligereVedtaksperioderAnnenForelder

data class TidligereVedtaksperioderDto(val infotrygd: TidligereInnvilgetVedtakDto?)

data class TidligereVedtaksperioderAnnenForelderDto(
    val efSak: TidligereInnvilgetVedtakDto,
    val infotrygd: TidligereInnvilgetVedtakDto
)

data class TidligereInnvilgetVedtakDto(
    val harTidligereOvergangsstønad: Boolean,
    val harTidligereBarnetilsyn: Boolean,
    val harTidligereSkolepenger: Boolean
)

fun TidligereVedtaksperioder?.tilDto() =
    TidligereVedtaksperioderDto(infotrygd = this?.infotrygd?.tilDto())

fun TidligereVedtaksperioderAnnenForelder?.tilDto() = this?.let {
    TidligereVedtaksperioderAnnenForelderDto(
        efSak = it.efSak.tilDto(),
        infotrygd = it.infotrygd.tilDto()
    )
}

fun TidligereInnvilgetVedtak.tilDto() =
    TidligereInnvilgetVedtakDto(
        harTidligereOvergangsstønad = this.harTidligereOvergangsstønad,
        harTidligereBarnetilsyn = this.harTidligereBarnetilsyn,
        harTidligereSkolepenger = this.harTidligereSkolepenger
    )
