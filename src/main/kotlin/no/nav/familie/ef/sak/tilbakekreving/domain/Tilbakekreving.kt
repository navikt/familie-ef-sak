package no.nav.familie.ef.sak.tilbakekreving.domain

import no.nav.familie.ef.sak.felles.domain.Sporbar
import no.nav.familie.ef.sak.tilbakekreving.dto.TilbakekrevingDto
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table
import java.util.UUID

@Table
data class Tilbakekreving(@Id
                          val behandlingId: UUID,
                          val valg: Tilbakekrevingsvalg,
                          val varseltekst: String? = null,
                          val begrunnelse: String,
                          @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                          val sporbar: Sporbar = Sporbar())

enum class Tilbakekrevingsvalg {
    OPPRETT_MED_VARSEL,
    OPPRETT_UTEN_VARSEL,
    AVVENT
}

fun Tilbakekreving.tilDto() = TilbakekrevingDto(valg = this.valg, varseltekst = this.varseltekst, begrunnelse = this.begrunnelse)