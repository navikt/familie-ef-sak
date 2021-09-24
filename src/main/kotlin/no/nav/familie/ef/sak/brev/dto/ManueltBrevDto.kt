package no.nav.familie.ef.sak.brev.dto

data class ManueltBrevDto(val overskrift: String,
                          val avsnitt: List<ManueltBrevAvsnitt>,
                          val fagsakId: String?,
                          val behandlingId: String?)

data class ManueltBrevAvsnitt(val deloverskrift: String, val innhold: String)


