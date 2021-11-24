package no.nav.familie.ef.sak.vilkår.dto

data class TidligereVedtaksperioderDto(val infotrygd: TidligereInnvilgetVedtakDto?)

data class TidligereInnvilgetVedtakDto(val harTidligereOvergangsstønad: Boolean,
                                       val harTidligereBarnetilsyn: Boolean,
                                       val harTidligereSkolepenger: Boolean)