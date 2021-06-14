package no.nav.familie.ef.sak.no.nav.familie.ef.sak.økonomi

import no.nav.familie.ef.sak.repository.domain.AndelTilkjentYtelse
import no.nav.familie.ef.sak.repository.domain.TilkjentYtelse
import no.nav.familie.ef.sak.repository.domain.TilkjentYtelseStatus
import no.nav.familie.ef.sak.repository.domain.TilkjentYtelseType
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import java.time.LocalDate
import java.util.UUID

fun lagTilkjentYtelse(andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
                      id: UUID = UUID.randomUUID(),
                      behandlingId: UUID = UUID.randomUUID(),
                      personident: String = "123",
                      utbetalingsoppdrag: Utbetalingsoppdrag? = null,
                      vedtaksdato: LocalDate? = null,
                      status: TilkjentYtelseStatus = TilkjentYtelseStatus.IKKE_KLAR,
                      type: TilkjentYtelseType = TilkjentYtelseType.FØRSTEGANGSBEHANDLING) =
        TilkjentYtelse(id = id,
                       behandlingId = behandlingId,
                       personident = personident,
                       utbetalingsoppdrag = utbetalingsoppdrag,
                       vedtaksdato = vedtaksdato,
                       status = status,
                       type = type,
                       andelerTilkjentYtelse = andelerTilkjentYtelse)

fun lagAndelTilkjentYtelse(beløp: Int,
                           fraOgMed: LocalDate,
                           tilOgMed: LocalDate,
                           personIdent: String = "123",
        //periodetype: Periodetype = Periodetype.MÅNED,
                           periodeId: Long? = null,
                           forrigePeriodeId: Long? = null,
                           kildeBehandlingId: UUID? = UUID.randomUUID(),
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
                            periodeId = periodeId,
                            forrigePeriodeId = forrigePeriodeId,
                            kildeBehandlingId = kildeBehandlingId)