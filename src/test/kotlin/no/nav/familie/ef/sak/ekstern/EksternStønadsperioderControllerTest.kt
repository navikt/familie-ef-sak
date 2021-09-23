package no.nav.familie.ef.sak.ekstern

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.felles.util.clientToken
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.ef.PerioderOvergangsstønadRequest
import no.nav.familie.kontrakter.felles.ef.PerioderOvergangsstønadResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.web.client.exchange
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

internal class EksternStønadsperioderControllerTest : OppslagSpringRunnerTest() {

    @Test
    internal fun `perioder - kaller med access_as_application`() {
        headers.setBearerAuth(clientToken("familie-ef-proxy", true))

        val response: ResponseEntity<Ressurs<PerioderOvergangsstønadResponse>> =
                restTemplate.exchange(localhost("/api/ekstern/perioder"),
                                      HttpMethod.POST,
                                      HttpEntity(PerioderOvergangsstønadRequest("1"), headers))

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body!!.status).isEqualTo(Ressurs.Status.SUKSESS)
    }

    @Test
    internal fun `perioder - skal feile når man savner access_as_application`() {
        headers.setBearerAuth(clientToken("familie-ef-proxy", false))

        val response: ResponseEntity<Ressurs<PerioderOvergangsstønadResponse>> =
                restTemplate.exchange(localhost("/api/ekstern/perioder"),
                                      HttpMethod.POST,
                                      HttpEntity(PerioderOvergangsstønadRequest("1"), headers))

        assertThat(response.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
        assertThat(response.body!!.status).isEqualTo(Ressurs.Status.FEILET)
    }
}