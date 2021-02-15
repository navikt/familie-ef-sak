package no.nav.familie.ef.sak.repository.domain


import no.nav.familie.ef.sak.api.dto.BrevRequest
import org.springframework.data.annotation.Id
import java.util.*

data class Vedtaksbrev(@Id
                       val behandlingId: UUID,
                       val utkastBrevRequest: BrevRequest,
                       val brevRequest: BrevRequest? = null,
                       val utkastPdf: ByteArray,
                       val pdf: ByteArray? = null)
