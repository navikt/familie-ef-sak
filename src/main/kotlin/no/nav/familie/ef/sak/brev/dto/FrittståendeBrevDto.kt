package no.nav.familie.ef.sak.brev.dto

import no.nav.familie.kontrakter.ef.felles.FrittståendeBrevType
import no.nav.familie.kontrakter.ef.felles.StønadType
import java.util.UUID

data class FrittståendeBrevDto(val overskrift: String,
                               val avsnitt: List<FrittståendeBrevAvsnitt>,
                               val fagsakId: UUID,
                               val stønadType: StønadType,
                               val brevType: FrittståendeBrevType)

data class FrittståendeBrevAvsnitt(val deloverskrift: String, val innhold: String)

data class VedtaksbrevFritekstDto(val overskrift: String,
                                  val avsnitt: List<FrittståendeBrevAvsnitt>,
                                  val behandlingId: UUID)

