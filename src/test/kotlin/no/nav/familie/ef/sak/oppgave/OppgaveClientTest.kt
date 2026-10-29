package no.nav.familie.ef.sak.oppgave

import io.mockk.mockk
import no.nav.familie.ef.sak.infrastruktur.config.IntegrasjonerConfig
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.kontrakter.felles.oppgave.OppgaveResponse
import no.nav.familie.kontrakter.felles.oppgave.v2.Arkivreferanse
import no.nav.familie.kontrakter.felles.oppgave.v2.Bruker
import no.nav.familie.kontrakter.felles.oppgave.v2.Brukertype
import no.nav.familie.kontrakter.felles.oppgave.v2.Enhet
import no.nav.familie.kontrakter.felles.oppgave.v2.Kategorisering
import no.nav.familie.kontrakter.felles.oppgave.v2.Kodeverk
import no.nav.familie.kontrakter.felles.oppgave.v2.Mappe
import no.nav.familie.kontrakter.felles.oppgave.v2.Medarbeider
import no.nav.familie.kontrakter.felles.oppgave.v2.OpprettOppgaveV2Request
import no.nav.familie.kontrakter.felles.oppgave.v2.Prioritet
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
    fun `skal sende V2-forespørsel med nokkelord og arkivreferanse til integrasjoner`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val client =
            OppgaveClient(
                restClient = builder.build(),
                integrasjonerConfig = IntegrasjonerConfig(URI.create("http://integrasjoner")),
                featureToggleService = mockk(),
            )
        server
            .expect(requestTo("http://integrasjoner/api/oppgave/v2/opprett"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(
                content().json(
                    """
                    {
                        "kategorisering": {
                            "tema": {"kode": "ENF"},
                            "oppgavetype": {"kode": "GOD_VED"},
                            "behandlingstema": {"kode": "ab0071"}
                        },
                        "fordeling": {
                            "enhet": {"nr": "4489"},
                            "mappe": {"id": 123},
                            "medarbeider": {"navident": "Z123456"}
                        },
                        "beskrivelse": "Sendt til godkjenning",
                        "prioritet": "HOY",
                        "fristDato": "2026-11-02",
                        "aktivDato": "2026-10-29",
                        "nokkelord": ["EØS", "Kontrollsak"],
                        "bruker": {"ident": "12345678901", "type": "PERSON"},
                        "arkivreferanse": {"saksnr": "456", "journalpostId": "789"},
                        "tilknyttetSystem": "familie-ef-sak"
                    }
                    """.trimIndent(),
                ),
            ).andRespond(
                withSuccess(jsonMapper.writeValueAsString(Ressurs.success(OppgaveResponse(123L))), MediaType.APPLICATION_JSON),
            )

        val oppgaveId =
            client.opprettOppgave(
                OpprettOppgaveV2Request(
                    kategorisering =
                        Kategorisering(
                            tema = Kodeverk("ENF"),
                            oppgavetype = Kodeverk("GOD_VED"),
                            behandlingstema = Kodeverk("ab0071"),
                        ),
                    fordeling =
                        OpprettOppgaveV2Request.Fordeling(
                            enhet = Enhet("4489"),
                            mappe = Mappe(123),
                            medarbeider = Medarbeider("Z123456"),
                        ),
                    beskrivelse = "Sendt til godkjenning",
                    prioritet = Prioritet.HOY,
                    fristDato = LocalDate.of(2026, 11, 2),
                    aktivDato = LocalDate.of(2026, 10, 29),
                    nokkelord = setOf("EØS", "Kontrollsak"),
                    bruker = Bruker("12345678901", Brukertype.PERSON),
                    arkivreferanse = Arkivreferanse(saksnr = "456", journalpostId = "789"),
                    tilknyttetSystem = "familie-ef-sak",
                ),
            )

        assertThat(oppgaveId).isEqualTo(123L)
        server.verify()
    }
}
