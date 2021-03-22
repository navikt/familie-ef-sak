package no.nav.familie.ef.sak.api.beregning

import no.nav.familie.ef.sak.repository.VedtakRepository
import no.nav.familie.ef.sak.repository.domain.InntektWrapper
import no.nav.familie.ef.sak.repository.domain.PeriodeWrapper
import no.nav.familie.ef.sak.repository.domain.Vedtak
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.util.*

@Service
class VedtakService(private val vedtakRepository: VedtakRepository) {

    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    fun lagreVedtak(vedtakRequest: VedtakRequest, behandlingId: UUID): UUID {
        return vedtakRepository.insert(Vedtak(behandlingId,
                                              vedtakRequest.resultatType,
                                              vedtakRequest.periodeBegrunnelse,
                                              vedtakRequest.inntektBegrunnelse,
                                              PeriodeWrapper(vedtakRequest.perioder),
                                              InntektWrapper(vedtakRequest.inntekter))).behandlingId
    }

    fun slettVedtakHvisFinnes(behandlingId: UUID) {
        vedtakRepository.findByIdOrNull(behandlingId)?.let {
            logger.info("Sletter vedtak for behandling=${behandlingId}")
            vedtakRepository.deleteById(behandlingId)
        }
    }
}
