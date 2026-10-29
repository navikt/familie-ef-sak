package no.nav.familie.ef.sak.oppgave

import io.mockk.mockk
import no.nav.familie.ef.sak.infrastruktur.config.IntegrasjonerConfig
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.kontrakter.felles.oppgave.IdentGruppe
import no.nav.familie.kontrakter.felles.oppgave.OppgaveIdentV2
import no.nav.familie.kontrakter.felles.oppgave.OppgavePrioritet
import no.nav.familie.kontrakter.felles.oppgave.OppgaveResponse
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.oppgave.OpprettOppgaveRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.content
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import java.net.URI
import java.time.LocalDate

internal class OppgaveClientTest {
    @Test
    fun `skal sende nokkelord med eksisterende forespørsel og endepunkt til integrasjoner`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val client =
            OppgaveClient(
                restClient = builder.build(),
                integrasjonerConfig = IntegrasjonerConfig(URI.create("http://integrasjoner")),
                featureToggleService = mockk(),
            )
        server
            .expect(requestTo("http://integrasjoner/api/oppgave/opprett"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(
                content().json(
                    """
                    {
                        "ident": {"ident": "12345678901", "gruppe": "FOLKEREGISTERIDENT"},
                        "saksId": "456",
                        "journalpostId": "789",
                        "tema": "ENF",
                        "oppgavetype": "GodkjenneVedtak",
                        "behandlingstema": "ab0071",
                        "enhetsnummer": "4489",
                        "mappeId": 123,
                        "tilordnetRessurs": "Z123456",
                        "beskrivelse": "Sendt til godkjenning",
                        "prioritet": "HOY",
                        "fristFerdigstillelse": "2026-11-02",
                        "aktivFra": "2026-10-29",
                        "nokkelord": ["EØS", "Kontrollsak"],
                        "behandlesAvApplikasjon": "familie-ef-sak"
                    }
                    """.trimIndent(),
                ),
            ).andRespond(
                withSuccess(jsonMapper.writeValueAsString(Ressurs.success(OppgaveResponse(123L))), MediaType.APPLICATION_JSON),
            )

        val oppgaveId =
            client.opprettOppgave(
                OpprettOppgaveRequest(
                    ident = OppgaveIdentV2("12345678901", IdentGruppe.FOLKEREGISTERIDENT),
                    saksId = "456",
                    journalpostId = "789",
                    tema = Tema.ENF,
                    oppgavetype = Oppgavetype.GodkjenneVedtak,
                    behandlingstema = "ab0071",
                    enhetsnummer = "4489",
                    mappeId = 123,
                    tilordnetRessurs = "Z123456",
                    beskrivelse = "Sendt til godkjenning",
                    prioritet = OppgavePrioritet.HOY,
                    fristFerdigstillelse = LocalDate.of(2026, 11, 2),
                    aktivFra = LocalDate.of(2026, 10, 29),
                    nokkelord = setOf("EØS", "Kontrollsak"),
                    behandlesAvApplikasjon = "familie-ef-sak",
                ),
            )

        assertThat(oppgaveId).isEqualTo(123L)
        server.verify()
    }
}
