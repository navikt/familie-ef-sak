package no.nav.familie.ef.sak.iverksett

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.simulering.SimuleringsposteringTestUtil
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.simulering.SimuleringService
import no.nav.familie.ef.sak.simulering.tilSimuleringsoppsummering
import no.nav.familie.ef.sak.tilbakekreving.TilbakekrevingService
import no.nav.familie.ef.sak.tilbakekreving.domain.Tilbakekreving
import no.nav.familie.ef.sak.tilbakekreving.domain.Tilbakekrevingsvalg
import no.nav.familie.kontrakter.felles.simulering.DetaljertSimuleringResultat
import no.nav.familie.kontrakter.felles.simulering.MottakerType
import no.nav.familie.kontrakter.felles.simulering.PosteringType
import no.nav.familie.kontrakter.felles.simulering.SimuleringMottaker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

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

    private val januarStart = LocalDate.of(2021, 1, 1)
    private val aprilSlutt = LocalDate.of(2021, 4, 30)
    private val juniStart = LocalDate.of(2021, 6, 1)
    private val augustSlutt = LocalDate.of(2021, 8, 31)
    private val oktoberStart = LocalDate.of(2021, 10, 1)
    private val oktoberSlutt = LocalDate.of(2021, 10, 31)
    private val januarTilApril = SimuleringsposteringTestUtil.lagPosteringer(fraDato = januarStart,
                                                                             antallMåneder = 4,
                                                                             beløp = BigDecimal(5000),
                                                                             posteringstype = PosteringType.FEILUTBETALING)


    private val mai = SimuleringsposteringTestUtil.lagPosteringer(fraDato = LocalDate.of(2021, 5, 1),
                                                                  antallMåneder = 1,
                                                                  beløp = BigDecimal(5000),
                                                                  posteringstype = PosteringType.YTELSE)


    private val juniTilAugust = SimuleringsposteringTestUtil.lagPosteringer(fraDato = juniStart,
                                                                            antallMåneder = 3,
                                                                            beløp = BigDecimal(5000),
                                                                            posteringstype = PosteringType.FEILUTBETALING)

    private val oktober = SimuleringsposteringTestUtil.lagPosteringer(fraDato = oktoberStart,
                                                                      antallMåneder = 1,
                                                                      beløp = BigDecimal(5000),
                                                                      posteringstype = PosteringType.FEILUTBETALING)


    private val simuleringsmottakere = listOf(SimuleringMottaker(
            simulertPostering = januarTilApril + mai + juniTilAugust + oktober,
            mottakerNummer = "12345678901",
            mottakerType = MottakerType.BRUKER
    ))

    @Test
    internal fun `skal slå sammen perioder som har feilutbetalinger til sammenhengende perioder`() {
        val simuleringsoppsummering =
                tilSimuleringsoppsummering(DetaljertSimuleringResultat(simuleringsmottakere), LocalDate.of(2021, 11, 1))

        val sammenhengendePerioderMedFeilutbetaling = simuleringsoppsummering.hentSammenhengendePerioderMedFeilutbetaling()
        assertThat(sammenhengendePerioderMedFeilutbetaling).hasSize(3)
        assertThat(sammenhengendePerioderMedFeilutbetaling.first().fom).isEqualTo(januarStart)
        assertThat(sammenhengendePerioderMedFeilutbetaling.first().tom).isEqualTo(aprilSlutt)

        assertThat(sammenhengendePerioderMedFeilutbetaling.second().fom).isEqualTo(juniStart)
        assertThat(sammenhengendePerioderMedFeilutbetaling.second().tom).isEqualTo(augustSlutt)

        assertThat(sammenhengendePerioderMedFeilutbetaling.last().fom).isEqualTo(oktoberStart)
        assertThat(sammenhengendePerioderMedFeilutbetaling.last().tom).isEqualTo(oktoberSlutt)
    }

    @Test
    internal fun `Skal mappe tilbakekreving med varseltekst og feilutbetaling`() {
        val behandling = behandling(fagsak())
        val forventetVarseltekst = "forventetVarseltekst"
        val simuleringsoppsummering =
                tilSimuleringsoppsummering(DetaljertSimuleringResultat(simuleringsmottakere), LocalDate.of(2021, 11, 1))

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

private fun <E> List<E>.second(): E {
    return this[1]
}
