package no.nav.familie.ef.sak.brev.domain

import no.nav.familie.ef.sak.felles.domain.Fil
import org.springframework.data.annotation.Id
import java.time.LocalDateTime
import java.util.UUID

data class Vedtaksbrev(
    @Id
    val behandlingId: UUID,
    val saksbehandlerHtml: String? = null,
    val brevmal: String,
    val saksbehandlersignatur: String,
    val besluttersignatur: String? = null,
    val beslutterPdf: Fil? = null,
    val enhet: String? = null,
    val saksbehandlerident: String,
    val beslutterident: String? = null,
    val opprettetTid: LocalDateTime? = null,
    val besluttetTid: LocalDateTime? = null
)

const val FRITEKST = "fritekst"
