package no.nav.familie.ef.sak.api.beregning

import no.nav.familie.ef.sak.repository.VedtakRepository
import no.nav.familie.ef.sak.repository.domain.Vedtak
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*

@Service
class VedtakService(private val vedtakRepository: VedtakRepository) {
    fun lagreVedtak(vedtakRequest: VedtakRequest, behandlingId: UUID): UUID {
        return vedtakRepository.insert(Vedtak(behandlingId, vedtakRequest.resultatType, vedtakRequest.periodeBegrunnelse, vedtakRequest.inntektBegrunnelse, vedtakRequest.perioder, vedtakRequest.inntekter)).behandlingId
    }
}
