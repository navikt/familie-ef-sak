package no.nav.familie.ef.sak.brev.dto

import java.time.LocalDate

data class FrittståendeBrevRequestDto(val overskrift: String,
                                      val avsnitt: List<FrittståendeBrevAvsnitt>,
                                      val brevdato: LocalDate,
                                      val ident: String,
                                      val navn: String)