package no.nav.familie.ef.sak.tilbakekreving

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.tilbakekreving.domain.Tilbakekrevingsvalg.OPPRETT_MED_VARSEL
import no.nav.familie.ef.sak.tilbakekreving.domain.Tilbakekrevingsvalg.OPPRETT_UTEN_VARSEL
import no.nav.familie.ef.sak.tilbakekreving.dto.TilbakekrevingDto
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.ef.StønadType.OVERGANGSSTØNAD
import no.nav.familie.kontrakter.felles.getDataOrThrow
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.exchange
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import java.util.UUID

internal class TilbakekrevingControllerTest : OppslagSpringRunnerTest() {
    @Autowired
    lateinit var behandlingService: BehandlingService

    @Autowired
    lateinit var fagsakService: FagsakService

    @BeforeEach
    fun setUp() {
        headers.setBearerAuth(lokalTestToken)
    }

    @Test
    internal fun `Skal lagre siste versjon av tilbakekreving ved to kall`() {
        val fagsak = fagsakService.hentEllerOpprettFagsakMedBehandlinger("01010172272", OVERGANGSSTØNAD)
        val behandling =
            behandlingService.opprettBehandling(
                BehandlingType.FØRSTEGANGSBEHANDLING,
                fagsak.id,
                behandlingsårsak = BehandlingÅrsak.SØKNAD,
            )
        lagInitiellTilbakekreving(behandling)
        val oppdatertTilbakekrevingsDto =
            TilbakekrevingDto(
                valg = OPPRETT_MED_VARSEL,
                varseltekst = "Dette er tekst",
                begrunnelse = "Nei",
            )

        lagreTilbakekreving(behandling, oppdatertTilbakekrevingsDto)
        val andreLagredeTilbakekrevingDto = hentTilbakekreving(behandling)

        assertThat(andreLagredeTilbakekrevingDto.body?.getDataOrThrow()).isEqualTo(oppdatertTilbakekrevingsDto)
    }

    private fun lagInitiellTilbakekreving(behandling: Behandling) {
        val initiellTilbakekrevingDto =
            TilbakekrevingDto(
                valg = OPPRETT_UTEN_VARSEL,
                varseltekst = "",
                begrunnelse = "Ja",
            )
        lagreTilbakekreving(behandling, initiellTilbakekrevingDto)
        val førsteLagredeTilbakekrevingDto = hentTilbakekreving(behandling)
        assertThat(førsteLagredeTilbakekrevingDto.body?.getDataOrThrow()).isEqualTo(initiellTilbakekrevingDto)
    }

    private fun hentTilbakekreving(behandling: Behandling) =
        restTemplate.exchange<Ressurs<TilbakekrevingDto?>>(
            localhost("/api/tilbakekreving/${behandling.id}"),
            HttpMethod.GET,
            HttpEntity<TilbakekrevingDto>(headers),
        )

    private fun lagreTilbakekreving(
        behandling: Behandling,
        forventetTilbakekrevingsDto: TilbakekrevingDto,
    ) {
        restTemplate.exchange<Ressurs<UUID>>(
            localhost("/api/tilbakekreving/${behandling.id}"),
            HttpMethod.POST,
            HttpEntity(forventetTilbakekrevingsDto, headers),
        )
    }
}
