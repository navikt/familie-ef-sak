package no.nav.familie.ef.sak.no.nav.familie.ef.sak.config

import io.mockk.*
import no.nav.familie.ef.sak.integration.OppgaveClient
import no.nav.familie.kontrakter.felles.oppgave.*
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import java.lang.IllegalStateException
import java.time.LocalDate


@Configuration
class OppgaveClientMock() {

    @Bean
    @Primary
    fun oppgaveClient(): OppgaveClient {
        val oppgaveClient: OppgaveClient = mockk()

        every {
            oppgaveClient.hentOppgaver(any())
        } returns FinnOppgaveResponseDto(3, listOf(oppgave1, oppgave2, oppgave3))

        every { oppgaveClient.finnOppgaveMedId(any()) } returns oppgave1

        every { oppgaveClient.opprettOppgave(any()) } returns 12345678L

        every { oppgaveClient.fordelOppgave(any(), any()) } returns 12345678L

        every { oppgaveClient.ferdigstillOppgave(any()) } just Runs
        
        return oppgaveClient
    }

    private val oppgave1 = lagOppgave(1L, Oppgavetype.Journalføring, "Z999999")
    private val oppgave2 = lagOppgave(2L, Oppgavetype.BehandleSak, "Z999999")
    private val oppgave3 = lagOppgave(3L, Oppgavetype.Journalføring, beskivelse = "")


    private fun lagOppgave(oppgaveId: Long,
                           oppgavetype: Oppgavetype,
                           tildeltRessurs: String? = null,
                           beskivelse: String? = "Beskrivelse av oppgaven. Denne teksten kan jo være lang, kort eller ikke inneholde noenting. "): Oppgave {
        return Oppgave(id = oppgaveId,
                       aktoerId = "1234",
                       identer = listOf(OppgaveIdentV2("11111111111", IdentGruppe.FOLKEREGISTERIDENT)),
                       journalpostId = "1234",
                       tildeltEnhetsnr = "4408",
                       tilordnetRessurs = tildeltRessurs,
                       mappeId = 100000035,
                       behandlesAvApplikasjon = "FS22",
                       beskrivelse = beskivelse,
                       tema = Tema.ENF,
                       behandlingstema = "ab0071",
                       oppgavetype = oppgavetype.value,
                       opprettetTidspunkt = LocalDate.of(
                               2020,
                               1,
                               1).toString(),
                       fristFerdigstillelse = LocalDate.of(
                               2020,
                               2,
                               1).toString(),
                       prioritet = OppgavePrioritet.NORM,
                       status = StatusEnum.OPPRETTET
        )
    }
}