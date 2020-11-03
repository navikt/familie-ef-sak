package no.nav.familie.ef.sak.økonomi

import no.nav.familie.ef.sak.repository.domain.AndelTilkjentYtelse
import no.nav.familie.ef.sak.repository.domain.TilkjentYtelse
import no.nav.familie.kontrakter.felles.oppdrag.Opphør
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Lager mal for generering av utbetalingsperioder med tilpasset setting av verdier basert på parametre
 *
 * @param[vedtak] for vedtakdato og opphørsdato hvis satt
 * @param[erEndringPåEksisterendePeriode] ved true vil oppdrag sette asksjonskode ENDR på linje og ikke referere bakover
 * @return mal med tilpasset lagPeriodeFraAndel
 */
data class UtbetalingsperiodeMal(
        val tilkjentYtelse: TilkjentYtelse,
        val erEndringPåEksisterendePeriode: Boolean = false
) {

    /**
     * Lager utbetalingsperioder som legges på utbetalingsoppdrag. En utbetalingsperiode tilsvarer linjer hos økonomi
     *
     * @param[andel] andel som skal mappes til periode
     * @param[opphørKjedeFom] fom-dato fra tidligste periode i kjede med endring
     * @return Periode til utbetalingsoppdrag
     */
    fun lagPeriodeFraAndel(andel: AndelTilkjentYtelse,
                           opphørKjedeFom: LocalDate? = null): Utbetalingsperiode =
            Utbetalingsperiode(
                    erEndringPåEksisterendePeriode = erEndringPåEksisterendePeriode,
                    opphør = if (erEndringPåEksisterendePeriode) utledOpphørPåLinje(opphørForVedtak = tilkjentYtelse.opphørFom,
                                                                                    opphørForLinje = opphørKjedeFom!!) else null,
                    forrigePeriodeId = andel.forrigePeriodeId,
                    periodeId = andel.periodeId!!,
                    datoForVedtak = tilkjentYtelse.vedtaksdato!!,
                    klassifisering = andel.type.tilKlassifisering(),
                    vedtakdatoFom = andel.stønadFom,
                    vedtakdatoTom = andel.stønadTom,
                    sats = BigDecimal(andel.beløp),
                    satsType = Utbetalingsperiode.SatsType.MND,
                    utbetalesTil = tilkjentYtelse.personident,
                    behandlingId = tilkjentYtelse.behandlingEksternId
            )


    private fun utledOpphørPåLinje(opphørForVedtak: LocalDate?, opphørForLinje: LocalDate): Opphør? {
        return if (opphørForVedtak != null) {
            Opphør(maxOf(opphørForVedtak, opphørForLinje))
        } else {
            Opphør(opphørForLinje)
        }
    }
}
