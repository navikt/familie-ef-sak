package no.nav.familie.ef.sak.vedtak

import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.repository.findAllByIdOrThrow
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.ef.sak.vedtak.domain.BrevmottakereWrapper
import no.nav.familie.ef.sak.vedtak.domain.Vedtak
import no.nav.familie.ef.sak.vedtak.dto.VedtakDto
import no.nav.familie.ef.sak.vedtak.dto.tilVedtak
import no.nav.familie.ef.sak.vedtak.dto.tilVedtakDto
import no.nav.familie.kontrakter.ef.iverksett.Brevmottaker
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

    fun hentVedtakForBehandlinger(behandlingIder: Set<UUID>): List<Vedtak> {
        return vedtakRepository.findAllByIdOrThrow(behandlingIder) { it.behandlingId }
    }

    fun hentVedtakHvisEksisterer(behandlingId: UUID): VedtakDto? {
        return vedtakRepository.findByIdOrNull(behandlingId)?.tilVedtakDto()
    }

    fun oppdaterSaksbehandler(behandlingId: UUID, saksbehandlerIdent: String) {
        val vedtak = hentVedtak(behandlingId)
        val oppdatertVedtak = vedtak.copy(saksbehandlerIdent = saksbehandlerIdent)
        vedtakRepository.update(oppdatertVedtak)
    }

    fun oppdaterBeslutter(behandlingId: UUID, beslutterIdent: String) {
        val vedtak = hentVedtak(behandlingId)
        val oppdatertVedtak = vedtak.copy(beslutterIdent = beslutterIdent)
        vedtakRepository.update(oppdatertVedtak)
    }

    fun leggTilBrevmottakere(behandlingId: UUID, brevmottakere: List<Brevmottaker>) {
        val vedtak = hentVedtak(behandlingId)

        validerAntallBrevmottakere(brevmottakere)

        vedtakRepository.update(vedtak.copy(brevmottakere = BrevmottakereWrapper(brevmottakere)))
    }

    private fun validerAntallBrevmottakere(brevmottakere: List<Brevmottaker>) {
        feilHvis(brevmottakere.isEmpty()) {
            "Vedtaksbrevet må ha minst 1 mottaker"
        }
        feilHvis(brevmottakere.size > 2) {
            "Vedtaksbrevet kan ikke ha mer enn 2 mottakere"
        }
    }

}
