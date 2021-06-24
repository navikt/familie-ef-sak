package no.nav.familie.ef.sak.api.beregning

import no.nav.familie.ef.sak.repository.VedtakRepository
import no.nav.familie.ef.sak.repository.domain.Vedtak
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class VedtakService(private val vedtakRepository: VedtakRepository) {

    fun lagreVedtak(vedtakDto: VedtakDto, behandlingId: UUID): UUID {
        return vedtakRepository.insert(vedtakDto.tilVedtak(behandlingId)).behandlingId
    }

    fun slettVedtakHvisFinnes(behandlingId: UUID) {
        vedtakRepository.findByIdOrNull(behandlingId)
                ?.let { vedtakRepository.deleteById(behandlingId) }
    }

    fun hentVedtak(behandlingId: UUID): Vedtak {
        return vedtakRepository.findByIdOrThrow(behandlingId)
    }

    fun hentVedtakHvisEksisterer(behandlingId: UUID): VedtakDto? {
        return vedtakRepository.findByIdOrNull(behandlingId)
                ?.tilVedtakDto()
    }

    fun oppdaterSaksbehandler(behandlingId: UUID, saksbehandlerIdent: String) {
        val vedtak = hentVedtak(behandlingId)
        val opdatertVedtak = vedtak.copy(saksbehandlerIdent = saksbehandlerIdent)
        vedtakRepository.update(opdatertVedtak)
    }

    fun oppdaterBeslutter(behandlingId: UUID, beslutterIdent: String) {
        val vedtak = hentVedtak(behandlingId)
        val opdatertVedtak = vedtak.copy(beslutterIdent = beslutterIdent)
        vedtakRepository.update(opdatertVedtak)
    }
}
