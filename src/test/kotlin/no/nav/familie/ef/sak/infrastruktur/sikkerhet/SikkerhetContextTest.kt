package no.nav.familie.ef.sak.infrastruktur.sikkerhet

import no.nav.familie.ef.sak.felles.util.BrukerContextUtil.clearBrukerContext
import no.nav.familie.ef.sak.felles.util.BrukerContextUtil.mockBrukerContext
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest

internal class SikkerhetContextTest {
    @Test
    internal fun `skal ikke godkjenne kall fra familie-ef-mottak for andre applikasjoner`() {
        mockBrukerContext("", azp_name = "prod-gcp:teamfamilie:familie-ef-personhendelse")
        Assertions.assertThat(SikkerhetContext.kallKommerFraFamilieEfMottak()).isFalse
        clearBrukerContext()

        mockBrukerContext("", azp_name = "prod-gcp:teamfamilie:familie-integrasjoner")
        Assertions.assertThat(SikkerhetContext.kallKommerFraFamilieEfMottak()).isFalse
        clearBrukerContext()
    }

    @Test
    internal fun `skal gjenkjenne kall fra familie-ef-mottak`() {
        mockBrukerContext("", azp_name = "dev-gcp:teamfamilie:familie-ef-mottak")
        Assertions.assertThat(SikkerhetContext.kallKommerFraFamilieEfMottak()).isTrue
        clearBrukerContext()
        mockBrukerContext("", azp_name = "prod-gcp:teamfamilie:familie-ef-mottak")
        Assertions.assertThat(SikkerhetContext.kallKommerFraFamilieEfMottak()).isTrue
        clearBrukerContext()
    }

    @Test
    internal fun `Nav-Ident header skal ignoreres når den er blank`() {
        val request = MockHttpServletRequest()
        request.addHeader("Nav-Ident", "")
        mockBrukerContext("B123456", servletRequest = request, azp_name = "prod-gcp:teamfamilie:familie-ef-mottak", oid = "oid1", sub = "oid1", roles = listOf("access_as_application"))
        Assertions.assertThat(SikkerhetContext.hentSaksbehandlerEllerSystembruker()).isEqualTo("B123456")
        clearBrukerContext()
    }

    @Test
    internal fun `Nav-Ident header skal ignoreres når den ikke matcher NAVIDENT_REGEX`() {
        val request = MockHttpServletRequest()
        request.addHeader("Nav-Ident", SikkerhetContext.SYSTEM_FORKORTELSE)
        mockBrukerContext("B123456", servletRequest = request, azp_name = "prod-gcp:teamfamilie:familie-ef-mottak", oid = "oid1", sub = "oid1", roles = listOf("access_as_application"))
        Assertions.assertThat(SikkerhetContext.hentSaksbehandlerEllerSystembruker()).isEqualTo("B123456")
        clearBrukerContext()
    }

    @Test
    internal fun `Nav-Ident header skal ignoreres når kallet ikke kommer fra betrodd kilde`() {
        val request = MockHttpServletRequest()
        request.addHeader("Nav-Ident", "A123456")
        mockBrukerContext("B123456", servletRequest = request, azp_name = "prod-gcp:some-team:some-app")
        Assertions.assertThat(SikkerhetContext.hentSaksbehandlerEllerSystembruker()).isEqualTo("B123456")
        clearBrukerContext()
    }

    @Test
    internal fun `Nav-Ident header skal aksepteres fra betrodd M2M-kilde med gyldig ident`() {
        val request = MockHttpServletRequest()
        request.addHeader("Nav-Ident", "A123456")
        mockBrukerContext("B123456", servletRequest = request, azp_name = "prod-gcp:teamfamilie:familie-ef-mottak", oid = "oid1", sub = "oid1", roles = listOf("access_as_application"))
        Assertions.assertThat(SikkerhetContext.hentSaksbehandlerEllerSystembruker()).isEqualTo("A123456")
        clearBrukerContext()
    }

    @Test
    internal fun `Nav-Ident header fra familie-klage skal aksepteres`() {
        val request = MockHttpServletRequest()
        request.addHeader("Nav-Ident", "Z654321")
        mockBrukerContext("B123456", servletRequest = request, azp_name = "prod-gcp:teamfamilie:familie-klage", oid = "oid1", sub = "oid1", roles = listOf("access_as_application"))
        Assertions.assertThat(SikkerhetContext.hentSaksbehandlerEllerSystembruker()).isEqualTo("Z654321")
        clearBrukerContext()
    }
}
