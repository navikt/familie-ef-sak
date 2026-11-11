package no.nav.familie.ef.sak.oppgave

import io.mockk.mockk
import no.nav.familie.ef.sak.infrastruktur.config.IntegrasjonerConfig
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.kontrakter.felles.oppgave.IdentGruppe
import no.nav.familie.kontrakter.felles.oppgave.OppdaterOppgaveNokkelordRequest
import no.nav.familie.kontrakter.felles.oppgave.OppgaveIdentV2
import no.nav.familie.kontrakter.felles.oppgave.OppgavePrioritet
import no.nav.familie.kontrakter.felles.oppgave.OppgaveResponse
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.oppgave.OpprettOppgaveRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.json.JsonCompareMode
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.content
import org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import java.net.URI
import java.time.LocalDate

internal class OppgaveClientTest {
    private val builder = RestClient.builder()
    private val server = MockRestServiceServer.bindTo(builder).build()
    private val client =
        OppgaveClient(
            restClient = builder.build(),
            integrasjonerConfig = IntegrasjonerConfig(URI.create("http://integrasjoner")),
            featureToggleService = mockk(),
        )

    @Test
    fun `skal opprette oppgave via eksisterende endepunkt uten nokkelord`() {
        server
            .expect(requestTo("http://integrasjoner/api/oppgave/opprett"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.nokkelord").doesNotExist())
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
                    behandlesAvApplikasjon = "familie-ef-sak",
                ),
            )

        assertThat(oppgaveId).isEqualTo(123L)
        server.verify()
    }

    @Test
    fun `skal patche kun nokkelord og gjeldende versjon`() {
        server
            .expect(requestTo("http://integrasjoner/api/oppgave/123/nokkelord"))
            .andExpect(method(HttpMethod.PATCH))
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(content().json("""{"nokkelord":["EØS","Kontrollsak"],"meta":{"versjon":7}}""", JsonCompareMode.STRICT))
            .andRespond(withSuccess(jsonMapper.writeValueAsString(Ressurs.success(OppgaveResponse(123L))), MediaType.APPLICATION_JSON))

        client.oppdaterOppgaveNokkelord(
            123L,
            OppdaterOppgaveNokkelordRequest(setOf("EØS", "Kontrollsak"), OppdaterOppgaveNokkelordRequest.Meta(7)),
        )

        server.verify()
    }

    @Test
    fun `skal sende tomt nokkelordsett for å fjerne nokkelord`() {
        server
            .expect(requestTo("http://integrasjoner/api/oppgave/123/nokkelord"))
            .andExpect(method(HttpMethod.PATCH))
            .andExpect(content().json("""{"nokkelord":[],"meta":{"versjon":7}}""", JsonCompareMode.STRICT))
            .andRespond(withSuccess(jsonMapper.writeValueAsString(Ressurs.success(OppgaveResponse(123L))), MediaType.APPLICATION_JSON))

        client.oppdaterOppgaveNokkelord(
            123L,
            OppdaterOppgaveNokkelordRequest(emptySet(), OppdaterOppgaveNokkelordRequest.Meta(7)),
        )

        server.verify()
    }

    @ParameterizedTest
    @ValueSource(ints = [409, 500])
    fun `skal sende patchfeil videre uten nye kall`(status: Int) {
        server
            .expect(requestTo("http://integrasjoner/api/oppgave/123/nokkelord"))
            .andExpect(method(HttpMethod.PATCH))
            .andRespond(withStatus(HttpStatus.valueOf(status)))

        val feil =
            assertThrows<RestClientResponseException> {
                client.oppdaterOppgaveNokkelord(
                    123L,
                    OppdaterOppgaveNokkelordRequest(setOf("EØS"), OppdaterOppgaveNokkelordRequest.Meta(7)),
                )
            }

        assertThat(feil.statusCode.value()).isEqualTo(status)
        server.verify()
    }
}
