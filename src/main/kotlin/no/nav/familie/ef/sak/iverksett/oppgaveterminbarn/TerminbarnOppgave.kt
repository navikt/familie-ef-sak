package no.nav.familie.ef.sak.iverksett.oppgaveterminbarn

import org.springframework.data.annotation.Id
import java.time.LocalDate
import java.util.UUID

data class TerminbarnOppgave(
    @Id
    val fagsakId: UUID,
    val termindato: LocalDate,
    val opprettetTid: LocalDate = LocalDate.now(),
)
