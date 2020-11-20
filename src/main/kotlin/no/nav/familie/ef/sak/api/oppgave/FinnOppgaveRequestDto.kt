package no.nav.familie.ef.sak.api.oppgave

import no.nav.familie.kontrakter.felles.oppgave.*
import java.time.LocalDate

data class FinnOppgaveRequestDto(val behandlingstema: String? = null,
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
                                 val ident: String?) {

    fun tilFinnOppgaveRequest(aktørid: String? = null): FinnOppgaveRequest =
            FinnOppgaveRequest(tema = Tema.ENF,
                               behandlingstema = if (this.behandlingstema != null) Behandlingstema.values()
                                       .find { it.value == this.behandlingstema } else null,
                               oppgavetype = if (this.oppgavetype != null) Oppgavetype.values()
                                       .find { it.value == this.oppgavetype } else null,
                               enhet = this.enhet,
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
                               enhetsmappe = if (this.enhetsmappe != null) Enhetsmappe.values()
                                       .find { it.value == this.enhetsmappe } else null,
                               limit = 150,
                               offset = 0)
}
