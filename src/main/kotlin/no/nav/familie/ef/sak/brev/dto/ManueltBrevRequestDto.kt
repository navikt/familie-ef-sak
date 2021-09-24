package no.nav.familie.ef.sak.brev.dto

import java.time.LocalDate

data class ManueltBrevRequestDto(val overskrift: String,
                                 val avsnitt: List<ManueltBrevAvsnitt>,
                                 val brevdato: LocalDate,
                                 val ident: String,
                                 val navn: String)