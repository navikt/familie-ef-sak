package no.nav.familie.ef.sak.repository.domain


import org.springframework.data.annotation.Id
import java.util.*

data class Vedtaksbrev(@Id
                       val behandlingId: UUID,
                       val saksbehandlerBrevrequest: String,
                       val brevmal: String,
                       val beslutterPdf: Fil? = null,
                       val saksbehandlersignatur: String,
                       val besluttersignatur: String? = null)
