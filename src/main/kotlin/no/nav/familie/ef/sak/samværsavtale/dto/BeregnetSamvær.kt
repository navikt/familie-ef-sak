package no.nav.familie.ef.sak.samværsavtale.dto

import no.nav.familie.ef.sak.brev.dto.Avsnitt
import java.util.UUID

data class BeregnetSamvær(
    val behandlingBarnId: UUID,
    val uker: List<Avsnitt>,
    val oppsummering: String,
)
