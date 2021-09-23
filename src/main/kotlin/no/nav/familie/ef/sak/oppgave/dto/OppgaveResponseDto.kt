package no.nav.familie.ef.sak.oppgave.dto

data class OppgaveResponseDto(val antallTreffTotalt: Long,
                              val oppgaver: List<OppgaveEfDto>)