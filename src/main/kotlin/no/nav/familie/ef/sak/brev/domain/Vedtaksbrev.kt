package no.nav.familie.ef.sak.brev.domain


import no.nav.familie.ef.sak.brev.dto.VedtaksbrevDto
import no.nav.familie.ef.sak.felles.domain.Fil
import org.springframework.data.annotation.Id
import java.util.UUID

data class Vedtaksbrev(@Id
                       val behandlingId: UUID,
                       val saksbehandlerBrevrequest: String,
                       val brevmal: String,
                       val saksbehandlersignatur: String,
                       val besluttersignatur: String? = null,
                       val beslutterPdf: Fil? = null,
                       val enhet: String? = null,
                       val saksbehandlerident: String,
                       val beslutterident: String? = null) {


}

fun Vedtaksbrev.tilDto(skjulBeslutterSignatur: Boolean): VedtaksbrevDto = VedtaksbrevDto(saksbehandlerBrevrequest = this.saksbehandlerBrevrequest,
                                                                                         brevmal = this.brevmal,
                                                                                         saksbehandlersignatur = this.saksbehandlersignatur,
                                                                                         besluttersignatur = this.besluttersignatur,
                                                                                         enhet = this.enhet,
                                                                                         skjulBeslutterSignatur = skjulBeslutterSignatur
)

const val FRITEKST = "fritekst"