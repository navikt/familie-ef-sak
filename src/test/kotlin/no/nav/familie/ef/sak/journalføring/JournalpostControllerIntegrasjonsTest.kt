package no.nav.familie.ef.sak.journalføring

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.journalføring.dto.JournalføringRequestV2
import no.nav.familie.ef.sak.journalføring.dto.Journalføringsaksjon
import no.nav.familie.ef.sak.journalføring.dto.Journalføringsårsak
import no.nav.familie.kontrakter.felles.Ressurs
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.resttestclient.exchange
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.util.UUID

internal class JournalpostControllerIntegrasjonsTest : OppslagSpringRunnerTest() {
    @Test
    internal fun `Skal returnere 403 FORBIDDEN med status IKKE_TILGANG dersom bruker kun har veilederrolle`() {
        headers.setBearerAuth(lokalVeilederToken)

        val response = fullførJournalpostV2("111")

        assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
        assertThat(response.body?.status).isEqualTo(Ressurs.Status.IKKE_TILGANG)
    }

    private fun fullførJournalpostV2(journalpostId: String): ResponseEntity<Ressurs<String>> =
        testRestTemplate.exchange(
            localhost("/api/journalpost/$journalpostId/fullfor/v2"),
            HttpMethod.POST,
            HttpEntity(
                JournalføringRequestV2(
                    dokumentTitler = null,
                    fagsakId = UUID.randomUUID(),
                    oppgaveId = "dummy-oppgave",
                    journalførendeEnhet = "9991",
                    aksjon = Journalføringsaksjon.OPPRETT_BEHANDLING,
                    årsak = Journalføringsårsak.DIGITAL_SØKNAD,
                ),
                headers,
            ),
        )
}
