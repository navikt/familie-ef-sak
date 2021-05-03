package no.nav.familie.ef.sak.økonomi

import no.nav.familie.ef.sak.dummy.FLYTTET_TIL_EF_IVERKSETT
import no.nav.familie.ef.sak.repository.domain.Stønadstype

@Deprecated(FLYTTET_TIL_EF_IVERKSETT)
fun Stønadstype.tilKlassifisering() = when (this) {
    Stønadstype.OVERGANGSSTØNAD -> "EFOG"
    Stønadstype.BARNETILSYN -> "EFBT"
    Stønadstype.SKOLEPENGER -> "EFSP"
}

@Deprecated(FLYTTET_TIL_EF_IVERKSETT)
fun String.tilStønadsType() = Stønadstype.values().firstOrNull { it.tilKlassifisering() == this }
                             ?: error("Finner ikke Stønadstype med klassifisering '$this'")
