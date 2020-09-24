package no.nav.familie.ef.sak.api.oppgave

import no.nav.familie.kontrakter.felles.oppgave.Behandlingstema
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

internal class FinnOppgaveRequestDtoTest {

    @Test
    internal fun `skal mappe om finnOppgaveRequestDto til FinnOppgaveRequest`() {
        val finnOppgaveRequest = FinnOppgaveRequestDto(
                behandlingstema = "Overgangsstønad",
                oppgavetype = "Journalføring",
                enhet = "1234",
                saksbehandler = "AB1234",
                journalpostId = "12345",
                tilordnetRessurs = "XY1234",
                tildeltRessurs = true,
                opprettet = LocalDate.of(2020, 1, 1),

                frist = LocalDate.of(2020, 1, 2)
        ).tilFinnOppgaveRequest()

        Assertions.assertThat(finnOppgaveRequest.behandlingstema).isEqualTo(Behandlingstema.Overgangsstønad)
        Assertions.assertThat(finnOppgaveRequest.oppgavetype).isEqualTo(Oppgavetype.Journalføring)
        Assertions.assertThat(finnOppgaveRequest.enhet).isEqualTo("1234")
        Assertions.assertThat(finnOppgaveRequest.saksbehandler).isEqualTo("AB1234")
        Assertions.assertThat(finnOppgaveRequest.journalpostId).isEqualTo("12345")
        Assertions.assertThat(finnOppgaveRequest.tilordnetRessurs).isEqualTo("XY1234")
        Assertions.assertThat(finnOppgaveRequest.tildeltRessurs).isEqualTo(true)
        Assertions.assertThat(finnOppgaveRequest.opprettetFomTidspunkt).isEqualTo(LocalDateTime.of(2020,1,1,0,0,0))
        Assertions.assertThat(finnOppgaveRequest.opprettetTomTidspunkt).isEqualTo(LocalDateTime.of(2020,1,2,0,0,0))
        Assertions.assertThat(finnOppgaveRequest.fristFomDato).isEqualTo(LocalDate.of(2020,1,2))
        Assertions.assertThat(finnOppgaveRequest.fristTomDato).isEqualTo(LocalDate.of(2020,1,2))
        Assertions.assertThat(finnOppgaveRequest.limit).isEqualTo(150)
        Assertions.assertThat(finnOppgaveRequest.offset).isEqualTo(0)


    }
}