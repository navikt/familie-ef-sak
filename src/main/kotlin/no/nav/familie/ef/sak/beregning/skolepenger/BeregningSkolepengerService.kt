package no.nav.familie.ef.sak.beregning.skolepenger

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.beregning.barnetilsyn.roundUp
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.ef.sak.tilkjentytelse.domain.TilkjentYtelse
import no.nav.familie.ef.sak.vedtak.dto.SkolepengerUtgiftDto
import no.nav.familie.ef.sak.vedtak.dto.UtgiftsperiodeSkolepengerDto
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.Year
import java.time.YearMonth
import java.time.temporal.ChronoUnit.MONTHS
import java.util.UUID

private val ONE_HUNDRED: BigDecimal = 100.toBigDecimal()

/**
 * Er maksbeløp per skoleår? Eller kan det endre seg midt i?
 *
 */

private val maksbeløpPerSkoleår = 68_000

@Service
class BeregningSkolepengerService(
    private val behandlingService: BehandlingService,
    private val tilkjentYtelseService: TilkjentYtelseService
) {

    fun beregnYtelse(
        utgiftsperioder: List<UtgiftsperiodeSkolepengerDto>,
        behandlingId: UUID,
        forrigeTilkjentYtelse: TilkjentYtelse? = forrigeTilkjentYtelse(behandlingId)
    ): BeregningSkolepengerResponse {
        return beregnYtelse(utgiftsperioder, forrigeTilkjentYtelse?.beløpPerÅrMåned() ?: emptyMap())
    }

    fun beregnYtelse(
        utgiftsperioder: List<UtgiftsperiodeSkolepengerDto>,
        tidligereUtbetalinger: Map<YearMonth, Int>
    ): BeregningSkolepengerResponse {
        validerGyldigePerioder(utgiftsperioder)
        validerFornuftigeBeløp(utgiftsperioder)

        val perioder = beregnSkoleårsperioder(tidligereUtbetalinger, utgiftsperioder)
        return BeregningSkolepengerResponse(perioder)
    }

    private fun beregnSkoleårsperioder(
        tidligereUtbetalinger: Map<YearMonth, Int>,
        utgiftsperioder: List<UtgiftsperiodeSkolepengerDto>
    ): List<BeløpsperiodeSkolepenger> {
        val tidligereUtbetalingerPerSkoleår = tidligereUtbetalingerPerSkoleår(tidligereUtbetalinger)

        return utgiftsperioder
            .groupBy { it.årMånedFra.skoleår() }
            .map {
                val previous = tidligereUtbetalingerPerSkoleår.getOrDefault(it.key, emptyMap())
                val periode = it.value.singleOrNull()
                brukerfeilHvis(periode == null) {
                    "Antall perioder for skoleår=${it.key} er fler enn 1"
                }
                beregnBeløpsperioder(it.key, previous, periode)
            }
    }

    private fun beregnBeløpsperioder(
        skoleår: Year,
        tidligereForbrukt: Map<YearMonth, Int>,
        periode: UtgiftsperiodeSkolepengerDto
    ): BeløpsperiodeSkolepenger {
        val maksbeløp = maksbeløpPerSkoleår

        val studiebelastning = periode.studiebelastning.toBigDecimal()
        val antallMåneder = minOf(MONTHS.between(periode.årMånedFra, periode.årMånedTil) + 1, 10)
        feilHvis(antallMåneder < 1 || antallMåneder > 10) {
            "Antall måneder er $antallMåneder men må være 1-10"
        }
        val muligmaksbeløp = beregnMuligMaksbeløp(antallMåneder, maksbeløp, studiebelastning)
        val utbetalingsperioder = beregnUtbetalingsperioder(periode, tidligereForbrukt, muligmaksbeløp)
        return BeløpsperiodeSkolepenger(
            skoleår = skoleår,
            maksbeløp = maksbeløp,
            maksbeløpFordeltAntallMåneder = muligmaksbeløp,
            alleredeUtbetalt = 0, //tidligereForbrukt,
            nyForbrukt = 0, //nyForbrukt, // Burde denna være en løpende nyForbrukt fra forrige periode?
            utbetalinger = utbetalingsperioder,
            grunnlag = BeregningsgrunnlagSkolepengerDto(
                periode.studietype,
                periode.studiebelastning,
                periode.tilPeriode()
            )
        )
    }

    private fun beregnUtbetalingsperioder(
        periode: UtgiftsperiodeSkolepengerDto,
        tidligereForbrukt: Map<YearMonth, Int>,
        muligmaksbeløp: Int
    ): List<BeregnetUtbetalingSkolepenger> {
        val sumTidligereUtbetalt = tidligereForbrukt.values.sum()
        var tilgjengelig = maxOf(muligmaksbeløp - sumTidligereUtbetalt, 0)

        return periode.utgifter.groupBy { it.årMånedFra }
            .toSortedMap()
            .map { (fra, utgifter) ->
                val (nyTilgjengelig, beregnetUtbetaling) = beregnPerÅrMåned(tidligereForbrukt, utgifter, fra, tilgjengelig)
                tilgjengelig = nyTilgjengelig
                beregnetUtbetaling
            }
    }

    private fun beregnPerÅrMåned(
        tidligereForbrukt: Map<YearMonth, Int>,
        utgifter: List<SkolepengerUtgiftDto>,
        fra: YearMonth,
        tilgjengelig: Int
    ): Pair<Int, BeregnetUtbetalingSkolepenger> {
        val utgifterTotaltForPeriode = utgifter.sumOf { it.utgifter }
        val tidligereForbruktForMåned = tidligereForbrukt[fra] ?: 0

        val ekstraUtbetalingForPeriode = maxOf(utgifterTotaltForPeriode - tidligereForbruktForMåned, 0)
        val ekstraUtbetaling = minOf(tilgjengelig, ekstraUtbetalingForPeriode)
        val nyttBeløp = tidligereForbruktForMåned + ekstraUtbetaling
        val nyTilgjengelig = tilgjengelig - ekstraUtbetaling
        return nyTilgjengelig to BeregnetUtbetalingSkolepenger(nyttBeløp, SkolepengerUtgiftDto(fra, utgifterTotaltForPeriode))
    }

    private fun beregnMuligMaksbeløp(
        antallMåneder: Long,
        maksbeløp: Int,
        studiebelastning: BigDecimal
    ): Int {
        val månedsdel = antallMåneder.toBigDecimal().divide(BigDecimal.TEN)
        val maksbeløpFordeltAntallMåneder = maksbeløp.toBigDecimal().multiply(månedsdel).roundUp()
        return maksbeløpFordeltAntallMåneder.multiply(studiebelastning.divide(ONE_HUNDRED)).roundUp().toInt()
    }

    private fun tidligereUtbetalingerPerSkoleår(tidligereUtbetalinger: Map<YearMonth, Int>) =
        tidligereUtbetalinger.entries
            .groupBy { it.key.skoleår() }
            .map { (skoleår, månedsbeløp) -> skoleår to månedsbeløp.associate { it.key to it.value } }
            .toMap()

    private fun forrigeTilkjentYtelse(behandlingId: UUID): TilkjentYtelse? {
        return behandlingService.hentSaksbehandling(behandlingId).forrigeBehandlingId
            ?.let { tilkjentYtelseService.hentForBehandling(it) }
    }

    private fun YearMonth.skoleår(): Year =
        Year.of(if (this.monthValue > 6) this.year else this.year.minus(1))

    private fun TilkjentYtelse.beløpPerÅrMåned() =
        this.andelerTilkjentYtelse.associate { YearMonth.from(it.stønadFom) to it.beløp }

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
