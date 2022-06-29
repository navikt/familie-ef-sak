package no.nav.familie.ef.sak.no.nav.familie.ef.sak.oppgave

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.oppgave.dto.FinnOppgaveRequestDto
import no.nav.familie.ef.sak.oppgave.dto.OppgaveResponseDto
import no.nav.familie.kontrakter.felles.Ressurs
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.web.client.exchange
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity

internal class OppgaveControllerIntegrasjonsTest : OppslagSpringRunnerTest() {

    @BeforeEach
    fun setUp() {
        headers.setBearerAuth(lokalTestToken)
    }

    @Test
    internal fun `Skal feile hvis personIdenten ikke finnes i pdl`() {
        val response = søkOppgave("19117313797")
        assertThat(response.body.status).isEqualTo(Ressurs.Status.FUNKSJONELL_FEIL)
        assertThat(response.body.frontendFeilmelding).isEqualTo("Finner ingen personer for valgt personident")
    }

    private fun søkOppgave(personIdent: String): ResponseEntity<Ressurs<OppgaveResponseDto>> {
        return restTemplate.exchange(
            localhost("/api/oppgave/soek/"),
            HttpMethod.POST,
            HttpEntity(FinnOppgaveRequestDto(ident = personIdent), headers)
        )
    }
}
