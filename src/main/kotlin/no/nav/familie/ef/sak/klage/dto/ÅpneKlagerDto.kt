package no.nav.familie.ef.sak.klage.dto

data class ÅpneKlagerDto(val infotrygd: ÅpneKlagerInfotrygd)

data class ÅpneKlagerInfotrygd(val overgangsstønad: Boolean, val barnetilsyn: Boolean, val skolepenger: Boolean)
