package no.nav.familie.ef.sak.beregning.barnetilsyn.satsendring

import no.nav.familie.ef.sak.beregning.barnetilsyn.BeløpsperiodeBarnetilsynDto
import no.nav.familie.ef.sak.beregning.barnetilsyn.tilBeløpsperioderPerUtgiftsmåned
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.vedtak.dto.PeriodeMedBeløpDto
import no.nav.familie.ef.sak.vedtak.dto.UtgiftsperiodeDto
import no.nav.familie.ef.sak.vedtak.historikk.AndelHistorikkDto
import no.nav.familie.ef.sak.vedtak.historikk.VedtakHistorikkService
import no.nav.familie.prosessering.internal.TaskService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.YearMonth
import java.util.UUID

@Service
class BarnetilsynSatsendringService(
    val barnetilsynSatsendringRepository: BarnetilsynSatsendringRepository,
    val vedtakHistorikkService: VedtakHistorikkService,
    val taskService: TaskService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun finnFagsakerSomSkalSatsendresMedNySatsDersomBaselineErOk() {
        val barnetilsynGjeldeneAvstemmingsfeil =
            finnFagsakerSomSkalSatsendresMedNySats(false)

        barnetilsynGjeldeneAvstemmingsfeil.forEach {
            logger.warn(
                "Skulle ikke ha fått differanse i andeler ved reberegning av barnetilsyn-saker med nåværende satser." +
                    " FagsakId: ${it.fagsakId}"
            )
        }

        if (barnetilsynGjeldeneAvstemmingsfeil.isEmpty()) {
            val fagsakerSomMåRevurderesGrunnetSatsendring =
                finnFagsakerSomSkalSatsendresMedNySats(true)

            fagsakerSomMåRevurderesGrunnetSatsendring.forEach {
                logger.info("${it.fagsakId}: skal revurderes/endres etter satsendring")
            }
        }
    }

    private fun finnFagsakerSomSkalSatsendresMedNySats(brukIkkeVedtatteSatser: Boolean = true): List<BarnetilsynSatsendringKanditat> {
        val fagsakIds = barnetilsynSatsendringRepository.finnSatsendringskandidaterForBarnetilsyn()
        val barnetilsynSatsendringKanditat: List<BarnetilsynSatsendringKanditat> =
            fagsakIds.map { BarnetilsynSatsendringKanditat(it, vedtakHistorikkService.hentAktivHistorikk(it)) }
        logger.info("Antall kandidater til satsendring: ${barnetilsynSatsendringKanditat.size}")

        val kandidaterMedSkalRevurderesSatt = barnetilsynSatsendringKanditat.map {
            val nåværendeAndelerForNesteÅr = it.andelerEtter(YearMonth.of(YearMonth.now().year, 12))
            val nyBeregningMånedsperioder = gjørNyBeregning(nåværendeAndelerForNesteÅr, brukIkkeVedtatteSatser)
            val skalRevurderes: Boolean =
                finnesStørreBeløpINyBeregning(nyBeregningMånedsperioder, nåværendeAndelerForNesteÅr)
            it.copy(skalRevurderes = skalRevurderes)
        }

        return kandidaterMedSkalRevurderesSatt.filter { it.skalRevurderes }
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
            feilHvis(it.erSanksjon) {
                "Støtter ikke sanksjon. Både erMidlertidigOpphør og sanksjonsårsak burde då settes"
            }
            UtgiftsperiodeDto(
                periode = it.andel.periode,
                barn = it.andel.barn,
                utgifter = it.andel.utgifter.toInt(),
                erMidlertidigOpphør = false,
                sanksjonsårsak = null
            ) // TODO sjekk erMidlertidigOpphør???...
        }
        return utgiftsperiode
    }

    @Transactional
    fun opprettTask() {
        val finnesTask = taskService.finnTaskMedPayloadOgType("barnetilsynSatsendring", BarnetilsynSatsendringTask.TYPE)
        if (finnesTask == null) {
            val task = BarnetilsynSatsendringTask.opprettTask()
            taskService.save(task)
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
