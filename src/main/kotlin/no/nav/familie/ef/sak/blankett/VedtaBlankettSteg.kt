package no.nav.familie.ef.sak.blankett

import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.behandlingsflyt.steg.BehandlingSteg
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vedtak.dto.Avslå
import no.nav.familie.ef.sak.vedtak.dto.Innvilget
import no.nav.familie.ef.sak.vedtak.dto.VedtakDto
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service

@Service
class VedtaBlankettSteg(private val vedtakService: VedtakService, private val blankettRepository: BlankettRepository) :
        BehandlingSteg<VedtakDto> {

    override fun validerSteg(saksbehandling: Saksbehandling) {
    }

    override fun stegType(): StegType {
        return StegType.VEDTA_BLANKETT
    }

    override fun utførSteg(saksbehandling: Saksbehandling, data: VedtakDto) {
        when (data) {
            is Innvilget, is Avslå -> {
                vedtakService.slettVedtakHvisFinnes(saksbehandling.id)
                vedtakService.lagreVedtak(vedtakDto = data, behandlingId = saksbehandling.id)
                blankettRepository.deleteById(saksbehandling.id)
            }
            else -> {
                val feilmelding = "Kan ikke sette vedtaksresultat som $data - ikke implementert"
                throw Feil(feilmelding, feilmelding, HttpStatus.BAD_REQUEST)
            }
        }
    }

}