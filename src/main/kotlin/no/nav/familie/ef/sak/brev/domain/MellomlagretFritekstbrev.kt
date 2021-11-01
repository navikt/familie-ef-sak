package no.nav.familie.ef.sak.brev.domain

import no.nav.familie.ef.sak.brev.dto.FrittståendeBrevAvsnitt
import org.springframework.data.annotation.Id
import java.util.UUID


data class MellomlagretFritekstbrev(@Id val behandlingId: UUID, val brev: Fritekstbrev)

data class Fritekstbrev(val overskrift: String, val avsnitt: List<FrittståendeBrevAvsnitt>)