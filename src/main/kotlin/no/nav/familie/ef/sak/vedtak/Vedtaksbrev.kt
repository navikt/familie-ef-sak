package no.nav.familie.ef.sak.vedtak


import no.nav.familie.ef.sak.domene.Fil
import org.springframework.data.annotation.Id
import java.util.UUID

data class Vedtaksbrev(@Id
                       val behandlingId: UUID,
                       val saksbehandlerBrevrequest: String,
                       val brevmal: String,
                       val saksbehandlersignatur: String,
                       val besluttersignatur: String? = null,
                       val beslutterPdf: Fil? = null)
