package no.nav.familie.ef.sak.beregning.barnetilsyn.satsendring

import no.nav.familie.ef.sak.beregning.barnetilsyn.mergeSammenhengendePerioder
import no.nav.familie.ef.sak.beregning.barnetilsyn.tilBeløpsperioderPerUtgiftsmåned
import no.nav.familie.ef.sak.vedtak.dto.PeriodeMedBeløpDto
import no.nav.familie.ef.sak.vedtak.dto.UtgiftsperiodeDto
import no.nav.familie.ef.sak.vedtak.historikk.AndelHistorikkDto
import no.nav.familie.ef.sak.vedtak.historikk.VedtakHistorikkService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.YearMonth
import java.util.UUID

@Service
class BarnetilsynSatsendringService(
    val barnetilsynSatsendringRepository: BarnetilsynSatsendringRepository,
    val vedtakHistorikkService: VedtakHistorikkService
) {
    private val logger = LoggerFactory.getLogger(javaClass)



    fun kjørSatsendring() {
        val fagsakIds = barnetilsynSatsendringRepository.finnSatsendringskandidaterForBarnetilsyn()
        val barnetilsynSatsendringKanditat : List <BarnetilsynSatsendringKanditat> = fagsakIds.map { BarnetilsynSatsendringKanditat(it, vedtakHistorikkService.hentAktivHistorikk(it)) }


        barnetilsynSatsendringKanditat.forEach{
            val andeler2023 = it.andelerEtter(YearMonth.of(2022, 12))

            val utgiftsperiode = andeler2023.map {
                UtgiftsperiodeDto( periode = it.andel.periode, barn = it.andel.barn, utgifter = it.andel.utgifter.toInt(), erMidlertidigOpphør = false)     // TODO sjekk erMidlertidigOpphør...
            }

            val simulertNyBeregning =
                utgiftsperiode.tilBeløpsperioderPerUtgiftsmåned(andeler2023.map {
                    PeriodeMedBeløpDto(periode = it.andel.periode, beløp = it.andel.kontantstøtte)
                }, andeler2023.map {
                    PeriodeMedBeløpDto(periode = it.andel.periode, beløp = it.andel.tilleggsstønad)
                }).values.toList()

            val sammenhengendePerioder = simulertNyBeregning.mergeSammenhengendePerioder()


//TODO sammenlikne  "simulertNyBeregning" med andeler2023?


        }


        logger.info("Kandidater satsendring størrelse ${barnetilsynSatsendringKanditat.size}")

    }
}

data class BarnetilsynSatsendringKanditat(
    val fagsakId: UUID,
    val andelshistorikk: List<AndelHistorikkDto>
) {
    fun andelerEtter(yearMonth: YearMonth): List<AndelHistorikkDto> {

        return andelshistorikk.filter { it.andel.periode.tom.isAfter(yearMonth) }
    }


}
