package no.nav.familie.ef.sak.økonomi

import no.nav.familie.ef.sak.repository.domain.YtelseType

fun YtelseType.tilKlassifisering() = when(this) {
    YtelseType.OVERGANGSSTØNAD -> "EFOG"
}

fun String.tilYtelseType() = YtelseType.values()
        .filter { it.tilKlassifisering()==this }
        .firstOrNull() ?: error("Finner ikke YtelseType med klassifisering '$this'")
