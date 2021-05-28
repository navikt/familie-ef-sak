package no.nav.familie.ef.sak.repository.domain


import org.springframework.data.annotation.Id
import java.util.*

data class Vedtaksbrev(@Id
                       val behandlingId: UUID,
                       val saksbehandlerEnBrevRequest: String,
                       val brevmal: String,
                       val beslutterPdf: Fil? = null)
