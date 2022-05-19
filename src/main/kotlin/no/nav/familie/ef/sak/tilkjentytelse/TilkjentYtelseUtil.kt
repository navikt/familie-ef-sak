package no.nav.familie.ef.sak.tilkjentytelse

import no.nav.familie.kontrakter.ef.iverksett.Periodetype
import no.nav.familie.kontrakter.felles.ef.StønadType

object TilkjentYtelseUtil {

    fun StønadType.tilPeriodeType(): Periodetype =
            if (this == StønadType.SKOLEPENGER) Periodetype.ENGANGSUTBETALING else Periodetype.MÅNED
}