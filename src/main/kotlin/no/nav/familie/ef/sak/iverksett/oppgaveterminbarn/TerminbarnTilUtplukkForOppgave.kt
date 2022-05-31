package no.nav.familie.ef.sak.iverksett.oppgaveterminbarn

import java.time.LocalDate
import java.util.UUID

data class TerminbarnTilUtplukkForOppgave(
    val behandlingId: UUID,
    val fagsakId: UUID,
    val eksternFagsakId: Long,
    val termindatoBarn: LocalDate
)
