package no.nav.familie.ef.sak.økonomi

import no.nav.familie.ef.sak.repository.domain.Stønadstype

@Deprecated("Denne er flyttet til EF-sak")
fun Stønadstype.tilKlassifisering() = when (this) {
    Stønadstype.OVERGANGSSTØNAD -> "EFOG"
    Stønadstype.BARNETILSYN -> "EFBT"
    Stønadstype.SKOLEPENGER -> "EFSP"
}

@Deprecated("Denne er flyttet til EF-sak")
fun String.tilStønadsType() = Stønadstype.values().firstOrNull { it.tilKlassifisering() == this }
                             ?: error("Finner ikke Stønadstype med klassifisering '$this'")
