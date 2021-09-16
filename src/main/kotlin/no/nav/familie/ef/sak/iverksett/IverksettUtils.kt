package no.nav.familie.ef.sak.iverksett

import no.nav.familie.ef.sak.tilkjentytelse.AndelTilkjentYtelse
import no.nav.familie.kontrakter.ef.iverksett.AndelTilkjentYtelseDto
import no.nav.familie.kontrakter.ef.iverksett.Periodetype

fun AndelTilkjentYtelse.tilIverksettDto() =
        AndelTilkjentYtelseDto(beløp = this.beløp,
                               periodetype = Periodetype.MÅNED,
                               inntekt = this.inntekt,
                               inntektsreduksjon = this.inntektsreduksjon,
                               samordningsfradrag = this.samordningsfradrag,
                               fraOgMed = this.stønadFom,
                               tilOgMed = this.stønadTom,
                               kildeBehandlingId = this.kildeBehandlingId)