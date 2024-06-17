package no.nav.familie.ef.sak.no.nav.familie.ef.sak.beregning.gomregningkalkyle

import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.beregning.BeregningRequest
import no.nav.familie.ef.sak.beregning.BeregningService
import no.nav.familie.ef.sak.beregning.Grunnbeløp
import no.nav.familie.ef.sak.beregning.Grunnbeløpsperioder
import no.nav.familie.ef.sak.beregning.Inntekt
import no.nav.familie.ef.sak.beregning.tilInntektsperioder
import no.nav.familie.ef.sak.vedtak.domain.AktivitetType.BARN_UNDER_ETT_ÅR
import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType.HOVEDPERIODE
import no.nav.familie.ef.sak.vedtak.dto.VedtaksperiodeDto
import no.nav.familie.ef.sak.vedtak.dto.tilPerioder
import no.nav.familie.kontrakter.felles.Månedsperiode
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth

/**
 * Denne klassen kan brukes for å beregne kostnad ved gomregning.
 * Med å sette forskjellige prosentsatser hhv MED og UTEN inntektsjusteringer
 * Uttrekk fra database til CSV - bytt utplukksmåned med den måneden data hentes ut for:
 *
 * select inntekt, inntektsreduksjon, samordningsfradrag, belop  from gjeldende_iverksatte_behandlinger gib
 *     join fagsak f on gib.fagsak_id = f.id
 *     join tilkjent_ytelse ty on ty.behandling_id = gib.id
 * join andel_tilkjent_ytelse aty on aty.tilkjent_ytelse = ty.id
 * where f.stonadstype='OVERGANGSSTØNAD' AND aty.stonad_fom <= '2024-02-01' AND aty.stonad_tom >= '2024-02-01'
 */
@Disabled
class GOmregningKalkyle : OppslagSpringRunnerTest() {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Autowired
    private lateinit var beregningService: BeregningService

    val justeringsfaktor1Prosent = BigDecimal(1.01).setScale(2, RoundingMode.HALF_UP)
    val justeringsfaktor2Prosent = BigDecimal(1.02).setScale(2, RoundingMode.HALF_UP)
    val justeringsfaktor3Prosent = BigDecimal(1.03).setScale(2, RoundingMode.HALF_UP)
    val justeringsfaktor4Prosent = BigDecimal(1.04).setScale(2, RoundingMode.HALF_UP)
    val utplukksMåned = YearMonth.of(2024, 2)

    @Test
    fun `skal kalkulere utbetalingsbeløp - gitt 2024 G-beløp uten økning (uten samordning)`() {
        mockGrunnbeløp(grunnbeløp())
        val sumBeløp = utførBeregningAvTotalbeløp()
        Assertions.assertThat(sumBeløp).isEqualTo(BigDecimal(123_488_015))
        logger.info("Utbetaling av totalen er $sumBeløp")
        unmockGrunnbeløp()
    }

    @Test
    fun `skal kalkulere utbetalingsbeløp - gitt 2024 G-beløp med 4 prosent økning (uten samordning)`() {
        mockGrunnbeløp(grunnbeløp(justeringsfaktor4Prosent))
        val sumBeløpMedInntektsjustering = utførBeregningAvTotalbeløp(justeringsfaktor4Prosent)
        val sumBeløp = utførBeregningAvTotalbeløp()
        Assertions.assertThat(sumBeløpMedInntektsjustering).isEqualTo(BigDecimal(128_547_568))
        Assertions.assertThat(sumBeløp).isEqualTo(BigDecimal(132_542_698))
        logger.info("Diff mellom utbetaling for $justeringsfaktor4Prosent prosent økning: ${sumBeløp.minus(sumBeløpMedInntektsjustering)}")
        unmockGrunnbeløp()
    }

    @Test
    fun `skal kalkulere utbetalingsbeløp - gitt 2024 G-beløp med 3 prosent økning (uten samordning)`() {
        val grunnbeløp = grunnbeløp(justeringsfaktor3Prosent)
        mockGrunnbeløp(grunnbeløp)
        val sumBeløpMedInntektsjustering = utførBeregningAvTotalbeløp(justeringsfaktor3Prosent)
        val sumBeløp = utførBeregningAvTotalbeløp()
        Assertions.assertThat(sumBeløpMedInntektsjustering).isEqualTo(BigDecimal(127_308_776))
        Assertions.assertThat(sumBeløp).isEqualTo(BigDecimal(130_259_980))
        logger.info("Diff mellom utbetaling for $justeringsfaktor3Prosent prosent økning: ${sumBeløp.minus(sumBeløpMedInntektsjustering)}")
        unmockGrunnbeløp()
    }

    @Test
    fun `skal kalkulere utbetalingsbeløp - gitt 2024 G-beløp med 2 prosent økning (uten samordning)`() {
        val grunnbeløp = grunnbeløp(justeringsfaktor2Prosent)
        mockGrunnbeløp(grunnbeløp)
        val sumBeløpMedInntektsjustering = utførBeregningAvTotalbeløp(justeringsfaktor2Prosent)
        val sumBeløp = utførBeregningAvTotalbeløp()
        Assertions.assertThat(sumBeløpMedInntektsjustering).isEqualTo(BigDecimal(126_074_688))
        Assertions.assertThat(sumBeløp).isEqualTo(BigDecimal(127_997_127))
        logger.info("Diff mellom utbetaling for $justeringsfaktor2Prosent prosent økning: ${sumBeløp.minus(sumBeløpMedInntektsjustering)}")
        unmockGrunnbeløp()
    }

    @Test
    fun `skal kalkulere utbetalingsbeløp - gitt 2024 G-beløp med 1 prosent økning (uten samordning)`() {
        val grunnbeløp = grunnbeløp(justeringsfaktor1Prosent)
        mockGrunnbeløp(grunnbeløp)
        val sumBeløpMedInntektsjustering = utførBeregningAvTotalbeløp(justeringsfaktor1Prosent)
        val sumBeløp = utførBeregningAvTotalbeløp()
        Assertions.assertThat(sumBeløpMedInntektsjustering).isEqualTo(BigDecimal(124_847_384))
        Assertions.assertThat(sumBeløp).isEqualTo(BigDecimal(125_741_828))
        logger.info("Diff mellom utbetaling for $justeringsfaktor1Prosent prosent økning: ${sumBeløp.minus(sumBeløpMedInntektsjustering)}")
        unmockGrunnbeløp()
    }

    private fun utførBeregningAvTotalbeløp(inntektsøkningsfaktor: BigDecimal = BigDecimal.ONE): BigDecimal {
        val readFile = readFile("basis-2024-gomregning.csv")
        val sumBeløp =
            readFile
                .lines()
                .mapIndexedNotNull(::parseRad)
                .filter { rad -> rad.samordningsfradrag == BigDecimal.ZERO }
                .map { rad -> kalkulerBeløpForRad(rad, inntektsøkningsfaktor) }
                .sumOf { it }
        return sumBeløp
    }

    private fun kalkulerBeløpForRad(
        rad: Rad,
        inntektsøkningsfaktor: BigDecimal,
    ): BigDecimal {
        val inntekt =
            Inntekt(
                årMånedFra = utplukksMåned,
                forventetInntekt = rad.forventetInntekt.multiply(inntektsøkningsfaktor),
                samordningsfradrag = rad.samordningsfradrag,
            )
        val request =
            BeregningRequest(
                inntekt = listOf(inntekt),
                vedtaksperioder =
                    listOf(
                        VedtaksperiodeDto(
                            årMånedFra = utplukksMåned,
                            årMånedTil = utplukksMåned,
                            aktivitet = BARN_UNDER_ETT_ÅR,
                            periodeType = HOVEDPERIODE,
                        ),
                    ),
            )

        val beregnetYtelse =
            beregningService.beregnYtelse(
                request.vedtaksperioder.tilPerioder(),
                request.inntekt.tilInntektsperioder(),
            )
        return beregnetYtelse.first().beløp
    }

    private fun parseRad(
        index: Int,
        radString: String,
    ) = if (index == 0) {
        null
    } else {
        val rad = radString.split(",")

        Rad(
            forventetInntekt = rad[0].toBigDecimal(),
            inntektsreduksjon = rad[1].toBigDecimal(),
            samordningsfradrag = rad[2].toBigDecimal(),
            beløp = rad[3].toBigDecimal(),
        )
    }

    private fun readFile(filnavn: String): String = this::class.java.getResource("/omregning/$filnavn")!!.readText()

    private fun unmockGrunnbeløp() {
        unmockkObject(Grunnbeløpsperioder)
    }

    private fun mockGrunnbeløp(grunnbeløp: Grunnbeløp) {
        mockkObject(Grunnbeløpsperioder)
        every { Grunnbeløpsperioder.grunnbeløpsperioder } returns listOf(grunnbeløp)
        every { Grunnbeløpsperioder.nyesteGrunnbeløp } returns grunnbeløp
        every { Grunnbeløpsperioder.nyesteGrunnbeløpGyldigFraOgMed } returns grunnbeløp.periode.fom
    }

    private fun grunnbeløp(justeringsfaktor: BigDecimal = BigDecimal.ONE) =
        Grunnbeløp(
            periode = Månedsperiode(utplukksMåned, YearMonth.from(LocalDate.MAX)),
            grunnbeløp = 118_620.toBigDecimal().multiply(justeringsfaktor),
            perMnd = 9_885.toBigDecimal().multiply(justeringsfaktor),
        )
}

data class Rad(
    val forventetInntekt: BigDecimal,
    val inntektsreduksjon: BigDecimal,
    val samordningsfradrag: BigDecimal,
    val beløp: BigDecimal,
)
