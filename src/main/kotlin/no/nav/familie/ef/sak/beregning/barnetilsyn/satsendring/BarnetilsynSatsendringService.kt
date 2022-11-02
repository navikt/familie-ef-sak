package no.nav.familie.ef.sak.beregning.barnetilsyn.satsendring

import no.nav.familie.ef.sak.beregning.barnetilsyn.BeløpsperiodeBarnetilsynDto
import no.nav.familie.ef.sak.beregning.barnetilsyn.tilBeløpsperioderPerUtgiftsmåned
import no.nav.familie.ef.sak.vedtak.dto.PeriodeMedBeløpDto
import no.nav.familie.ef.sak.vedtak.dto.UtgiftsperiodeDto
import no.nav.familie.ef.sak.vedtak.historikk.AndelHistorikkDto
import no.nav.familie.ef.sak.vedtak.historikk.VedtakHistorikkService
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.YearMonth
import java.util.UUID

@Service
class BarnetilsynSatsendringService(
    val barnetilsynSatsendringRepository: BarnetilsynSatsendringRepository,
    val vedtakHistorikkService: VedtakHistorikkService,
    val taskRepository: TaskRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun sjekkIngenEndringerMedNåværendeSats(): Boolean {
        val fagsakIds = barnetilsynSatsendringRepository.finnSatsendringskandidaterForBarnetilsyn()
        val barnetilsynSatsendringKanditat: List<BarnetilsynSatsendringKanditat> = fagsakIds.map { BarnetilsynSatsendringKanditat(it, vedtakHistorikkService.hentAktivHistorikk(it)) }

        val kandidaterMedSkalRevurderesSatt = barnetilsynSatsendringKanditat.map {
            val nåværendeAndelerForNesteÅr = it.andelerEtter(YearMonth.of(YearMonth.now().year, 12))
            val nyBeregningMånedsperioder = gjørNyBeregning(nåværendeAndelerForNesteÅr, brukIkkeVedtatteSatser = false)
            val skalRevurderes: Boolean = finnesStørreBeløpINyBeregning(nyBeregningMånedsperioder, nåværendeAndelerForNesteÅr)

            // val sammenhengendePerioder = simulertNyBeregning.mergeSammenhengendePerioder()
            it.copy(skalRevurderes = skalRevurderes)
        }

        logger.info("Antall kandidater til sjekk på satsendring med nåværende satser: ${barnetilsynSatsendringKanditat.size}")

        kandidaterMedSkalRevurderesSatt.filter { it.skalRevurderes }.forEach {
            logger.warn(
                "Skulle ikke ha fått differanse i andeler ved reberegning av barnetilsyn-saker med nåværende satser." +
                    " FagsakId: ${it.fagsakId}"
            )
        }

        return kandidaterMedSkalRevurderesSatt.any { it.skalRevurderes }
    }

    fun logFagsakerSomSkalSatsendresMedNySats() {
        if (sjekkIngenEndringerMedNåværendeSats()) {
            return
        }
        val fagsakIds = barnetilsynSatsendringRepository.finnSatsendringskandidaterForBarnetilsyn()
        val barnetilsynSatsendringKanditat: List<BarnetilsynSatsendringKanditat> = fagsakIds.map { BarnetilsynSatsendringKanditat(it, vedtakHistorikkService.hentAktivHistorikk(it)) }

        val kandidaterMedSkalRevurderesSatt = barnetilsynSatsendringKanditat.map {
            val nåværendeAndelerForNesteÅr = it.andelerEtter(YearMonth.of(YearMonth.now().year, 12))
            val nyBeregningMånedsperioder = gjørNyBeregning(nåværendeAndelerForNesteÅr, brukIkkeVedtatteSatser = true)
            val skalRevurderes: Boolean = finnesStørreBeløpINyBeregning(nyBeregningMånedsperioder, nåværendeAndelerForNesteÅr)

            // val sammenhengendePerioder = simulertNyBeregning.mergeSammenhengendePerioder()
            it.copy(skalRevurderes = skalRevurderes)
        }

        logger.info("Antall kandidater til satsendring: ${barnetilsynSatsendringKanditat.size}")

        kandidaterMedSkalRevurderesSatt.filter { it.skalRevurderes }.forEach {
            logger.info("${it.fagsakId}: skal revurderes/endres etter satsendring")
        }
    }

    private fun finnesStørreBeløpINyBeregning(
        nyBeregningMånedsperioder: List<BeløpsperiodeBarnetilsynDto>,
        nåværendeAndelerForNesteÅr: List<AndelHistorikkDto>
    ) = nyBeregningMånedsperioder.any { nyMånedsberegning ->
        nåværendeAndelerForNesteÅr.any { it.andel.periode.overlapper(nyMånedsberegning.periode) && it.andel.beløp < nyMånedsberegning.beløp }
    }

    private fun gjørNyBeregning(andelerNesteÅr: List<AndelHistorikkDto>, brukIkkeVedtatteSatser: Boolean = false): List<BeløpsperiodeBarnetilsynDto> {
        val utgiftsperiode = mapAndelerForNesteÅrTilUtgiftsperiodeDto(andelerNesteÅr)

        val simulertNyBeregning =
            utgiftsperiode.tilBeløpsperioderPerUtgiftsmåned(
                andelerNesteÅr.map {
                    PeriodeMedBeløpDto(periode = it.andel.periode, beløp = it.andel.kontantstøtte)
                },
                andelerNesteÅr.map {
                    PeriodeMedBeløpDto(periode = it.andel.periode, beløp = it.andel.tilleggsstønad)
                },
                brukIkkeVedtatteSatser = brukIkkeVedtatteSatser
            ).values.toList()
        return simulertNyBeregning
    }

    private fun mapAndelerForNesteÅrTilUtgiftsperiodeDto(andeler2023: List<AndelHistorikkDto>): List<UtgiftsperiodeDto> {
        val utgiftsperiode = andeler2023.map {
            UtgiftsperiodeDto(
                periode = it.andel.periode,
                barn = it.andel.barn,
                utgifter = it.andel.utgifter.toInt(),
                erMidlertidigOpphør = false
            ) // TODO sjekk erMidlertidigOpphør???...
        }
        return utgiftsperiode
    }

    @Transactional
    fun opprettTask() {
        val finnesTask = taskRepository.findByPayloadAndType("barnetilsynSatsendring", BarnetilsynSatsendringTask.TYPE)
        if (finnesTask == null) {
            val task = BarnetilsynSatsendringTask.opprettTask()
            taskRepository.save(task)
        }
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
