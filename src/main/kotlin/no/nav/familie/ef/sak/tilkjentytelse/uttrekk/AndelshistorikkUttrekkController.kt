package no.nav.familie.ef.sak.tilkjentytelse.uttrekk

import no.nav.familie.ef.sak.vedtak.historikk.VedtakHistorikkService
import no.nav.security.token.support.core.api.Unprotected
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import java.util.UUID

@Controller
@Unprotected
@RequestMapping(
    path = ["/api/uttrekk"],
    produces = [MediaType.APPLICATION_JSON_VALUE]
)
class AndelshistorikkUttrekkController(
    val andelshistorikkUttrekkRepository: AndelshistorikkUttrekkRepository,
    val vedtakHistorikkService: VedtakHistorikkService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    @GetMapping("/manglertilsyn")
    fun hentDataManglerTilsyn2022() {

        val fagsakerMedTilsynManglerKandidater =
            andelshistorikkUttrekkRepository.finnFagsakerMedTilsynManglerKandidater()

        val tilsynmangelKanditatMedHistorikk: List<VedtakMedTilsynordningKanditat> =
            fagsakerMedTilsynManglerKandidater.map {
                VedtakMedTilsynordningKanditat(
                    it,
                    vedtakHistorikkService.hentAktivHistorikk(it)
                )
            }

        val harAndelI2022MedManglerTilsyn = tilsynmangelKanditatMedHistorikk.filter { it.harAndelOgManglerTilsyn(2022) }

        val alleAvsluttedeMedAntall = harAndelI2022MedManglerTilsyn
            .filter { it.harAvsluttetPeriodeMedManglendeTilsyn(2022) }
            .map {
                val parr = FagsakMånedsbeløpUttrekkDto(
                    it.fagsakId,
                    it.antallMånederMedManglendeTilsynSomErAvsluttet(2022),
                    it.beløpForManglendeTilsynSomErAvsluttet(2022)
                )
                secureLogger.info("Snittutregning-manglertilsyn: $parr ")
                parr
            }

        val alleMnd = alleAvsluttedeMedAntall.sumOf { it.antallMåneder }
        val snitt = alleMnd.div(alleAvsluttedeMedAntall.size)
        val snittBeløp = alleAvsluttedeMedAntall.sumOf { it.totalBeløp }.div(alleMnd)

        logger.info("Uttrekk unntatt aktivitet: mangler tilsyn. Total: ${harAndelI2022MedManglerTilsyn.size}. Snitt mnd: $snitt, Snitt beløp pr mnd: $snittBeløp ")
    }
}

data class FagsakMånedsbeløpUttrekkDto(val fagsakId: UUID, val antallMåneder: Long, val totalBeløp: Long)
