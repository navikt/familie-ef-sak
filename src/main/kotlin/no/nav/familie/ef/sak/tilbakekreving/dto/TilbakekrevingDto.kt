package no.nav.familie.ef.sak.tilbakekreving.dto

import no.nav.familie.ef.sak.tilbakekreving.domain.Tilbakekreving
import no.nav.familie.ef.sak.tilbakekreving.domain.Tilbakekrevingsvalg
import java.util.UUID

data class TilbakekrevingDto(val valg: Tilbakekrevingsvalg,
                             val varseltekst: String? = null,
                             var begrunnelse: String,
                             val behandlingFinnes: Boolean)


fun TilbakekrevingDto.tilDomene(behandlingId: UUID) = Tilbakekreving(
        behandlingId = behandlingId,
        valg = this.valg,
        varseltekst = this.varseltekst,
        begrunnelse = this.begrunnelse
)