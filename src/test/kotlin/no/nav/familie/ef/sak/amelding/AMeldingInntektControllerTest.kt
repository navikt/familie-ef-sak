package no.nav.familie.ef.sak.amelding

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.fagsakpersoner
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.getDataOrThrow
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.resttestclient.exchange
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity

internal class AMeldingInntektControllerTest : OppslagSpringRunnerTest() {
    private val fagsak = fagsak(identer = fagsakpersoner(setOf("1")))

    @BeforeEach
    fun setUp() {
        headers.setBearerAuth(lokalTestToken)
        testoppsettService.lagreFagsak(fagsak)
    }

    @Test
    internal fun `skal generere url til a-inntekt`() {
        val response = kallGenererUrl()
        assertThat(response.body?.getDataOrThrow()).isEqualTo("https://ainntekt")
    }

    private fun kallGenererUrl(): ResponseEntity<Ressurs<String>> =
        testRestTemplate.exchange(
            localhost("/api/inntekt/fagsak/${fagsak.id}/generer-url"),
            HttpMethod.GET,
            HttpEntity<Ressurs<String>>(headers),
        )
}
