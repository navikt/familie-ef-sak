package no.nav.familie.ef.sak.repository.domain


import org.springframework.data.annotation.Id
import java.util.UUID

data class Vedtaksbrev(@Id
                       val behandlingId: UUID,
                       val saksbehandlerBrevrequest: String,
                       val brevmal: String,
                       val saksbehandlersignatur: String,
                       val besluttersignatur: String? = null,
                       val beslutterPdf: Fil? = null)
