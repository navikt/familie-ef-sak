package no.nav.familie.ef.sak.brev.dto

import java.util.UUID

data class FrittståendeBrevDto(val overskrift: String,
                               val avsnitt: List<FrittståendeBrevAvsnitt>,
                               val fagsakId: UUID)

data class FrittståendeBrevAvsnitt(val deloverskrift: String, val innhold: String)

data class VedtaksbrevFritekstDto(val overskrift: String,
                                  val avsnitt: List<FrittståendeBrevAvsnitt>,
                                  val behandlingId: UUID)

