package no.nav.familie.ef.sak.repository.domain


import no.nav.familie.ef.sak.api.dto.BrevRequest
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import java.util.*

data class Vedtaksbrev(@Id
                       val behandlingId: UUID,
                       val utkastBrevRequest: BrevRequest,
                       val brevRequest: BrevRequest? = null,
                       @Column("utkast_pdf")
                       val utkastPdf: Fil,
                       @Column("pdf")
                       val pdf: Fil? = null)
