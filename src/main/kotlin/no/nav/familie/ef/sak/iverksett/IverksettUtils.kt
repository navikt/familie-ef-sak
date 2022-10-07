package no.nav.familie.ef.sak.iverksett

import no.nav.familie.ef.sak.brev.domain.BrevmottakerOrganisasjon
import no.nav.familie.ef.sak.brev.domain.BrevmottakerPerson
import no.nav.familie.ef.sak.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.familie.kontrakter.ef.iverksett.AndelTilkjentYtelseDto
import no.nav.familie.kontrakter.ef.iverksett.Brevmottaker
import no.nav.familie.kontrakter.ef.iverksett.Brevmottaker.IdentType

fun AndelTilkjentYtelse.tilIverksettDto() =
    AndelTilkjentYtelseDto(
        beløp = this.beløp,
        inntekt = this.inntekt,
        inntektsreduksjon = this.inntektsreduksjon,
        samordningsfradrag = this.samordningsfradrag,
        fraOgMed = this.stønadFom,
        tilOgMed = this.stønadTom,
        periode = this.periode,
        kildeBehandlingId = this.kildeBehandlingId
    )

fun BrevmottakerPerson.tilIverksettDto() =
    Brevmottaker(
        ident = this.personIdent,
        navn = this.navn,
        mottakerRolle = this.mottakerRolle.tilIverksettDto(),
        identType = IdentType.PERSONIDENT
    )

fun BrevmottakerOrganisasjon.tilIverksettDto() =
    Brevmottaker(
        ident = this.organisasjonsnummer,
        navn = this.navnHosOrganisasjon,
        mottakerRolle = this.mottakerRolle.tilIverksettDto(),
        identType = IdentType.ORGANISASJONSNUMMER
    )
