package no.nav.familie.ef.sak.no.nav.familie.ef.sak.økonomi

import no.nav.familie.ef.sak.repository.domain.AndelTilkjentYtelse
import no.nav.familie.ef.sak.repository.domain.TilkjentYtelse
import no.nav.familie.ef.sak.repository.domain.TilkjentYtelseType
import java.time.LocalDate
import java.util.UUID

fun lagTilkjentYtelse(andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
                      id: UUID = UUID.randomUUID(),
                      behandlingId: UUID = UUID.randomUUID(),
                      personident: String = "123",
                      vedtaksdato: LocalDate? = null,
                      type: TilkjentYtelseType = TilkjentYtelseType.FØRSTEGANGSBEHANDLING) =
        TilkjentYtelse(id = id,
                       behandlingId = behandlingId,
                       personident = personident,
                       vedtaksdato = vedtaksdato,
                       type = type,
                       andelerTilkjentYtelse = andelerTilkjentYtelse)

fun lagAndelTilkjentYtelse(beløp: Int,
                           fraOgMed: LocalDate,
                           tilOgMed: LocalDate,
                           personIdent: String = "123",
        //periodetype: Periodetype = Periodetype.MÅNED,
                           kildeBehandlingId: UUID = UUID.randomUUID(),
                           inntekt: Int = 0,
                           samordningsfradrag: Int = 0,
                           inntektsreduksjon: Int = 0) =
        AndelTilkjentYtelse(beløp = beløp,
                            stønadFom = fraOgMed,
                            stønadTom = tilOgMed,
                            personIdent = personIdent,
                //periodetype = periodetype,
                            inntekt = inntekt,
                            samordningsfradrag = samordningsfradrag,
                            inntektsreduksjon = inntektsreduksjon,
                            kildeBehandlingId = kildeBehandlingId)