package no.nav.familie.ef.sak.vedtak

import no.nav.familie.ef.sak.felles.util.EnvUtil
import no.nav.familie.ef.sak.vedtak.domain.BarnetilsynWrapper
import no.nav.familie.ef.sak.vedtak.domain.PeriodetypeBarnetilsyn
import no.nav.familie.ef.sak.vedtak.dto.ResultatType
import no.nav.security.token.support.core.api.Unprotected
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/periodetype-barnetilsyn-patch"], produces = [MediaType.APPLICATION_JSON_VALUE])
@Unprotected
class PatchPeriodetypeBarnetilsynController(
    private val vedtakRepository: VedtakRepository
) {

    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    @Transactional
    @PostMapping("{dryRun}")
    fun patchPeriodetyperBarnetilsyn(@PathVariable dryRun: Boolean) {

        val barnetilsynSaker = vedtakRepository.findAll().filter { it.barnetilsyn != null }

        if (!EnvUtil.erIDev()) {
            if (barnetilsynSaker.any { it.resultatType == ResultatType.SANKSJONERE }) {
                error("Skal ikke finnes sanksjon av barnetilsyn")
            }
        }

        val patchetSaker = barnetilsynSaker.map { vedtak ->
            val nyePerioder = vedtak.barnetilsyn?.perioder?.map { periode ->
                val periodetype = if (vedtak.resultatType == ResultatType.SANKSJONERE) {
                    PeriodetypeBarnetilsyn.SANKSJON_1_MND
                } else if (periode.erMidlertidigOpphør == true) {
                    PeriodetypeBarnetilsyn.OPPHØR
                } else {
                    PeriodetypeBarnetilsyn.ORDINÆR
                }
                val nyPeriode =
                    periode.copy(
                        periodetype = periodetype
                    )
                nyPeriode
            } ?: error("Fant ingen perioder")

            vedtak.copy(
                barnetilsyn = BarnetilsynWrapper(
                    perioder = nyePerioder,
                    begrunnelse = vedtak.barnetilsyn.begrunnelse
                )
            )
        }

        logger.info("Fant ${patchetSaker.size} vedtak for patching")

        if (!dryRun) {
            vedtakRepository.updateAll(patchetSaker)
            logger.info("Patchet ${patchetSaker.size} vedtak")
        }
    }
}
