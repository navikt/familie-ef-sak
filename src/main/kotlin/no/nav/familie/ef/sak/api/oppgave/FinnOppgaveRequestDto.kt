package no.nav.familie.ef.sak.api.oppgave

import no.nav.familie.kontrakter.felles.oppgave.Behandlingstema
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveRequest
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.oppgave.Tema
import java.time.LocalDate

data class FinnOppgaveRequestDto(
        val behandlingstema: String? = null,
        val oppgavetype: String? = null,
        val enhet: String? = null,
        val saksbehandler: String? = null,
        val journalpostId: String? = null,
        val tilordnetRessurs: String? = null,
        val tildeltRessurs: Boolean? = null,
        val opprettet: LocalDate? = null,
        val frist: LocalDate? = null) {

    fun tilFinnOppgaveRequest(): FinnOppgaveRequest = FinnOppgaveRequest(
            tema = Tema.ENF,
            behandlingstema = if (this.behandlingstema != null) Behandlingstema.values()
                    .find { it.name.equals(behandlingstema, true) } else null,
            oppgavetype = if (this.oppgavetype != null) Oppgavetype.values()
                    .find { it.name.equals(this.oppgavetype, true) } else null,
            enhet = this.enhet,
            saksbehandler = this.saksbehandler,
            journalpostId = this.journalpostId,
            tildeltRessurs = this.tildeltRessurs,
            tilordnetRessurs = this.tilordnetRessurs,
            opprettetFomTidspunkt = this.opprettet?.atStartOfDay(),
            opprettetTomTidspunkt = this.opprettet?.plusDays(1)?.atStartOfDay(),
            fristFomDato = this.frist,
            fristTomDato = this.frist,
            aktivFomDato = null,
            aktivTomDato = null,
            limit = 150,
            offset = 0)
}