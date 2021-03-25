package no.nav.familie.ef.sak.api.beregning

import no.nav.familie.ef.sak.repository.VedtakRepository
import no.nav.familie.ef.sak.repository.domain.InntektWrapper
import no.nav.familie.ef.sak.repository.domain.PeriodeWrapper
import no.nav.familie.ef.sak.repository.domain.Vedtak
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.util.*

@Service
class VedtakService(private val vedtakRepository: VedtakRepository) {

    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    fun lagreVedtak(vedtak: VedtakDto, behandlingId: UUID): UUID {
        return vedtakRepository.insert(Vedtak(behandlingId,
                                              vedtak.resultatType,
                                              vedtak.periodeBegrunnelse,
                                              vedtak.inntektBegrunnelse,
                                              PeriodeWrapper(vedtak.perioder),
                                              InntektWrapper(vedtak.inntekter))).behandlingId
    }

    fun slettVedtakHvisFinnes(behandlingId: UUID) {
        vedtakRepository.findByIdOrNull(behandlingId)?.let {
            logger.info("Sletter vedtak for behandling=${behandlingId}")
            vedtakRepository.deleteById(behandlingId)
        }
    }

    fun hentVedtak(behandlingId: UUID): Vedtak {
        return vedtakRepository.findByIdOrThrow(behandlingId)
    }

    fun hentVedtakHvisEksisterer(behandlingId: UUID): VedtakDto? {
        return vedtakRepository.findByIdOrNull(behandlingId)
                ?.let { logger.info(it.toString())
                    return VedtakDto(it.resultatType, it.periodeBegrunnelse, it.inntektBegrunnelse, it.perioder.perioder) }
    }
}
