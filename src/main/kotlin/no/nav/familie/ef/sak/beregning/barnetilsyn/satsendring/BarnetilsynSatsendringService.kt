package no.nav.familie.ef.sak.beregning.barnetilsyn.satsendring

import no.nav.familie.ef.sak.beregning.barnetilsyn.tilBeløpsperioderPerUtgiftsmåned
import no.nav.familie.ef.sak.vedtak.dto.PeriodeMedBeløpDto
import no.nav.familie.ef.sak.vedtak.dto.UtgiftsperiodeDto
import no.nav.familie.ef.sak.vedtak.historikk.AndelHistorikkDto
import no.nav.familie.ef.sak.vedtak.historikk.VedtakHistorikkService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.YearMonth
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

@Service
class BarnetilsynSatsendringService(
    val barnetilsynSatsendringRepository: BarnetilsynSatsendringRepository,
    val vedtakHistorikkService: VedtakHistorikkService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @OptIn(ExperimentalTime::class)
    fun kjørSatsendring() {
        val elapsed: Duration = measureTime {
            val fagsakIds = barnetilsynSatsendringRepository.finnSatsendringskandidaterForBarnetilsyn()
            val barnetilsynSatsendringKanditat: List<BarnetilsynSatsendringKanditat> = fagsakIds.map { BarnetilsynSatsendringKanditat(it, vedtakHistorikkService.hentAktivHistorikk(it)) }

            val skalRevurderes = barnetilsynSatsendringKanditat.map {
                val andeler2023 = it.andelerEtter(YearMonth.of(2022, 12))
                val utgiftsperiode = andeler2023.map {
                    UtgiftsperiodeDto(
                        periode = it.andel.periode,
                        barn = it.andel.barn,
                        utgifter = it.andel.utgifter.toInt(),
                        erMidlertidigOpphør = false
                    ) // TODO sjekk erMidlertidigOpphør???...
                }

                val simulertNyBeregning =
                    utgiftsperiode.tilBeløpsperioderPerUtgiftsmåned(
                        andeler2023.map {
                            PeriodeMedBeløpDto(periode = it.andel.periode, beløp = it.andel.kontantstøtte)
                        },
                        andeler2023.map {
                            PeriodeMedBeløpDto(periode = it.andel.periode, beløp = it.andel.tilleggsstønad)
                        }
                    ).values.toList()

                val skalRevurderes: Boolean = simulertNyBeregning.any { nyMånedsberegning ->
                    andeler2023.any { it.andel.periode.overlapper(nyMånedsberegning.periode) && it.andel.beløp < nyMånedsberegning.beløp }
                }
                // val sammenhengendePerioder = simulertNyBeregning.mergeSammenhengendePerioder()
                it.copy(skalRevurderes = skalRevurderes)
            }
            logger.info("Kandidater satsendring størrelse ${barnetilsynSatsendringKanditat.size}")
            skalRevurderes.forEach {
                logger.info("${it.fagsakId}: Skal revurderes/endres etter satsendring:  ${it.skalRevurderes}")
            }
        }
        logger.info("Duration: $elapsed")
    }
}

data class BarnetilsynSatsendringKanditat(
    val fagsakId: UUID,
    val andelshistorikk: List<AndelHistorikkDto>,
    val skalRevurderes: Boolean = false
) {
    fun andelerEtter(yearMonth: YearMonth): List<AndelHistorikkDto> {
        return andelshistorikk.filter { it.andel.periode.tom.isAfter(yearMonth) }
    }
}
