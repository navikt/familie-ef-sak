package no.nav.familie.ef.sak.brev.dto

import java.util.UUID

data class FrittståendeBrevDto(val overskrift: String,
                               val avsnitt: List<FrittståendeBrevAvsnitt>,
                               val fagsakId: UUID,
                               val brevType: FrittståendeBrevKategori)

data class FrittståendeBrevAvsnitt(val deloverskrift: String, val innhold: String, val skalSkjulesIBrevbygger: Boolean? = false)


