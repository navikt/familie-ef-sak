package no.nav.familie.ef.sak.arbeidsforhold

import io.mockk.verify
import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.arbeidsforhold.ekstern.ArbeidsforholdClient
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.fagsakpersoner
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.getDataOrThrow
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.exchange
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import java.time.LocalDate
import java.util.UUID

internal class ArbeidsforholdControllerTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var arbeidsforholdClient: ArbeidsforholdClient

    private val fagsak = fagsak(identer = fagsakpersoner(setOf("1")))

    @BeforeEach
    fun setUp() {
        headers.setBearerAuth(lokalTestToken)
        testoppsettService.lagreFagsak(fagsak)
    }

    @Test
    internal fun `skal hente arbeidsforhold med ansettelsesperiodeFom i query param`() {
        val arbeidsforhold = hentArbeidsforhold(fagsak.id)
        assertThat(arbeidsforhold.body!!.getDataOrThrow()).isNotNull
        verify { arbeidsforholdClient.hentArbeidsforhold("1") }
    }

    private fun hentArbeidsforhold(fagsakId: UUID): ResponseEntity<Ressurs<List<ArbeidsforholdDto>>> =
        restTemplate.exchange(
            localhost("/api/arbeidsforhold/fagsak/$fagsakId?ansettelsesperiodeFom=2021-01-01"),
            HttpMethod.GET,
            HttpEntity<Ressurs<List<ArbeidsforholdDto>>>(headers),
        )
}
