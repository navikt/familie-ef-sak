package no.nav.familie.ef.sak.brev.domain

import no.nav.familie.ef.sak.brev.dto.FrittståendeBrevAvsnitt

data class Fritekstbrev(val overskrift: String, val avsnitt: List<FrittståendeBrevAvsnitt>)