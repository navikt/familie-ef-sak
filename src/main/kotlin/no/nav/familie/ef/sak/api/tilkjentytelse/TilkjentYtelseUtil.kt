package no.nav.familie.ef.sak.api.tilkjentytelse

import no.nav.familie.ef.sak.repository.domain.TilkjentYtelse

fun mergeAndeler(nyTilkjentYtelse: TilkjentYtelse, forrigeTilkjentYtelse: TilkjentYtelse): TilkjentYtelse {
    val forrigeAndeler = forrigeTilkjentYtelse.andelerTilkjentYtelse.map {
        it.copy(kildeBehandlingId = nyTilkjentYtelse.behandlingId,
                personIdent = nyTilkjentYtelse.personident) to Pair(it.kildeBehandlingId, it.personIdent)
    }.toMap()

    var harFunnetEndring = false
    val mergetAndeler = nyTilkjentYtelse.andelerTilkjentYtelse.sortedBy { it.stønadFom }.map {
        val forrigeAndel = forrigeAndeler[it]
        if (forrigeAndel != null) {
            if (harFunnetEndring) {
                it
            } else {
                it.copy(kildeBehandlingId = forrigeAndel.first, personIdent = forrigeAndel.second)
            }
        } else {
            harFunnetEndring = true
            it
        }
    }
    return nyTilkjentYtelse.copy(andelerTilkjentYtelse = mergetAndeler)
}