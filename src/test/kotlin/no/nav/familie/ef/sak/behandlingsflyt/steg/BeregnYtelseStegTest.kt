package no.nav.familie.ef.sak.behandlingsflyt.steg

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.beregning.Beløpsperiode
import no.nav.familie.ef.sak.beregning.BeregningService
import no.nav.familie.ef.sak.felles.dto.Periode
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.simulering.SimuleringService
import no.nav.familie.ef.sak.simulering.Simuleringsresultat
import no.nav.familie.ef.sak.tilbakekreving.TilbakekrevingService
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.ef.sak.tilkjentytelse.domain.TilkjentYtelse
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vedtak.domain.AvslagÅrsak
import no.nav.familie.ef.sak.vedtak.dto.Avslå
import no.nav.familie.ef.sak.vedtak.dto.Innvilget
import no.nav.familie.ef.sak.vedtak.dto.Opphør
import no.nav.familie.ef.sak.vedtak.dto.VedtakDto
import no.nav.familie.ef.sak.økonomi.lagAndelTilkjentYtelse
import no.nav.familie.ef.sak.økonomi.lagTilkjentYtelse
import no.nav.familie.kontrakter.felles.simulering.BeriketSimuleringsresultat
import no.nav.familie.kontrakter.felles.simulering.DetaljertSimuleringResultat
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

internal class BeregnYtelseStegTest {

    private val tilkjentYtelseService = mockk<TilkjentYtelseService>(relaxed = true)
    private val behandlingService = mockk<BehandlingService>()
    private val beregningService = mockk<BeregningService>()
    private val vedtakService = mockk<VedtakService>(relaxed = true)
    private val simuleringService = mockk<SimuleringService>()
    private val tilbakekrevingService = mockk<TilbakekrevingService>(relaxed = true)

    private val steg = BeregnYtelseSteg(tilkjentYtelseService,
                                        behandlingService,
                                        beregningService,
                                        simuleringService,
                                        vedtakService,
                                        tilbakekrevingService)

    @BeforeEach
    internal fun setUp() {
        every { behandlingService.hentAktivIdent(any()) } returns "123"
        every { simuleringService.hentOgLagreSimuleringsresultat(any()) }
                .returns(Simuleringsresultat(behandlingId = UUID.randomUUID(),
                                             data = DetaljertSimuleringResultat(emptyList()),
                                             beriketData = BeriketSimuleringsresultat(mockk(),
                                                                                      mockk())))
    }

    @Test
    internal fun `revurdering - nye andeler legges til etter forrige andeler`() {
        val forrigeAndelFom = LocalDate.of(2021, 1, 1)
        val forrigeAndelTom = LocalDate.of(2021, 3, 31)
        val nyAndelFom = LocalDate.of(2022, 1, 1)
        val nyAndelTom = LocalDate.of(2022, 1, 31)

        val slot = slot<TilkjentYtelse>()
        every { tilkjentYtelseService.opprettTilkjentYtelse(capture(slot)) } answers { firstArg() }
        every { tilkjentYtelseService.hentForBehandling(any()) } returns
                lagTilkjentYtelse(listOf(lagAndelTilkjentYtelse(100, forrigeAndelFom, forrigeAndelTom)))
        every { beregningService.beregnYtelse(any(), any()) } returns listOf(lagBeløpsperiode(nyAndelFom, nyAndelTom))

        utførSteg(BehandlingType.REVURDERING, forrigeBehandlingId = UUID.randomUUID())

        val andeler = slot.captured.andelerTilkjentYtelse
        assertThat(andeler).hasSize(2)
        assertThat(andeler[0].stønadFom).isEqualTo(forrigeAndelFom)
        assertThat(andeler[0].stønadTom).isEqualTo(forrigeAndelTom)

        assertThat(andeler[1].stønadFom).isEqualTo(nyAndelFom)
        assertThat(andeler[1].stønadTom).isEqualTo(nyAndelTom)

        assertThat(andeler[0].kildeBehandlingId).isNotEqualTo(andeler[1].kildeBehandlingId)
        verify(exactly = 1) {
            simuleringService.hentOgLagreSimuleringsresultat(any())
        }
    }


    @Test
    internal fun `revurdering - førstegangsbehandling er avslått - kun nye andeler som skal gjelde`() {
        val nyAndelFom = LocalDate.of(2022, 1, 1)
        val nyAndelTom = LocalDate.of(2022, 1, 31)

        val slot = slot<TilkjentYtelse>()
        every { tilkjentYtelseService.opprettTilkjentYtelse(capture(slot)) } answers { firstArg() }
        every { tilkjentYtelseService.hentForBehandling(any()) } throws IllegalArgumentException("Hjelp")
        every { beregningService.beregnYtelse(any(), any()) } returns listOf(lagBeløpsperiode(nyAndelFom, nyAndelTom))

        utførSteg(BehandlingType.REVURDERING, forrigeBehandlingId = null)

        val andeler = slot.captured.andelerTilkjentYtelse
        assertThat(andeler).hasSize(1)
        assertThat(andeler[0].stønadFom).isEqualTo(nyAndelFom)
        assertThat(andeler[0].stønadTom).isEqualTo(nyAndelTom)

        verify(exactly = 1) {
            simuleringService.hentOgLagreSimuleringsresultat(any())
        }
    }

    @Test
    internal fun `innvilget - skal kaste feil når man sender inn uten nye beløpsperioder`() {
        every { beregningService.beregnYtelse(any(), any()) } returns emptyList()
        assertThrows<Feil> { utførSteg(BehandlingType.REVURDERING) }
    }

    @Test
    internal fun `førstegangsbehandling - happy case`() {
        every { beregningService.beregnYtelse(any(), any()) } returns listOf(lagBeløpsperiode(LocalDate.now(), LocalDate.now()))
        utførSteg(BehandlingType.FØRSTEGANGSBEHANDLING)

        verify(exactly = 0) { tilkjentYtelseService.hentForBehandling(any()) }
    }

    @Test
    internal fun `slåSammenAndelerSomSkalVidereføres - nye perioder er før forrige andeler`() {
        val forrigeAndelFom = LocalDate.of(2022, 1, 1)
        val forrigeAndelTom = LocalDate.of(2022, 3, 31)
        val nyAndelFom = LocalDate.of(2021, 1, 1)
        val nyAndelTom = LocalDate.of(2021, 1, 31)

        val forrigeAndeler = listOf(lagAndelTilkjentYtelse(50, forrigeAndelFom, forrigeAndelTom))
        val beløpsperioder = listOf(lagAndelTilkjentYtelse(100, nyAndelFom, nyAndelTom))
        val nyeAndeler = steg.slåSammenAndelerSomSkalVidereføres(beløpsperioder, lagTilkjentYtelse(forrigeAndeler))

        assertThat(nyeAndeler).hasSize(1)
        assertThat(nyeAndeler[0].stønadFom).isEqualTo(nyAndelFom)
        assertThat(nyeAndeler[0].stønadTom).isEqualTo(nyAndelTom)
        assertThat(nyeAndeler[0].beløp).isEqualTo(100)

        assertThat(forrigeAndeler[0].kildeBehandlingId).isNotEqualTo(nyeAndeler[0].kildeBehandlingId)
        assertThat(nyeAndeler[0].kildeBehandlingId).isEqualTo(nyeAndeler[0].kildeBehandlingId)
    }

    @Test
    internal fun `slåSammenAndelerSomSkalVidereføres - legger på ny periode rett etter forrige periode`() {
        val forrigeAndelFom = LocalDate.of(2021, 1, 1)
        val forrigeAndelTom = LocalDate.of(2021, 10, 31)
        val nyAndelFom = LocalDate.of(2021, 11, 1)
        val nyAndelTom = LocalDate.of(2021, 11, 30)

        val forrigeAndeler = listOf(lagAndelTilkjentYtelse(50, forrigeAndelFom, forrigeAndelTom))
        val beløpsperioder = listOf(lagAndelTilkjentYtelse(100, nyAndelFom, nyAndelTom))
        val nyeAndeler = steg.slåSammenAndelerSomSkalVidereføres(beløpsperioder, lagTilkjentYtelse(forrigeAndeler))

        assertThat(nyeAndeler).hasSize(2)
        assertThat(nyeAndeler[0].stønadFom).isEqualTo(forrigeAndelFom)
        assertThat(nyeAndeler[0].stønadTom).isEqualTo(forrigeAndelTom)
        assertThat(nyeAndeler[0].beløp).isEqualTo(50)

        assertThat(nyeAndeler[1].stønadFom).isEqualTo(nyAndelFom)
        assertThat(nyeAndeler[1].stønadTom).isEqualTo(nyAndelTom)
        assertThat(nyeAndeler[1].beløp).isEqualTo(100)

        assertThat(nyeAndeler[0].kildeBehandlingId).isNotEqualTo(nyeAndeler[1].kildeBehandlingId)
    }

    @Test
    internal fun `slåSammenAndelerSomSkalVidereføres - skal avkorte den tidligere perioden og legge på nye perioder`() {
        val forrigeAndelFom = LocalDate.of(2021, 1, 1)
        val forrigeAndelTom = LocalDate.of(2021, 12, 31)
        val nyAndelFom = LocalDate.of(2021, 11, 1)
        val nyAndelTom = LocalDate.of(2021, 11, 30)

        val forrigeAndeler = listOf(lagAndelTilkjentYtelse(50, forrigeAndelFom, forrigeAndelTom))
        val beløpsperioder = listOf(lagAndelTilkjentYtelse(100, nyAndelFom, nyAndelTom))
        val nyeAndeler = steg.slåSammenAndelerSomSkalVidereføres(beløpsperioder, lagTilkjentYtelse(forrigeAndeler))

        assertThat(nyeAndeler).hasSize(2)
        assertThat(nyeAndeler[0].stønadFom).isEqualTo(forrigeAndelFom)
        assertThat(nyeAndeler[0].stønadTom).isEqualTo(LocalDate.of(2021, 10, 31))
        assertThat(nyeAndeler[0].beløp).isEqualTo(50)

        assertThat(nyeAndeler[1].stønadFom).isEqualTo(nyAndelFom)
        assertThat(nyeAndeler[1].stønadTom).isEqualTo(nyAndelTom)
        assertThat(nyeAndeler[1].beløp).isEqualTo(100)
    }

    @Test
    internal fun `slåSammenAndelerSomSkalVidereføres - bytter ut siste andelen med en ny andel`() {
        val forrigeAndelFom = LocalDate.of(2021, 1, 1)
        val forrigeAndelTom = LocalDate.of(2021, 10, 31)
        val forrigeAndelFom2 = LocalDate.of(2021, 11, 1)
        val forrigeAndelTom2 = LocalDate.of(2021, 11, 30)
        val nyAndelFom = LocalDate.of(2021, 11, 1)
        val nyAndelTom = LocalDate.of(2021, 11, 30)

        val forrigeAndeler = listOf(lagAndelTilkjentYtelse(50, forrigeAndelFom, forrigeAndelTom),
                                    lagAndelTilkjentYtelse(70, forrigeAndelFom2, forrigeAndelTom2))
        val beløpsperioder = listOf(lagAndelTilkjentYtelse(100, nyAndelFom, nyAndelTom))
        val nyeAndeler = steg.slåSammenAndelerSomSkalVidereføres(beløpsperioder, lagTilkjentYtelse(forrigeAndeler))

        assertThat(nyeAndeler).hasSize(2)
        assertThat(nyeAndeler[0].stønadFom).isEqualTo(forrigeAndelFom)
        assertThat(nyeAndeler[0].stønadTom).isEqualTo(LocalDate.of(2021, 10, 31))
        assertThat(nyeAndeler[0].beløp).isEqualTo(50)

        assertThat(nyeAndeler[1].stønadFom).isEqualTo(nyAndelFom)
        assertThat(nyeAndeler[1].stønadTom).isEqualTo(nyAndelTom)
        assertThat(nyeAndeler[1].beløp).isEqualTo(100)
    }

    @Test
    internal fun `skal opphøre vedtak fra dato`() {
        val opphørFom = YearMonth.of(2021, 6)

        val forrigeAndelFom = LocalDate.of(2021, 1, 1)
        val forrigeAndelTom = LocalDate.of(2021, 12, 31)
        val forventetNyAndelFom = LocalDate.of(2021, 1, 1)
        val forventetNyAndelTom = LocalDate.of(2021, 5, 31)

        val slot = slot<TilkjentYtelse>()
        every { tilkjentYtelseService.opprettTilkjentYtelse(capture(slot)) } answers { firstArg() }
        every { tilkjentYtelseService.hentForBehandling(any()) } returns
                lagTilkjentYtelse(listOf(lagAndelTilkjentYtelse(100, forrigeAndelFom, forrigeAndelTom)))

        utførSteg(BehandlingType.REVURDERING,
                  Opphør(opphørFom = opphørFom, begrunnelse = "null"),
                  forrigeBehandlingId = UUID.randomUUID())

        assertThat(slot.captured.andelerTilkjentYtelse).hasSize(1)
        assertThat(slot.captured.andelerTilkjentYtelse.first().stønadFom).isEqualTo(forventetNyAndelFom)
        assertThat(slot.captured.andelerTilkjentYtelse.first().stønadTom).isEqualTo(forventetNyAndelTom)
    }

    @Test
    internal fun `skal feile hvis opphørsdato ikke sammenfaller med en periode`() {
        val opphørFom = YearMonth.of(2021, 6)

        val forrigeAndelFom = LocalDate.of(2021, 1, 1)
        val forrigeAndelTom = LocalDate.of(2021, 3, 31)

        val slot = slot<TilkjentYtelse>()
        every { tilkjentYtelseService.opprettTilkjentYtelse(capture(slot)) } answers { firstArg() }
        every { tilkjentYtelseService.hentForBehandling(any()) } returns
                lagTilkjentYtelse(listOf(lagAndelTilkjentYtelse(100, forrigeAndelFom, forrigeAndelTom)))

        assertThrows<Feil> {
            utførSteg(BehandlingType.REVURDERING,
                      Opphør(opphørFom = opphørFom, begrunnelse = "null"),
                      forrigeBehandlingId = UUID.randomUUID())
        }
    }

    @Test
    internal fun `skal få kun første periode hvis opphørsdato er starten på neste periode som er delt i to`() {
        val opphørFom = YearMonth.of(2021, 9)

        val andel1Fom = LocalDate.of(2021, 1, 1)
        val andel1Tom = LocalDate.of(2021, 6, 30)
        val andel2Fom = LocalDate.of(2021, 9, 1)
        val andel2Tom = LocalDate.of(2021, 12, 31)

        val slot = slot<TilkjentYtelse>()
        every { tilkjentYtelseService.opprettTilkjentYtelse(capture(slot)) } answers { firstArg() }
        every { tilkjentYtelseService.hentForBehandling(any()) } returns
                lagTilkjentYtelse(listOf(lagAndelTilkjentYtelse(100, andel1Fom, andel1Tom),
                                         lagAndelTilkjentYtelse(200, andel2Fom, andel2Tom)))

        utførSteg(BehandlingType.REVURDERING,
                  Opphør(opphørFom = opphørFom, begrunnelse = "null"),
                  forrigeBehandlingId = UUID.randomUUID())
        assertThat(slot.captured.andelerTilkjentYtelse).hasSize(1)
        assertThat(slot.captured.andelerTilkjentYtelse.first().stønadFom).isEqualTo(andel1Fom)
        assertThat(slot.captured.andelerTilkjentYtelse.first().stønadTom).isEqualTo(andel1Tom)

    }

    @Test
    internal fun `skal feile hvis opphørsdato er mellom to perioder`() {
        val opphørFom = YearMonth.of(2021, 8)

        val andel1Fom = LocalDate.of(2021, 1, 1)
        val andel1Tom = LocalDate.of(2021, 6, 30)
        val andel2Fom = LocalDate.of(2021, 9, 1)
        val andel2Tom = LocalDate.of(2021, 12, 31)

        val slot = slot<TilkjentYtelse>()
        every { tilkjentYtelseService.opprettTilkjentYtelse(capture(slot)) } answers { firstArg() }
        every { tilkjentYtelseService.hentForBehandling(any()) } returns
                lagTilkjentYtelse(listOf(lagAndelTilkjentYtelse(100, andel1Fom, andel1Tom),
                                         lagAndelTilkjentYtelse(200, andel2Fom, andel2Tom)))

        assertThrows<Feil> {
            utførSteg(BehandlingType.REVURDERING,
                      Opphør(opphørFom = opphørFom, begrunnelse = "null"),
                      forrigeBehandlingId = UUID.randomUUID())
        }

    }


    @Test
    internal fun `skal opphøre hvis opphørsdato samsvarer med startdato for andel`() {
        val opphørFom = YearMonth.of(2021, 7)

        val andel1Fom = LocalDate.of(2021, 1, 1)
        val andel1Tom = LocalDate.of(2021, 6, 30)
        val andel2Fom = LocalDate.of(2021, 7, 1)
        val andel2Tom = LocalDate.of(2021, 12, 31)

        val forventetNyAndelFom = LocalDate.of(2021, 1, 1)
        val forventetNyAndelTom = LocalDate.of(2021, 6, 30)

        val slot = slot<TilkjentYtelse>()
        every { tilkjentYtelseService.opprettTilkjentYtelse(capture(slot)) } answers { firstArg() }
        every { tilkjentYtelseService.hentForBehandling(any()) } returns
                lagTilkjentYtelse(listOf(lagAndelTilkjentYtelse(100, andel1Fom, andel1Tom),
                                         lagAndelTilkjentYtelse(200, andel2Fom, andel2Tom)))

        utførSteg(BehandlingType.REVURDERING,
                  Opphør(opphørFom = opphørFom, begrunnelse = "null"),
                  forrigeBehandlingId = UUID.randomUUID())

        assertThat(slot.captured.andelerTilkjentYtelse).hasSize(1)
        assertThat(slot.captured.andelerTilkjentYtelse.first().stønadFom).isEqualTo(forventetNyAndelFom)
        assertThat(slot.captured.andelerTilkjentYtelse.first().stønadTom).isEqualTo(forventetNyAndelTom)
    }

    @Test
    internal fun `skal opphøre hvis opphørsdato er måneden etter periodestart`() {
        val opphørFom = YearMonth.of(2021, 8)

        val andel1Fom = LocalDate.of(2021, 1, 1)
        val andel1Tom = LocalDate.of(2021, 6, 30)
        val andel2Fom = LocalDate.of(2021, 7, 1)
        val andel2Tom = LocalDate.of(2021, 12, 31)

        val forventetAndelFom1 = LocalDate.of(2021, 1, 1)
        val forventetAndelTom1 = LocalDate.of(2021, 6, 30)
        val forventetAndelFom2 = LocalDate.of(2021, 7, 1)
        val forventetAndelTom2 = LocalDate.of(2021, 7, 31)

        val slot = slot<TilkjentYtelse>()
        every { tilkjentYtelseService.opprettTilkjentYtelse(capture(slot)) } answers { firstArg() }
        every { tilkjentYtelseService.hentForBehandling(any()) } returns
                lagTilkjentYtelse(listOf(lagAndelTilkjentYtelse(100, andel1Fom, andel1Tom),
                                         lagAndelTilkjentYtelse(200, andel2Fom, andel2Tom)))

        utførSteg(BehandlingType.REVURDERING,
                  Opphør(opphørFom = opphørFom, begrunnelse = "null"),
                  forrigeBehandlingId = UUID.randomUUID())

        assertThat(slot.captured.andelerTilkjentYtelse).hasSize(2)
        assertThat(slot.captured.andelerTilkjentYtelse[0].stønadFom).isEqualTo(forventetAndelFom1)
        assertThat(slot.captured.andelerTilkjentYtelse[0].stønadTom).isEqualTo(forventetAndelTom1)
        assertThat(slot.captured.andelerTilkjentYtelse[1].stønadFom).isEqualTo(forventetAndelFom2)
        assertThat(slot.captured.andelerTilkjentYtelse[1].stønadTom).isEqualTo(forventetAndelTom2)
    }

    @Test
    internal fun `skal opphøre hvis opphørsdato samsvarer med startdato for første andel`() {
        val opphørFom = YearMonth.of(2021, 1)

        val andelFom = LocalDate.of(2021, 1, 1)
        val andelTom = LocalDate.of(2021, 6, 30)

        val slot = slot<TilkjentYtelse>()
        every { tilkjentYtelseService.opprettTilkjentYtelse(capture(slot)) } answers { firstArg() }
        every { tilkjentYtelseService.hentForBehandling(any()) } returns
                lagTilkjentYtelse(listOf(lagAndelTilkjentYtelse(100, andelFom, andelTom)))

        utførSteg(BehandlingType.REVURDERING,
                  Opphør(opphørFom = opphørFom, begrunnelse = "null"),
                  forrigeBehandlingId = UUID.randomUUID())

        assertThat(slot.captured.andelerTilkjentYtelse).hasSize(0)
    }

    @Test
    internal fun `skal feile ved opphør, dersom behandlingstype ikke er revurdering`() {
        val feil = assertThrows<Feil> {
            utførSteg(BehandlingType.FØRSTEGANGSBEHANDLING,
                      Opphør(opphørFom = YearMonth.of(2021, 6), begrunnelse = "null"), forrigeBehandlingId = UUID.randomUUID())
        }
        assertThat(feil.frontendFeilmelding).contains("Kan kun opphøre ved revurdering")
    }


    @Test
    internal fun `skal slette tilbakekreving og simulering ved avslag`() {
        every { simuleringService.slettSimuleringForBehandling(any()) } just Runs
        every { tilbakekrevingService.slettTilbakekreving(any()) } just Runs
        utførSteg(type = BehandlingType.FØRSTEGANGSBEHANDLING,
                  vedtak = Avslå(avslåBegrunnelse = "", avslåÅrsak = AvslagÅrsak.VILKÅR_IKKE_OPPFYLT))

        verify { tilbakekrevingService.slettTilbakekreving(any()) }
        verify { simuleringService.slettSimuleringForBehandling(any()) }
    }


    private fun utførSteg(type: BehandlingType,
                          vedtak: VedtakDto = Innvilget(periodeBegrunnelse = "", inntektBegrunnelse = ""),
                          forrigeBehandlingId: UUID? = null) {
        steg.utførSteg(behandling(fagsak(), type = type, forrigeBehandlingId = forrigeBehandlingId), data = vedtak)
    }

    private fun lagBeløpsperiode(fom: LocalDate, tom: LocalDate) =
            Beløpsperiode(Periode(fom, tom), null, BigDecimal.ZERO, BigDecimal.ZERO)


}