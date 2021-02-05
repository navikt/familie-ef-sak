package no.nav.familie.ef.sak.repository.domain


import no.nav.familie.ef.sak.api.dto.BrevRequest
import no.nav.familie.ef.sak.service.steg.StegType
import org.springframework.data.annotation.Id
import java.util.*

data class Vedtaksbrev(@Id
                       val id: UUID = UUID.randomUUID(),
                       val behandlingId: UUID,
                       val steg: StegType,
                       val brevRequest: BrevRequest,
                       val pdf: ByteArray)
