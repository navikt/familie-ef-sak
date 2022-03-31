package no.nav.familie.ef.sak.iverksett.oppgaveterminbarn

import nonapi.io.github.classgraph.json.Id
import org.springframework.data.relational.core.mapping.Column
import java.time.LocalDate
import java.util.UUID

data class TerminbarnOppgave(@Id
                             val fagsakId: UUID,
                             val termindato: LocalDate,
                             val opprettetTid: LocalDate = LocalDate.now())