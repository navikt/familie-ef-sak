package no.nav.familie.ef.sak.økonomi

import no.nav.familie.ef.sak.beregning.Grunnbeløpsperioder
import no.nav.familie.ef.sak.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.familie.ef.sak.tilkjentytelse.domain.TilkjentYtelse
import no.nav.familie.ef.sak.tilkjentytelse.domain.TilkjentYtelseType
import no.nav.familie.kontrakter.felles.Månedsperiode
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

fun lagTilkjentYtelse(
    andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
    id: UUID = UUID.randomUUID(),
    behandlingId: UUID = UUID.randomUUID(),
    personident: String = "123",
    type: TilkjentYtelseType = TilkjentYtelseType.FØRSTEGANGSBEHANDLING,
    startdato: LocalDate = andelerTilkjentYtelse.minOfOrNull { it.stønadFom } ?: LocalDate.now(),
    grunnbeløpsmåned: YearMonth = Grunnbeløpsperioder.nyesteGrunnbeløp.periode.fom,
) = TilkjentYtelse(
    id = id,
    behandlingId = behandlingId,
    personident = personident,
    type = type,
    andelerTilkjentYtelse = andelerTilkjentYtelse,
    startdato = startdato,
    grunnbeløpsmåned = grunnbeløpsmåned,
)

fun lagAndelTilkjentYtelse(
    beløp: Int,
    fraOgMed: LocalDate,
    tilOgMed: LocalDate,
    personIdent: String = "123",
    // periodetype: Periodetype = Periodetype.MÅNED,
    kildeBehandlingId: UUID = UUID.randomUUID(),
    inntekt: Int = 0,
    samordningsfradrag: Int = 0,
    inntektsreduksjon: Int = 0,
) =
    AndelTilkjentYtelse(
        beløp = beløp,
        periode = Månedsperiode(fraOgMed, tilOgMed),
        personIdent = personIdent,
        // periodetype = periodetype,
        inntekt = inntekt,
        samordningsfradrag = samordningsfradrag,
        inntektsreduksjon = inntektsreduksjon,
        kildeBehandlingId = kildeBehandlingId,
    )
