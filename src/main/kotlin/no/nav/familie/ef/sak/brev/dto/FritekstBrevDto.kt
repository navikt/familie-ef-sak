package no.nav.familie.ef.sak.brev.dto

import java.util.UUID

@Deprecated("Skal slettes")
data class FritekstBrevDto(
    val overskrift: String,
    val avsnitt: List<FrittståendeBrevAvsnitt>,
    val behandlingId: UUID,
    val brevType: FritekstBrevKategori,
)
