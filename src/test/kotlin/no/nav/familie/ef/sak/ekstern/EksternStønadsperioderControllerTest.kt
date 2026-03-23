package no.nav.familie.ef.sak.ekstern

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.ef.EksternePerioderMedBeløpResponse
import no.nav.familie.kontrakter.felles.ef.EksternePerioderRequest
import no.nav.familie.kontrakter.felles.ef.EksternePerioderResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.resttestclient.exchange
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

internal class EksternStønadsperioderControllerTest : OppslagSpringRunnerTest() {
    @Test
    internal fun `perioder - kaller med access_as_application`() {
        headers.setBearerAuth(clientToken("familie-ef-proxy", true))
        utførKallOgVerifiser<EksternePerioderResponse>("/api/ekstern/perioder") { response ->
            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body?.status).isEqualTo(Ressurs.Status.SUKSESS)
        }
    }

    @Test
    internal fun `perioder - skal feile når man savner access_as_application`() {
        headers.setBearerAuth(clientToken("familie-ef-proxy", false))

        utførKallOgVerifiser<EksternePerioderResponse>("/api/ekstern/perioder") { response ->
            assertThat(response.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
            assertThat(response.body?.status).isEqualTo(Ressurs.Status.FEILET)
        }
    }

    @Test
    internal fun `perioder - alle stønader - kaller med access_as_application`() {
        headers.setBearerAuth(clientToken("familie-ef-proxy", true))

        utførKallOgVerifiser<EksternePerioderResponse>("/api/ekstern/perioder/alle-stonader") { response ->
            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body?.status).isEqualTo(Ressurs.Status.SUKSESS)
        }
    }

    @Test
    internal fun `perioder - alle stønader - skal feile når man savner access_as_application`() {
        headers.setBearerAuth(clientToken("familie-ef-proxy", false))

        utførKallOgVerifiser<EksternePerioderResponse>("/api/ekstern/perioder/alle-stonader") { response ->
            assertThat(response.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
            assertThat(response.body?.status).isEqualTo(Ressurs.Status.FEILET)
        }
    }

    @Test
    internal fun `full overgangsstønad - skal kunne kalle endepunkt med client_credential token`() {
        headers.setBearerAuth(clientToken("familie-ba-sak", true))

        utførKallOgVerifiser<EksternePerioderResponse>("/api/ekstern/perioder/full-overgangsstonad", PersonIdent("1")) { response ->
            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body?.status).isEqualTo(Ressurs.Status.SUKSESS)
        }
    }

    @Test
    internal fun `full overgangsstønad - skal ikke kunne kalle endepunkt uten token`() {
        utførKallOgVerifiser<EksternePerioderResponse>("/api/ekstern/perioder/full-overgangsstonad", PersonIdent("1")) { response ->
            assertThat(response.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
            assertThat(response.body?.status).isEqualTo(Ressurs.Status.FEILET)
        }
    }

    @Test
    internal fun `full overgangsstønad - skal kunne kalle endepunkt med on-behalf-of token`() {
        headers.setBearerAuth(onBehalfOfToken("familie-ba-sak"))
        utførKallOgVerifiser<EksternePerioderResponse>("/api/ekstern/perioder/full-overgangsstonad", PersonIdent("1")) { response ->
            assertThat(response.body?.status).isEqualTo(Ressurs.Status.SUKSESS)
            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        }
    }

    @Test
    internal fun `full overgangsstønad - on-behalf-of token har ikke tilgang til person`() {
        headers.setBearerAuth(onBehalfOfToken("d21e00a4-969d-4b28-8782-dc818abfae65"))

        utførKallOgVerifiser<EksternePerioderResponse>("/api/ekstern/perioder/full-overgangsstonad", PersonIdent("ikkeTilgang")) { response ->
            assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
            assertThat(response.body?.status).isEqualTo(Ressurs.Status.IKKE_TILGANG)
        }
    }

    @Test
    internal fun `perioder overgangsstønad - kaller med access_as_application`() {
        headers.setBearerAuth(clientToken("familie-ef-proxy", true))
        utførKallOgVerifiser<EksternePerioderResponse>("/api/ekstern/perioder/overgangsstonad") { response ->
            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body?.status).isEqualTo(Ressurs.Status.SUKSESS)
        }
    }

    @Test
    internal fun `perioder overgangsstønad - skal feile når man savner access_as_application`() {
        headers.setBearerAuth(clientToken("familie-ef-proxy", false))

        utførKallOgVerifiser<EksternePerioderResponse>("/api/ekstern/perioder/overgangsstonad") { response ->
            assertThat(response.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
            assertThat(response.body?.status).isEqualTo(Ressurs.Status.FEILET)
        }
    }

    @Test
    internal fun `perioder overgangsstønad med beløp - kaller med access_as_application`() {
        headers.setBearerAuth(clientToken("familie-ef-proxy", true))
        utførKallOgVerifiser<EksternePerioderMedBeløpResponse>("/api/ekstern/perioder/overgangsstonad/med-belop") { response ->
            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body?.status).isEqualTo(Ressurs.Status.SUKSESS)
        }
    }

    @Test
    internal fun `perioder overganggstønad med beløp - skal feile når man savner access_as_application`() {
        headers.setBearerAuth(clientToken("familie-ef-proxy", false))

        utførKallOgVerifiser<EksternePerioderMedBeløpResponse>("/api/ekstern/perioder/overgangsstonad/med-belop") { response ->
            assertThat(response.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
            assertThat(response.body?.status).isEqualTo(Ressurs.Status.FEILET)
        }
    }

    private fun <T> utførKallOgVerifiser(
        url: String,
        request: Any = EksternePerioderRequest("1"),
        lazyMessage: (ResponseEntity<Ressurs<T>>) -> Unit,
    ) {
        val response: ResponseEntity<Ressurs<T>> =
            testRestTemplate.exchange(
                localhost(url),
                HttpMethod.POST,
                HttpEntity(request, headers),
            )
        lazyMessage(response)
    }
}
