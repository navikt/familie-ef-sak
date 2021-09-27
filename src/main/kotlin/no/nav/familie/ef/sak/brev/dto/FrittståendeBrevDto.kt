package no.nav.familie.ef.sak.brev.dto

data class FrittståendeBrevDto(val overskrift: String,
                               val avsnitt: List<FrittståendeBrevAvsnitt>,
                               val fagsakId: String)

data class FrittståendeBrevAvsnitt(val deloverskrift: String, val innhold: String)

data class VedtaksbrevFritekstDto(val overskrift: String,
                                  val avsnitt: List<FrittståendeBrevAvsnitt>,
                                  val behandlingId: String)

