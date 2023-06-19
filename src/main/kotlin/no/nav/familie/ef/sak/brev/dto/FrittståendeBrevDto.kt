package no.nav.familie.ef.sak.brev.dto

import no.nav.familie.ef.sak.brev.BrevmottakereDto
import java.util.UUID

@Deprecated("Skal slettes")
data class FrittståendeBrevDto(
    val overskrift: String,
    val avsnitt: List<FrittståendeBrevAvsnitt>,
    val fagsakId: UUID,
    val brevType: FrittståendeBrevKategori,
    val mottakere: BrevmottakereDto?,
)

@Deprecated("Skal slettes")
data class FrittståendeBrevAvsnitt(val deloverskrift: String, val innhold: String, val skalSkjulesIBrevbygger: Boolean? = false)
