package no.nav.familie.ef.sak.beregning.skolepenger

import no.nav.familie.ef.sak.beregning.barnetilsyn.roundUp
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.vedtak.dto.InnvilgelseSkolepenger
import no.nav.familie.ef.sak.vedtak.dto.UtgiftsperiodeSkolepengerDto
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.Year
import java.time.temporal.ChronoUnit.MONTHS

private val ONE_HUNDRED: BigDecimal = 100.toBigDecimal()

/**
 * Er maksbeløp per skoleår? Eller kan det endre seg midt i?
 *
 * TODO: validere at tom_dato ikke kan gå over skoleår, då må man legge inn en ny BeløpsperiodeSkolepengerDto, med utbetaling bak i tiden
 */

private val maksbeløpPerSkoleår = 68_000

@Service
class BeregningSkolepengerService {

    fun beregnYtelse(innvilgelse: InnvilgelseSkolepenger): BeregningSkolepengerResponse {
        return beregnYtelse(innvilgelse.perioder)
    }

    fun beregnYtelse(utgiftsperioder: List<UtgiftsperiodeSkolepengerDto>): BeregningSkolepengerResponse {
        validerGyldigePerioder(utgiftsperioder)
        validerFornuftigeBeløp(utgiftsperioder)

        // fra forrige vedtak
        val tidligereForbruktePerioder = mutableMapOf<Year, Int>()

        val perioder = utgiftsperioder.groupBy {
            Year.of(if (it.årMånedFra.monthValue > 6) it.årMånedFra.year else it.årMånedFra.year.minus(1))
        }.map {
            val previous = tidligereForbruktePerioder.getOrDefault(it.key, 0)
            val periode = it.value.singleOrNull()
            brukerfeilHvis(periode == null) {
                "Antall perioder for skoleår=${it.key} er fler enn 1"
            }
            beregn(it.key, previous, periode)
        }
        return BeregningSkolepengerResponse(perioder)
    }

    private fun beregn(
        skoleår: Year,
        tidligereForbrukt: Int,
        periode: UtgiftsperiodeSkolepengerDto
    ): BeløpsperiodeSkolepenger {
        val maksbeløp = maksbeløpPerSkoleår
        var nyForbrukt = tidligereForbrukt
        val studiebelastning = periode.studiebelastning.toBigDecimal()
        val antallMåneder = minOf(MONTHS.between(periode.årMånedFra, periode.årMånedTil) + 1, 10)
        feilHvis(antallMåneder < 1 || antallMåneder > 10) {
            "Antall måneder er $antallMåneder men må være 1-10"
        }
        val månedsdel = antallMåneder.toBigDecimal().divide(BigDecimal.TEN)
        val maksbeløpFordeltAntallMåneder = maksbeløp.toBigDecimal().multiply(månedsdel).roundUp()
        val maksbeløpEtterStudieredusering =
            maksbeløpFordeltAntallMåneder.multiply(studiebelastning.divide(ONE_HUNDRED)).roundUp().toInt()
        val tilgjengeligFraTidligere = maxOf(maksbeløp - nyForbrukt, 0)
        var tilgjengelig = minOf(tilgjengeligFraTidligere, maksbeløpEtterStudieredusering)
        val nyeUtbetalinger = periode.utgifter.map {
            val nyTilgjengelig = maxOf(tilgjengelig - it.utgifter, 0)
            val utbetales = tilgjengelig - nyTilgjengelig
            tilgjengelig = nyTilgjengelig
            nyForbrukt += utbetales
            BeregnetUtbetalingSkolepenger(utbetales, it)
        }
        return BeløpsperiodeSkolepenger(
            skoleår = skoleår,
            maksbeløp = maksbeløp,
            maksbeløpFordeltAntallMåneder = maksbeløpEtterStudieredusering,
            alleredeUtbetalt = tidligereForbrukt,
            nyForbrukt = nyForbrukt, // Burde denna være en løpende nyForbrukt fra forrige periode?
            utbetalinger = nyeUtbetalinger,
            grunnlag = BeregningsgrunnlagSkolepengerDto(
                periode.studietype,
                periode.studiebelastning,
                periode.tilPeriode()
            )
        )
    }

    private fun validerFornuftigeBeløp(utgiftsperioder: List<UtgiftsperiodeSkolepengerDto>) {

        brukerfeilHvis(utgiftsperioder.any { periode -> periode.utgifter.any { it.utgifter < 0 } }) {
            "Utgifter kan ikke være mindre enn 0"
        }

        brukerfeilHvis(utgiftsperioder.any { it.studiebelastning < 1 }) { "Studiebelastning må være over 0" }
        brukerfeilHvis(utgiftsperioder.any { it.studiebelastning > 100 }) { "Studiebelastning må være under eller lik 100" }
    }

    private fun validerGyldigePerioder(utgiftsperioder: List<UtgiftsperiodeSkolepengerDto>) {
        brukerfeilHvis(utgiftsperioder.isEmpty()) {
            "Ingen utgiftsperioder"
        }
    }
}
