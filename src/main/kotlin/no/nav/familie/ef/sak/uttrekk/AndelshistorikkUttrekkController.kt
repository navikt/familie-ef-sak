package no.nav.familie.ef.sak.uttrekk

import no.nav.familie.ef.sak.vedtak.historikk.VedtakHistorikkService
import no.nav.security.token.support.core.api.Unprotected
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import java.math.BigDecimal
import java.time.YearMonth
import java.util.UUID

@Controller
@Unprotected
@RequestMapping(
    path = ["/api/uttrekk/andelshistorikk"],
    produces = [MediaType.APPLICATION_JSON_VALUE]
)
class AndelshistorikkUttrekkController(
    val andelshistorikkUttrekkRepository: AndelshistorikkUttrekkRepository,
    val vedtakHistorikkService: VedtakHistorikkService
) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    @GetMapping("/manglertilsyn")
    fun hentDataManglerTilsyn2022(): ResponseEntity<String> {
        val fagsakerMedTilsynManglerKandidater = andelshistorikkUttrekkRepository.finnFagsakerMedTilsynManglerKandidater()

        val fagsakerMedAndelshistorikk: List<UttrekkFagsakMedAndelshistorikk> =
            fagsakerMedTilsynManglerKandidater.map {
                UttrekkFagsakMedAndelshistorikk(it, vedtakHistorikkService.hentAktivHistorikk(it))
            }

        val alleSomManglerTilsynForÅr = fagsakerMedAndelshistorikk.filter { it.harAndelOgManglerTilsyn(2022) }

        val alleAvsluttedeMedBeløpOgAntallMåneder: List<ResultatPerFagsak> = alleSomManglerTilsynForÅr
            .filter { it.harAvsluttetPeriodeMedManglendeTilsyn(2022) }
            .map {
                val resultatPerFagsak = ResultatPerFagsak(
                    it.fagsakId,
                    it.antallMånederMedManglendeTilsynSomErAvsluttet(2022),
                    it.beløpForManglendeTilsynSomErAvsluttet(2022),
                    it.tidligsteFom(2022),
                )
                secureLogger.info("Snittutregning-manglertilsyn: $resultatPerFagsak ")
                resultatPerFagsak
            }

        val alleMnd = BigDecimal.valueOf(alleAvsluttedeMedBeløpOgAntallMåneder.sumOf { it.antallMåneder })
        val snittAntMnd = alleMnd.div(BigDecimal(alleAvsluttedeMedBeløpOgAntallMåneder.size))
        val snittBeløp = alleAvsluttedeMedBeløpOgAntallMåneder.sumOf { it.totalBeløp.toBigDecimal() }.div(alleMnd)

        val uttrekkResultat =
            "Uttrekk unntatt aktivitet: mangler tilsyn. Total: ${alleSomManglerTilsynForÅr.size}. Snitt mnd: $snittAntMnd, Snitt beløp pr mnd: $snittBeløp "
        logger.info(uttrekkResultat)
        return ResponseEntity.ok(uttrekkResultat)
    }
}

data class ResultatPerFagsak(
    val fagsakId: UUID,
    val antallMåneder: Long,
    val totalBeløp: Long,
    val tidligsteFom: YearMonth
)
