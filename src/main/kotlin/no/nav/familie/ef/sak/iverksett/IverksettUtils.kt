package no.nav.familie.ef.sak.iverksett

import no.nav.familie.ef.sak.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.familie.kontrakter.ef.iverksett.AndelTilkjentYtelseDto

fun AndelTilkjentYtelse.tilIverksettDto() =
    AndelTilkjentYtelseDto(
        beløp = this.beløp,
        inntekt = this.inntekt,
        inntektsreduksjon = this.inntektsreduksjon,
        samordningsfradrag = this.samordningsfradrag,
        fraOgMed = this.stønadFom,
        tilOgMed = this.stønadTom,
        kildeBehandlingId = this.kildeBehandlingId
    )
