package no.nav.familie.ef.sak.infrastruktur.exception

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.infrastruktur.exception.TestExceptionType.MANGLERTILGANG
import no.nav.familie.ef.sak.infrastruktur.exception.TestExceptionType.RUNTIME
import no.nav.familie.ef.sak.infrastruktur.exception.TestExceptionType.SOCKET_TIMEOUT
import no.nav.familie.ef.sak.infrastruktur.exception.TestExceptionType.TIMEOUT
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.Ressurs.Status.FEILET
import no.nav.familie.kontrakter.felles.Ressurs.Status.IKKE_TILGANG
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.web.client.exchange
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.ResourceAccessException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeoutException

internal class ApiExceptionHandlerTest : OppslagSpringRunnerTest() {
    @BeforeEach
    fun setUp() {
        headers.setBearerAuth(lokalTestToken)
    }

    @Test
    internal fun `Skal håndtere timeout exception`() {
        val response = gjørKallSomKaster(TIMEOUT)
        assertThat(response.body?.status).isEqualTo(FEILET)
        assertThat(response.body?.melding).contains("Timeout feil")
        assertThat(response.body?.frontendFeilmelding).contains("Kommunikasjonsproblemer med andre systemer - prøv igjen")
    }

    @Test
    internal fun `Skal håndtere socket exception feil`() {
        val response = gjørKallSomKaster(SOCKET_TIMEOUT)
        assertThat(response.body?.status).isEqualTo(FEILET)
        assertThat(response.body?.melding).contains("Timeout feil")
        assertThat(response.body?.frontendFeilmelding).contains("Kommunikasjonsproblemer med andre systemer - prøv igjen")
    }

    @Test
    internal fun `Skal håndtere runtime exception feil`() {
        val response = gjørKallSomKaster(RUNTIME)
        assertThat(response.body?.status).isEqualTo(FEILET)
        assertThat(response.body?.melding).doesNotContain("Timeout feil")
        assertThat(response.body?.melding).contains("Uventet feil")
    }

    @Test
    internal fun `Skal håndtere tilgang exception feil`() {
        val response = gjørKallSomKaster(MANGLERTILGANG)
        assertThat(response.statusCode.value()).isEqualTo(HttpStatus.FORBIDDEN.value())
        assertThat(response.body?.status).isEqualTo(IKKE_TILGANG)
        assertThat(response.body?.melding).contains("manglertilgang123")
    }

    private fun gjørKallSomKaster(feil: TestExceptionType) =
        restTemplate.exchange<Ressurs<String>>(
            localhost("/api/testfeil/$feil"),
            HttpMethod.GET,
            HttpEntity<Ressurs<String>>(headers),
        )
}

enum class TestExceptionType {
    TIMEOUT,
    MANGLERTILGANG,
    SOCKET_TIMEOUT,
    RUNTIME,
}

@RestController
@RequestMapping("/api/testfeil/")
@ProtectedWithClaims(issuer = "azuread")
class TestController {
    @GetMapping(path = ["{exception}"])
    fun kastTimeoutException(
        @PathVariable exception: TestExceptionType,
    ): Ressurs<String> =
        throw when (exception) {
            TIMEOUT -> RuntimeException(TimeoutException(""))
            SOCKET_TIMEOUT -> RuntimeException(ResourceAccessException(" ", SocketTimeoutException("Read timed out")))
            MANGLERTILGANG -> ManglerTilgang("manglertilgang123", "feil til den som mangler tilgang")
            RUNTIME -> RuntimeException("")
        }
}
