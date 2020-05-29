package no.nav.familie.ef.sak.økonomi

import no.nav.familie.ef.sak.økonomi.domain.TilkjentYtelse
import no.nav.familie.kontrakter.felles.oppdrag.Opphør
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag.KodeEndring.NY
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag.KodeEndring.UEND
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode.SatsType.MND
import no.nav.fpsak.tidsserie.LocalDateSegment
import java.math.BigDecimal
import java.time.LocalDateTime

object Utbetalingsoppdrag {

    // Må forsikre oss om at tidslinjesegmentene er i samme rekkefølge for å få konsekvent periodeId
    // Sorter etter fraDato, sats, og evt til dato
    // PeriodeId = periodeIdStart + offset
    // Beholder bare siste utbetalingsperiode hvis det er opphør.
    fun lagUtbetalingsoppdrag(saksbehandlerId: String,
                              tilkjentYtelse: TilkjentYtelse): Utbetalingsoppdrag {

        val erOpphør = tilkjentYtelse.opphørFom != null

        val utbetalingsperiodeMal =
                if (erOpphør)
                    UtbetalingsperiodeMal(tilkjentYtelse, true, tilkjentYtelse.forrigePeriodeIdStart!!)
                else
                    UtbetalingsperiodeMal(tilkjentYtelse)

        val tidslinjeMap = beregnUtbetalingsperioder(tilkjentYtelse.andelerTilkjentYtelse)

        val utbetalingsperioder = tidslinjeMap.flatMap { (klassifisering, tidslinje) ->
            tidslinje.toSegments()
                    .sortedWith(compareBy<LocalDateSegment<Int>>({ it.fom }, { it.value }, { it.tom }))
                    .mapIndexed { indeks, segment ->
                        utbetalingsperiodeMal.lagPeriode(klassifisering, segment, indeks)
                    }.kunSisteHvis(erOpphør)
        }

        return Utbetalingsoppdrag(saksbehandlerId = saksbehandlerId,
                                  kodeEndring = if (!erOpphør) NY else UEND,
                                  fagSystem = FAGSYSTEM,
                                  saksnummer = tilkjentYtelse.saksnummer,
                                  aktoer = tilkjentYtelse.personident,
                                  utbetalingsperiode = utbetalingsperioder,
                                  avstemmingTidspunkt = finnAvstemmingTidspunkt())
    }

    private fun <T> List<T>.kunSisteHvis(kunSiste: Boolean): List<T> {
        return this.foldRight(mutableListOf()) { element, resultat ->
            if (resultat.size == 0 || !kunSiste) resultat.add(0, element)

            resultat
        }
    }

    private fun finnAvstemmingTidspunkt(): LocalDateTime = LocalDateTime.now()

    private data class UtbetalingsperiodeMal(val tilkjentYtelse: TilkjentYtelse,
                                             val erEndringPåEksisterendePeriode: Boolean = false,
                                             val periodeIdStart: Long = tilkjentYtelse.periodeIdStart) {

        fun lagPeriode(klassifisering: String, segment: LocalDateSegment<Int>, periodeIdOffset: Int): Utbetalingsperiode {

            return Utbetalingsperiode(erEndringPåEksisterendePeriode,
                                      tilkjentYtelse.opphørFom?.let { Opphør(it) },
                                      periodeIdStart + periodeIdOffset,
                                      if (periodeIdOffset > 0) periodeIdStart + (periodeIdOffset - 1).toLong() else null,
                                      tilkjentYtelse.vedtaksdato!!,
                                      klassifisering,
                                      segment.fom,
                                      segment.tom,
                                      BigDecimal(segment.value),
                                      MND,
                                      tilkjentYtelse.personident,
                                      tilkjentYtelse.periodeIdStart)
        }
    }
}
