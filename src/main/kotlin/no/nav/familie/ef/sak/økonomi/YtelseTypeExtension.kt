package no.nav.familie.ef.sak.økonomi

import no.nav.familie.ef.sak.repository.domain.Stønadstype
import no.nav.familie.ef.sak.repository.domain.YtelseType

fun YtelseType.tilKlassifisering() = when (this) {
    YtelseType.OVERGANGSSTØNAD -> "EFOG"
}

fun String.tilYtelseType() = YtelseType.values().firstOrNull { it.tilKlassifisering() == this }
                             ?: error("Finner ikke YtelseType med klassifisering '$this'")


fun Stønadstype.tilYtelseType() = when (this) {
    Stønadstype.OVERGANGSSTØNAD -> YtelseType.OVERGANGSSTØNAD
    Stønadstype.BARNETILSYN -> TODO()
    Stønadstype.SKOLEPENGER -> TODO()
}