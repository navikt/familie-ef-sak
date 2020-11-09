package no.nav.familie.ef.sak.økonomi

import no.nav.familie.ef.sak.repository.domain.Stønadstype
import no.nav.familie.ef.sak.repository.domain.YtelseType

fun YtelseType.tilKlassifisering() = when(this) {
    YtelseType.OVERGANGSSTØNAD -> "EFOG"
}

fun String.tilYtelseType() = YtelseType.values()
        .filter { it.tilKlassifisering()==this }
        .firstOrNull() ?: error("Finner ikke YtelseType med klassifisering '$this'")


fun Stønadstype.tilYtelseType() = when(this) {
    Stønadstype.OVERGANGSSTØNAD -> YtelseType.OVERGANGSSTØNAD
    Stønadstype.BARNETILSYN -> TODO()
    Stønadstype.SKOLEPENGER -> TODO()
}