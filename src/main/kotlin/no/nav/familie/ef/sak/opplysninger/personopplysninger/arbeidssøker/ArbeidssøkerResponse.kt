package no.nav.familie.ef.sak.opplysninger.personopplysninger.arbeidssøker

import java.time.LocalDateTime
import java.util.UUID

data class ArbeidssøkerResponse(
    val perioder: List<ArbeidssøkerPeriode>,
)

data class ArbeidssøkerPeriode(
    val periodeId: UUID = UUID.randomUUID(),
    val startet: LocalDateWrapper,
    val avsluttet: LocalDateWrapper?,
)

data class LocalDateWrapper(
    val tidspunkt: LocalDateTime,
)
