package no.nav.familie.ef.sak.økonomi

import no.nav.familie.ef.sak.repository.domain.Stønadstype

fun Stønadstype.tilKlassifisering() = when (this) {
    Stønadstype.OVERGANGSSTØNAD -> "EFOG"
    Stønadstype.BARNETILSYN -> "EFBT"
    Stønadstype.SKOLEPENGER -> "EFSP"
}

fun String.tilStønadsType() = Stønadstype.values().firstOrNull { it.tilKlassifisering() == this }
                             ?: error("Finner ikke YtelseType med klassifisering '$this'")
