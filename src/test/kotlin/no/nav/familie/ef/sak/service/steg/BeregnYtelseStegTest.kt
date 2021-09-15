package no.nav.familie.ef.sak.service.steg

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ef.sak.api.beregning.Beløpsperiode
import no.nav.familie.ef.sak.api.beregning.BeregningService
import no.nav.familie.ef.sak.api.beregning.Innvilget
import no.nav.familie.ef.sak.api.beregning.VedtakDto
import no.nav.familie.ef.sak.api.beregning.VedtakService
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.økonomi.lagAndelTilkjentYtelse
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.økonomi.lagTilkjentYtelse
import no.nav.familie.ef.sak.repository.domain.BehandlingType
import no.nav.familie.ef.sak.repository.domain.TilkjentYtelse
import no.nav.familie.ef.sak.service.BehandlingService
import no.nav.familie.ef.sak.service.TilkjentYtelseService
import no.nav.familie.ef.sak.simulering.SimuleringService
import no.nav.familie.ef.sak.simulering.Simuleringsresultat
import no.nav.familie.ef.sak.util.Periode
import no.nav.familie.kontrakter.felles.simulering.DetaljertSimuleringResultat
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

internal class BeregnYtelseStegTest {

    private val tilkjentYtelseService = mockk<TilkjentYtelseService>(relaxed = true)
    private val behandlingService = mockk<BehandlingService>()
    private val beregningService = mockk<BeregningService>()
    private val vedtakService = mockk<VedtakService>(relaxed = true)
    private val simuleringService = mockk<SimuleringService>()

    private val steg =
            BeregnYtelseSteg(tilkjentYtelseService, behandlingService, beregningService, simuleringService, vedtakService)

    @BeforeEach
    internal fun setUp() {
        every { behandlingService.hentAktivIdent(any()) } returns "123"
        every { simuleringService.hentOgLagreSimuleringsresultat(any()) } returns Simuleringsresultat(behandlingId = UUID.randomUUID(),
                                                                                                      data = DetaljertSimuleringResultat(
                                                                                                              emptyList()))
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
        verify (exactly = 1){
            simuleringService.hentOgLagreSimuleringsresultat(any())
        }
    }

    @Test
    internal fun `innvilget - skal kaste feil når man sender inn uten nye beløpsperioder`() {
        every { beregningService.beregnYtelse(any(), any()) } returns emptyList()
        assertThrows<IllegalStateException> { utførSteg(BehandlingType.REVURDERING) }
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

    private fun utførSteg(type: BehandlingType, vedtak: VedtakDto = Innvilget(periodeBegrunnelse = "", inntektBegrunnelse = ""), forrigeBehandlingId: UUID? = null) {
        steg.utførSteg(behandling(fagsak(), type = type, forrigeBehandlingId = forrigeBehandlingId), vedtak = vedtak)
    }

    private fun lagBeløpsperiode(fom: LocalDate, tom: LocalDate) =
            Beløpsperiode(Periode(fom, tom), null, BigDecimal.ZERO, BigDecimal.ZERO)


}