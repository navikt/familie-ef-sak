package no.nav.familie.ef.sak.økonomi

import no.nav.familie.ef.sak.økonomi.domain.AndelTilkjentYtelse
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
    // PeriodeId = Vedtak.id * 1000 + offset
    // Beholder bare siste utbetalingsperiode hvis det er opphør.
    fun lagUtbetalingsoppdrag(saksbehandlerId: String,
                              tilkjentYtelse: TilkjentYtelse,
                              andelerTilkjentYtelse: List<AndelTilkjentYtelse>): Utbetalingsoppdrag {

        val erOpphør = tilkjentYtelse.opphørFom != null

        val utbetalingsperiodeMal =
                if (erOpphør)
                    UtbetalingsperiodeMal(tilkjentYtelse, true, tilkjentYtelse.forrigeTilkjentYtelseId!!)
                else
                    UtbetalingsperiodeMal(tilkjentYtelse)

        val tidslinjeMap = beregnUtbetalingsperioder(andelerTilkjentYtelse)

        val utbetalingsperioder = tidslinjeMap.flatMap { (klassifisering, tidslinje) ->
            tidslinje.toSegments()
                    .sortedWith(compareBy<LocalDateSegment<Int>>({ it.fom }, { it.value }, { it.tom }))
                    .mapIndexed { indeks, segment ->
                        utbetalingsperiodeMal.lagPeriode(klassifisering, segment, indeks)
                    }.kunSisteHvis(erOpphør)
        }

        return Utbetalingsoppdrag(
                saksbehandlerId = saksbehandlerId,
                kodeEndring = if (!erOpphør) NY else UEND,
                fagSystem = FAGSYSTEM,
                saksnummer = tilkjentYtelse.saksnummer,
                aktoer = tilkjentYtelse.personIdentifikator,
                utbetalingsperiode = utbetalingsperioder,
                avstemmingTidspunkt = finnAvstemmingTidspunkt()
        )
    }

    private fun <T> List<T>.kunSisteHvis(kunSiste: Boolean): List<T> {
        return this.foldRight(mutableListOf()) { element, resultat ->
            if (resultat.size == 0 || !kunSiste) resultat.add(0, element)

            resultat
        }
    }

    internal fun finnAvstemmingTidspunkt() : LocalDateTime = LocalDateTime.now()

    private data class UtbetalingsperiodeMal(
            val tilkjentYtelse: TilkjentYtelse,
            val erEndringPåEksisterendePeriode: Boolean = false,
            val periodeIdStart: Long = tilkjentYtelse.id
    ) {

        private val MAX_PERIODEID_OFFSET = 1_000

        fun lagPeriode(klassifisering: String, segment: LocalDateSegment<Int>, periodeIdOffset: Int): Utbetalingsperiode {

            // Vedtak-id øker med 50, så vi kan ikke risikere overflow
            if (periodeIdOffset >= MAX_PERIODEID_OFFSET) {
                throw IllegalArgumentException("periodeIdOffset forsøkt satt høyere enn ${MAX_PERIODEID_OFFSET}. " +
                                               "Det ville ført til duplisert periodeId")
            }

            // Skaper "plass" til offset
            val utvidetPeriodeIdStart = periodeIdStart * MAX_PERIODEID_OFFSET

            return Utbetalingsperiode(
                    erEndringPåEksisterendePeriode,
                    tilkjentYtelse.opphørFom?.let { Opphør(it) },
                    utvidetPeriodeIdStart + periodeIdOffset,
                    if (periodeIdOffset > 0) utvidetPeriodeIdStart + (periodeIdOffset - 1).toLong() else null,
                    tilkjentYtelse.vedtaksdato!!,
                    klassifisering,
                    segment.fom,
                    segment.tom,
                    BigDecimal(segment.value),
                    MND,
                    tilkjentYtelse.personIdentifikator,
                    tilkjentYtelse.id
            )
        }
    }
}
