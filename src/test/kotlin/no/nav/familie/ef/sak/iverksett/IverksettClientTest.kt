package no.nav.familie.ef.sak.iverksett

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import no.nav.familie.ef.sak.felles.domain.Fil
import no.nav.familie.ef.sak.infrastruktur.config.JsonMapperProvider
import no.nav.familie.kontrakter.ef.felles.BehandlingType
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.ef.felles.Opplysningskilde
import no.nav.familie.kontrakter.ef.felles.Revurderingsårsak
import no.nav.familie.kontrakter.ef.felles.Vedtaksresultat
import no.nav.familie.kontrakter.ef.iverksett.BehandlingKategori
import no.nav.familie.kontrakter.ef.iverksett.BehandlingsdetaljerDto
import no.nav.familie.kontrakter.ef.iverksett.FagsakdetaljerDto
import no.nav.familie.kontrakter.ef.iverksett.IverksettOvergangsstønadDto
import no.nav.familie.kontrakter.ef.iverksett.SøkerDto
import no.nav.familie.kontrakter.ef.iverksett.VedtaksdetaljerOvergangsstønadDto
import no.nav.familie.kontrakter.ef.iverksett.ÅrsakRevurderingDto
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.boot.restclient.RestTemplateBuilder
import org.springframework.http.HttpHeaders
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.web.client.RestTemplate
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class IverksettClientTest {
    @AfterEach
    fun tearDownEachTest() {
        wiremockServer.resetAll()
    }

    @Test
    fun `iverksett med årsakRevurdering`() {
        wiremockServer.stubFor(
            post(urlEqualTo("/api/iverksett"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                        .withBody("{}"),
                ),
        )

        val iverksettDto = lagIverksettDtoMedÅrsakRevurdering()
        val fil = Fil("test-innhold".toByteArray())

        client.iverksett(iverksettDto, fil)

        val requests = wiremockServer.findAll(postRequestedFor(urlEqualTo("/api/iverksett")))
        assertThat(requests).hasSize(1)

        val body = requests.first().bodyAsString
        assertThat(body).contains("årsakRevurdering")
        assertThat(body).contains("opplysningskilde")
        assertThat(body).contains("MELDING_MODIA")
        assertThat(body).contains("ENDRING_INNTEKT")
    }

    @Test
    fun `iverksettUtenBrev sender request med årsakRevurdering`() {
        wiremockServer.stubFor(
            post(urlEqualTo("/api/iverksett/uten-brev"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                        .withBody("{}"),
                ),
        )

        val iverksettDto = lagIverksettDtoMedÅrsakRevurdering()

        client.iverksettUtenBrev(iverksettDto)

        val requests = wiremockServer.findAll(postRequestedFor(urlEqualTo("/api/iverksett/uten-brev")))
        assertThat(requests).hasSize(1)

        val body = requests.first().bodyAsString
        assertThat(body).contains("årsakRevurdering")
        assertThat(body).contains("MELDING_MODIA")
        assertThat(body).contains("ENDRING_INNTEKT")
    }

    @Test
    fun `iverksett uten årsakRevurdering sender null`() {
        wiremockServer.stubFor(
            post(urlEqualTo("/api/iverksett"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                        .withBody("{}"),
                ),
        )

        val iverksettDto = lagIverksettDtoUtenÅrsakRevurdering()
        val fil = Fil("test".toByteArray())

        client.iverksett(iverksettDto, fil)

        val requests = wiremockServer.findAll(postRequestedFor(urlEqualTo("/api/iverksett")))
        val body = requests.first().bodyAsString

        assertThat(body).doesNotContain("MELDING_MODIA")
    }

    private fun lagIverksettDtoMedÅrsakRevurdering(
        behandlingId: UUID = UUID.randomUUID(),
        fagsakId: UUID = UUID.randomUUID(),
    ): IverksettOvergangsstønadDto =
        IverksettOvergangsstønadDto(
            fagsak =
                FagsakdetaljerDto(
                    fagsakId = fagsakId,
                    eksternId = 123L,
                    stønadstype = StønadType.OVERGANGSSTØNAD,
                ),
            behandling =
                BehandlingsdetaljerDto(
                    behandlingId = behandlingId,
                    behandlingType = BehandlingType.REVURDERING,
                    behandlingÅrsak = BehandlingÅrsak.NYE_OPPLYSNINGER,
                    eksternId = 456L,
                    vilkårsvurderinger = emptyList(),
                    forrigeBehandlingId = null,
                    forrigeBehandlingEksternId = null,
                    kravMottatt = LocalDate.now(),
                    årsakRevurdering =
                        ÅrsakRevurderingDto(
                            opplysningskilde = Opplysningskilde.MELDING_MODIA,
                            årsak = Revurderingsårsak.ENDRING_INNTEKT,
                        ),
                    kategori = BehandlingKategori.NASJONAL,
                ),
            søker =
                SøkerDto(
                    personIdent = "12345678901",
                    barn = emptyList(),
                    tilhørendeEnhet = "4489",
                    adressebeskyttelse = null,
                ),
            vedtak =
                VedtaksdetaljerOvergangsstønadDto(
                    resultat = Vedtaksresultat.INNVILGET,
                    vedtakstidspunkt = LocalDateTime.now(),
                    opphørÅrsak = null,
                    saksbehandlerId = "saksbehandler",
                    beslutterId = "beslutter",
                    tilkjentYtelse = null,
                    vedtaksperioder = emptyList(),
                    tilbakekreving = null,
                    brevmottakere = emptyList(),
                    avslagÅrsak = null,
                    grunnbeløp = null,
                ),
        )

    private fun lagIverksettDtoUtenÅrsakRevurdering(): IverksettOvergangsstønadDto =
        IverksettOvergangsstønadDto(
            fagsak =
                FagsakdetaljerDto(
                    fagsakId = UUID.randomUUID(),
                    eksternId = 123L,
                    stønadstype = StønadType.OVERGANGSSTØNAD,
                ),
            behandling =
                BehandlingsdetaljerDto(
                    behandlingId = UUID.randomUUID(),
                    behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                    behandlingÅrsak = BehandlingÅrsak.SØKNAD,
                    eksternId = 456L,
                    vilkårsvurderinger = emptyList(),
                    forrigeBehandlingId = null,
                    forrigeBehandlingEksternId = null,
                    kravMottatt = LocalDate.now(),
                    årsakRevurdering = null,
                    kategori = BehandlingKategori.NASJONAL,
                ),
            søker =
                SøkerDto(
                    personIdent = "12345678901",
                    barn = emptyList(),
                    tilhørendeEnhet = "4489",
                    adressebeskyttelse = null,
                ),
            vedtak =
                VedtaksdetaljerOvergangsstønadDto(
                    resultat = Vedtaksresultat.INNVILGET,
                    vedtakstidspunkt = LocalDateTime.now(),
                    opphørÅrsak = null,
                    saksbehandlerId = "saksbehandler",
                    beslutterId = "beslutter",
                    tilkjentYtelse = null,
                    vedtaksperioder = emptyList(),
                    tilbakekreving = null,
                    brevmottakere = emptyList(),
                    avslagÅrsak = null,
                    grunnbeløp = null,
                ),
        )

    companion object {
        private lateinit var wiremockServer: WireMockServer
        private lateinit var client: IverksettClient

        @BeforeAll
        @JvmStatic
        fun initClass() {
            wiremockServer = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())
            wiremockServer.start()

            val restTemplate: RestTemplate =
                RestTemplateBuilder()
                    .additionalMessageConverters(
                        JacksonJsonHttpMessageConverter(JsonMapperProvider.jsonMapper),
                    ).build()

            client = IverksettClient(wiremockServer.baseUrl(), restTemplate)
        }

        @AfterAll
        @JvmStatic
        fun tearDown() {
            wiremockServer.stop()
        }
    }
}
