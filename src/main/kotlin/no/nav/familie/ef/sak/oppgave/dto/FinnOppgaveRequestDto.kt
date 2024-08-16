package no.nav.familie.ef.sak.oppgave.dto

import no.nav.familie.kontrakter.felles.Behandlingstema
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveRequest
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import java.time.LocalDate
import no.nav.familie.kontrakter.felles.oppgave.Sorteringsfelt
import no.nav.familie.kontrakter.felles.oppgave.Sorteringsrekkefølge

data class FinnOppgaveRequestDto(
    val behandlingstema: String? = null,
    val oppgavetype: String? = null,
    val enhet: String? = null,
    val saksbehandler: String? = null,
    val journalpostId: String? = null,
    val tilordnetRessurs: String? = null,
    val tildeltRessurs: Boolean? = null,
    val opprettetFom: LocalDate? = null,
    val opprettetTom: LocalDate? = null,
    val fristFom: LocalDate? = null,
    val fristTom: LocalDate? = null,
    val enhetsmappe: Long? = null,
    val mappeId: Long? = null,
    val erUtenMappe: Boolean? = null,
    val ident: String?,
) {
    fun tilFinnOppgaveRequest(aktørid: String? = null): FinnOppgaveRequest =
        FinnOppgaveRequest(
            tema = Tema.ENF,
            behandlingstema =
                if (this.behandlingstema != null) {
                    Behandlingstema.values().find { it.value == this.behandlingstema }
                } else {
                    null
                },
            oppgavetype =
                if (this.oppgavetype != null) {
                    Oppgavetype.values().find { it.value == this.oppgavetype }
                } else {
                    null
                },
            enhet = this.enhet,
            erUtenMappe = this.erUtenMappe,
            saksbehandler = this.saksbehandler,
            aktørId = aktørid,
            journalpostId = this.journalpostId,
            tildeltRessurs = this.tildeltRessurs,
            tilordnetRessurs = this.tilordnetRessurs,
            opprettetFomTidspunkt = this.opprettetFom?.atStartOfDay(),
            opprettetTomTidspunkt = this.opprettetTom?.plusDays(1)?.atStartOfDay(),
            fristFomDato = this.fristFom,
            fristTomDato = this.fristTom,
            aktivFomDato = null,
            aktivTomDato = null,
            mappeId = this.mappeId,
            limit = 300,
            offset = 0,
            sorteringsfelt = Sorteringsfelt.FRIST,
            sorteringsrekkefølge = Sorteringsrekkefølge.ASC,
        )
}
