package no.nav.familie.ef.sak.service.steg

import no.nav.familie.ef.sak.api.Feil
import no.nav.familie.ef.sak.api.beregning.ResultatType
import no.nav.familie.ef.sak.api.beregning.VedtakDto
import no.nav.familie.ef.sak.api.beregning.VedtakService
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
        if (data.resultatType != ResultatType.INNVILGE) {
            val feilmelding = "Kan ikke sette vedtaksresultat som ${data.resultatType} - ikke implementert"
            throw Feil(feilmelding, feilmelding, HttpStatus.BAD_REQUEST)
        }

        vedtakService.slettVedtakHvisFinnes(behandling.id)
        vedtakService.lagreVedtak(vedtak = data, behandlingId = behandling.id)
        blankettRepository.deleteById(behandling.id)

    }

}