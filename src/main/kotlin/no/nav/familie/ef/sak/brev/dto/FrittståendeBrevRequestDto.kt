package no.nav.familie.ef.sak.brev.dto

data class FrittståendeBrevRequestDto(val overskrift: String,
                                      val avsnitt: List<FrittståendeBrevAvsnitt>,
                                      val personIdent: String,
                                      val navn: String)