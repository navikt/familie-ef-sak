package no.nav.familie.ef.sak.vilkår.dto

data class TidligereVedtaksperioderDto(val infotrygd: FinnesTidligereVedtaksperioder?)

data class FinnesTidligereVedtaksperioder(val overgangsstønad: Boolean,
                                          val barnetilsyn: Boolean,
                                          val skolepenger: Boolean)