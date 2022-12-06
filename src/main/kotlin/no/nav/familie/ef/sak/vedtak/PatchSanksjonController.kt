package no.nav.familie.ef.sak.vedtak

import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.vedtak.domain.PeriodeWrapper
import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType
import no.nav.familie.ef.sak.vedtak.dto.ResultatType
import no.nav.security.token.support.core.api.Unprotected
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.transaction.annotation.Transactional
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/sanksjon-patch"], produces = [MediaType.APPLICATION_JSON_VALUE])
@Unprotected
@Validated
class PatchSanksjonController(
    private val vedtakRepository: VedtakRepository

) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    @PostMapping("{dryRun}")
    fun oppdater(@PathVariable dryRun: Boolean) {
        val sankjonsvedtak = vedtakRepository.findAllByResultatType(ResultatType.SANKSJONERE)

        sankjonsvedtak.forEach { vedtak ->
            feilHvis(vedtak.resultatType != ResultatType.SANKSJONERE) {
                "Vedtak behandlingId=${vedtak.behandlingId} er av resultatType=${vedtak.resultatType}"
            }
            feilHvis(vedtak.sanksjonsårsak == null) {
                "Vedtak behandlingId=${vedtak.behandlingId} mangler sanksjonsårsan"
            }
            val perioder = vedtak.perioder?.perioder ?: emptyList()
            val vedtaksperiode = perioder.single()

            feilHvis(vedtaksperiode.periodeType !== VedtaksperiodeType.SANKSJON) {
                "Vedtak behandlingId=${vedtak.behandlingId} inneholder ikke sanksjon"
            }
            logger.info("Oppdaterer behandling=${vedtak.behandlingId} dryRun=$dryRun")

            if (!dryRun) {
                val antallOppdaterte = vedtakRepository.oppdaterPerioder(
                    vedtak.behandlingId,
                    PeriodeWrapper(listOf(vedtaksperiode.copy(sanksjonsårsak = vedtak.sanksjonsårsak)))
                )
                feilHvis(antallOppdaterte != 1) {
                    "Vedtak behandlingId=${vedtak.behandlingId} oppdaterte $antallOppdaterte raderﬁ"
                }
            }
        }
    }
}