package no.nav.familie.ef.sak.service.steg

import no.nav.familie.ef.sak.api.Feil
import no.nav.familie.ef.sak.api.beregning.*
import no.nav.familie.ef.sak.blankett.BlankettRepository
import no.nav.familie.ef.sak.repository.domain.Behandling
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service

@Service
class VedtaBlankettSteg(private val vedtakService: VedtakService, private val blankettRepository: BlankettRepository) : BehandlingSteg<VedtakDto> {

    override fun validerSteg(behandling: Behandling) {
    }

    override fun stegType(): StegType {
        return StegType.VEDTA_BLANKETT
    }

    override fun utførSteg(behandling: Behandling, data: VedtakDto) {
        when(data) {
            is Avslå -> {
                vedtakService.slettVedtakHvisFinnes(behandling.id)
                vedtakService.lagreVedtak(vedtakDto = data, behandlingId = behandling.id)
                blankettRepository.deleteById(behandling.id)
            }
            is Innvilget -> {
                vedtakService.slettVedtakHvisFinnes(behandling.id)
                vedtakService.lagreVedtak(vedtakDto = data, behandlingId = behandling.id)
                blankettRepository.deleteById(behandling.id)
            } else -> {
                val feilmelding = "Kan ikke sette vedtaksresultat som $data - ikke implementert"
                throw Feil(feilmelding, feilmelding, HttpStatus.BAD_REQUEST)
            }
        }
    }

}