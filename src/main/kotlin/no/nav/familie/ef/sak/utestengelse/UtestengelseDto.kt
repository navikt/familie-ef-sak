package no.nav.familie.ef.sak.utestengelse

import no.nav.familie.kontrakter.felles.Månedsperiode
import java.time.LocalDateTime
import java.util.UUID

data class UtestengelseDto(
    val id: UUID,
    val periode: Månedsperiode,
    val slettet: Boolean,
    val opprettetAv: String,
    val opprettetTid: LocalDateTime,
    val endretAv: String,
    val endretTid: LocalDateTime,
)

data class OpprettUtestengelseDto(
    val fagsakPersonId: UUID,
    val periode: Månedsperiode,
)
