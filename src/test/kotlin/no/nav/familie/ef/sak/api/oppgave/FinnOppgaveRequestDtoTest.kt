package no.nav.familie.ef.sak.api.oppgave

import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppgave.Behandlingstema
import no.nav.familie.kontrakter.felles.oppgave.Enhetsmappe
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

internal class FinnOppgaveRequestDtoTest {

    @Test
    internal fun `skal mappe om finnOppgaveRequestDto til FinnOppgaveRequest`() {
        val eksternInput = HashMap<String, String>()
        eksternInput["behandlingstema"] = "ab0071"
        eksternInput["oppgavetype"] = "JFR"
        eksternInput["enhet"] = "1234"
        eksternInput["saksbehandler"] = "AB1234"
        eksternInput["journalpostId"] = "12345"
        eksternInput["tilordnetRessurs"] = "XY1234"
        eksternInput["tildeltRessurs"] = "true"
        eksternInput["enhetsmappe"] = "100000035"
        eksternInput["opprettetFom"] = LocalDate.of(2020, 1, 1).toString()
        eksternInput["opprettetTom"] = LocalDate.of(2020, 1, 2).toString()
        eksternInput["fristFom"] = LocalDate.of(2020, 1, 2).toString()
        eksternInput["fristTom"] = LocalDate.of(2020, 1, 2).toString()

        val finnOppgaveRequestDto =
                objectMapper.readValue(objectMapper.writeValueAsString(eksternInput), FinnOppgaveRequestDto::class.java)
        val finnOppgaveRequest = finnOppgaveRequestDto.tilFinnOppgaveRequest()

        Assertions.assertThat(finnOppgaveRequest.behandlingstema).isEqualTo(Behandlingstema.Overgangsstønad)
        Assertions.assertThat(finnOppgaveRequest.oppgavetype).isEqualTo(Oppgavetype.Journalføring)
        Assertions.assertThat(finnOppgaveRequest.enhet).isEqualTo("1234")
        Assertions.assertThat(finnOppgaveRequest.saksbehandler).isEqualTo("AB1234")
        Assertions.assertThat(finnOppgaveRequest.journalpostId).isEqualTo("12345")
        Assertions.assertThat(finnOppgaveRequest.tilordnetRessurs).isEqualTo("XY1234")
        Assertions.assertThat(finnOppgaveRequest.tildeltRessurs).isEqualTo(true)
        Assertions.assertThat(finnOppgaveRequest.opprettetFomTidspunkt).isEqualTo(LocalDateTime.of(2020, 1, 1, 0, 0, 0))
        Assertions.assertThat(finnOppgaveRequest.opprettetTomTidspunkt).isEqualTo(LocalDateTime.of(2020, 1, 3, 0, 0, 0))
        Assertions.assertThat(finnOppgaveRequest.fristFomDato).isEqualTo(LocalDate.of(2020, 1, 2))
        Assertions.assertThat(finnOppgaveRequest.fristTomDato).isEqualTo(LocalDate.of(2020, 1, 2))
        Assertions.assertThat(finnOppgaveRequest.limit).isEqualTo(150)
        Assertions.assertThat(finnOppgaveRequest.offset).isEqualTo(0)
        Assertions.assertThat(finnOppgaveRequest.enhetsmappe).isEqualTo(Enhetsmappe.SøknaderKlarTilBehandling)


    }
}