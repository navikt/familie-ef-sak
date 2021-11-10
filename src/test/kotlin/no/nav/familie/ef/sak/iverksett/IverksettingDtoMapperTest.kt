package no.nav.familie.ef.sak.iverksett

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.simulering.SimuleringService
import no.nav.familie.ef.sak.tilbakekreving.TilbakekrevingService
import no.nav.familie.ef.sak.tilbakekreving.domain.Tilbakekreving
import no.nav.familie.ef.sak.tilbakekreving.domain.Tilbakekrevingsvalg
import no.nav.familie.kontrakter.felles.simulering.Simuleringsoppsummering
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class IverksettingDtoMapperTest {

    val tilbakekrevingService = mockk<TilbakekrevingService>()
    val simuleringService = mockk<SimuleringService>()
    val iverksettingDtoMapper =
            IverksettingDtoMapper(arbeidsfordelingService = mockk(),
                                  behandlinghistorikkService = mockk(),
                                  fagsakService = mockk(),
                                  grunnlagsdataService = mockk(),
                                  simuleringService = simuleringService,
                                  søknadService = mockk(),
                                  tilbakekrevingService = tilbakekrevingService,
                                  tilkjentYtelseService = mockk(),
                                  vedtakService = mockk(),
                                  vilkårsvurderingRepository = mockk())


    @Test
    internal fun `Skal mappe tilbakekreving med varseltekst og feilutbetaling`() {

        val behandling = behandling(fagsak())
        val forventetVarseltekst = "forventetVarseltekst"
        val simuleringsoppsummering = Simuleringsoppsummering(
                perioder = emptyList(),
                fomDatoNestePeriode = null,
                etterbetaling = BigDecimal.ZERO,
                feilutbetaling = BigDecimal.TEN,
                fom = null,
                tomDatoNestePeriode = null,
                forfallsdatoNestePeriode = null,
                tidSimuleringHentet = null,
                tomSisteUtbetaling = null
        )

        every {
            tilbakekrevingService.hentTilbakekreving(behandlingId = behandling.id)
        } returns Tilbakekreving(behandlingId = behandling.id,
                                 valg = Tilbakekrevingsvalg.OPPRETT_MED_VARSEL,
                                 varseltekst = forventetVarseltekst,
                                 begrunnelse = "ingen")
        every {
            simuleringService.hentLagretSimuleringsresultat(behandlingId = behandling.id)
        } returns simuleringsoppsummering.copy(feilutbetaling = BigDecimal.TEN)

        val tilbakekreving = iverksettingDtoMapper.mapTilbakekreving(behandling.id)
        assertThat(tilbakekreving?.tilbakekrevingMedVarsel?.varseltekst).isEqualTo(forventetVarseltekst)
        assertThat(tilbakekreving?.tilbakekrevingMedVarsel?.sumFeilutbetaling).isEqualTo(BigDecimal.TEN)
    }

}

