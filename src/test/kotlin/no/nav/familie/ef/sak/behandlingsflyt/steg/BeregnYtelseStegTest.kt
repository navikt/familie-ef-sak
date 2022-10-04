package no.nav.familie.ef.sak.behandlingsflyt.steg

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ef.sak.barn.BarnService
import no.nav.familie.ef.sak.barn.BehandlingBarn
import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.beregning.Beløpsperiode
import no.nav.familie.ef.sak.beregning.BeregningService
import no.nav.familie.ef.sak.beregning.Inntekt
import no.nav.familie.ef.sak.beregning.ValiderOmregningService
import no.nav.familie.ef.sak.beregning.barnetilsyn.BeløpsperiodeBarnetilsynDto
import no.nav.familie.ef.sak.beregning.barnetilsyn.BeregningBarnetilsynService
import no.nav.familie.ef.sak.beregning.barnetilsyn.BeregningsgrunnlagBarnetilsynDto
import no.nav.familie.ef.sak.beregning.skolepenger.BeregningSkolepengerService
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.felles.util.mockFeatureToggleService
import no.nav.familie.ef.sak.infrastruktur.exception.ApiFeil
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.fagsakpersoner
import no.nav.familie.ef.sak.repository.saksbehandling
import no.nav.familie.ef.sak.repository.tilkjentYtelse
import no.nav.familie.ef.sak.repository.vedtaksperiodeDto
import no.nav.familie.ef.sak.simulering.SimuleringService
import no.nav.familie.ef.sak.simulering.Simuleringsresultat
import no.nav.familie.ef.sak.tilbakekreving.TilbakekrevingService
import no.nav.familie.ef.sak.tilkjentytelse.AndelsHistorikkService
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.ef.sak.tilkjentytelse.domain.TilkjentYtelse
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vedtak.domain.AktivitetType
import no.nav.familie.ef.sak.vedtak.domain.AvslagÅrsak
import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType
import no.nav.familie.ef.sak.vedtak.dto.Avslå
import no.nav.familie.ef.sak.vedtak.dto.InnvilgelseBarnetilsyn
import no.nav.familie.ef.sak.vedtak.dto.InnvilgelseOvergangsstønad
import no.nav.familie.ef.sak.vedtak.dto.Opphør
import no.nav.familie.ef.sak.vedtak.dto.ResultatType
import no.nav.familie.ef.sak.vedtak.dto.Sanksjonert
import no.nav.familie.ef.sak.vedtak.dto.SanksjonertPeriodeDto
import no.nav.familie.ef.sak.vedtak.dto.Sanksjonsårsak
import no.nav.familie.ef.sak.vedtak.dto.TilleggsstønadDto
import no.nav.familie.ef.sak.vedtak.dto.UtgiftsperiodeDto
import no.nav.familie.ef.sak.vedtak.dto.VedtakDto
import no.nav.familie.ef.sak.vedtak.dto.VedtaksperiodeDto
import no.nav.familie.ef.sak.vedtak.historikk.AndelHistorikkDto
import no.nav.familie.ef.sak.vedtak.historikk.AndelMedGrunnlagDto
import no.nav.familie.ef.sak.vedtak.historikk.EndringType
import no.nav.familie.ef.sak.vedtak.historikk.HistorikkEndring
import no.nav.familie.ef.sak.økonomi.lagAndelTilkjentYtelse
import no.nav.familie.ef.sak.økonomi.lagTilkjentYtelse
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.felles.Månedsperiode
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.kontrakter.felles.simulering.BeriketSimuleringsresultat
import no.nav.familie.kontrakter.felles.simulering.DetaljertSimuleringResultat
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

internal class BeregnYtelseStegTest {

    private val tilkjentYtelseService = mockk<TilkjentYtelseService>(relaxed = true)
    private val andelsHistorikkService = mockk<AndelsHistorikkService>(relaxed = true)
    private val beregningService = mockk<BeregningService>()
    private val beregningBarnetilsynService = mockk<BeregningBarnetilsynService>()
    private val beregningSkolepengerService = mockk<BeregningSkolepengerService>()
    private val vedtakService = mockk<VedtakService>(relaxed = true)
    private val simuleringService = mockk<SimuleringService>()
    private val tilbakekrevingService = mockk<TilbakekrevingService>(relaxed = true)
    private val barnService = mockk<BarnService>(relaxed = true)
    private val fagsakService = mockk<FagsakService>(relaxed = true)
    private val validerOmregningService = mockk<ValiderOmregningService>(relaxed = true)
    private val featureToggleService = mockFeatureToggleService()

    private val steg = BeregnYtelseSteg(
        tilkjentYtelseService,
        andelsHistorikkService,
        beregningService,
        beregningBarnetilsynService,
        beregningSkolepengerService,
        simuleringService,
        vedtakService,
        tilbakekrevingService,
        barnService,
        fagsakService,
        validerOmregningService,
        featureToggleService
    )

    private val slot = slot<TilkjentYtelse>()

    @BeforeEach
    internal fun setUp() {
        every { featureToggleService.isEnabled(any()) } returns true
        every { fagsakService.fagsakMedOppdatertPersonIdent(any()) } returns fagsak(fagsakpersoner(setOf("123")))
        every { simuleringService.hentOgLagreSimuleringsresultat(any()) }
            .returns(
                Simuleringsresultat(
                    behandlingId = UUID.randomUUID(),
                    data = DetaljertSimuleringResultat(emptyList()),
                    beriketData = BeriketSimuleringsresultat(
                        mockk(),
                        mockk()
                    )
                )
            )
        slot.clear()
        every { tilkjentYtelseService.opprettTilkjentYtelse(capture(slot)) } answers { firstArg() }
    }

    @Nested
    inner class UtførSteg {

        @Test
        internal fun `revurdering - nye andeler legges til etter forrige andeler`() {
            val forrigeAndelFom = LocalDate.of(2021, 1, 1)
            val forrigeAndelTom = LocalDate.of(2021, 3, 31)
            val nyAndelFom = LocalDate.of(2022, 1, 1)
            val nyAndelTom = LocalDate.of(2022, 1, 31)

            every { tilkjentYtelseService.hentForBehandling(any()) } returns
                lagTilkjentYtelse(listOf(lagAndelTilkjentYtelse(100, forrigeAndelFom, forrigeAndelTom)))
            every { beregningService.beregnYtelse(any(), any()) }
                .returns(listOf(lagBeløpsperiode(YearMonth.from(nyAndelFom), YearMonth.from(nyAndelTom))))

            utførSteg(BehandlingType.REVURDERING, forrigeBehandlingId = UUID.randomUUID())

            val andeler = slot.captured.andelerTilkjentYtelse
            assertThat(andeler).hasSize(2)
            assertThat(andeler[0].periode.fom).isEqualTo(forrigeAndelFom)
            assertThat(andeler[0].periode.tom).isEqualTo(forrigeAndelTom)

            assertThat(andeler[1].periode.fom).isEqualTo(nyAndelFom)
            assertThat(andeler[1].periode.tom).isEqualTo(nyAndelTom)

            assertThat(andeler[0].kildeBehandlingId).isNotEqualTo(andeler[1].kildeBehandlingId)
            verify(exactly = 1) {
                simuleringService.hentOgLagreSimuleringsresultat(any())
            }
        }

        @Test
        internal fun `revurdering - førstegangsbehandling er avslått - kun nye andeler som skal gjelde`() {
            val nyAndelFom = YearMonth.of(2022, 1)
            val nyAndelTom = YearMonth.of(2022, 1)

            every { tilkjentYtelseService.hentForBehandling(any()) } throws IllegalArgumentException("Hjelp")
            every { beregningService.beregnYtelse(any(), any()) } returns listOf(lagBeløpsperiode(nyAndelFom, nyAndelTom))

            utførSteg(
                BehandlingType.REVURDERING,
                forrigeBehandlingId = null,
                vedtak = innvilget(
                    listOf(
                        vedtaksperiodeDto(
                            årMånedFra = nyAndelFom,
                            årMånedTil = nyAndelTom
                        )
                    ),
                    listOf(inntekt(nyAndelFom))
                )
            )

            val andeler = slot.captured.andelerTilkjentYtelse
            assertThat(andeler).hasSize(1)
            assertThat(andeler[0].periode.fomMåned).isEqualTo(nyAndelFom)
            assertThat(andeler[0].periode.tomMåned).isEqualTo(nyAndelTom)

            verify(exactly = 1) {
                simuleringService.hentOgLagreSimuleringsresultat(any())
            }
        }

        @Test
        internal fun `innvilget - skal kaste feil når man sender inn uten nye beløpsperioder`() {
            every { beregningService.beregnYtelse(any(), any()) } returns emptyList()
            assertThrows<ApiFeil> { utførSteg(BehandlingType.REVURDERING) }
        }

        @Test
        internal fun `førstegangsbehandling - happy case`() {
            every { beregningService.beregnYtelse(any(), any()) } returns listOf(
                lagBeløpsperiode(
                    YearMonth.now(),
                    YearMonth.now()
                )
            )
            utførSteg(BehandlingType.FØRSTEGANGSBEHANDLING)

            verify(exactly = 0) { tilkjentYtelseService.hentForBehandling(any()) }
        }

        @Test
        internal fun `skal opphøre vedtak fra dato`() {
            val opphørFom = YearMonth.of(2021, 6)

            val forrigeAndelFom = LocalDate.of(2021, 1, 1)
            val forrigeAndelTom = LocalDate.of(2021, 12, 31)
            val forventetNyAndelFom = LocalDate.of(2021, 1, 1)
            val forventetNyAndelTom = LocalDate.of(2021, 5, 31)

            every { tilkjentYtelseService.hentForBehandling(any()) } returns
                lagTilkjentYtelse(listOf(lagAndelTilkjentYtelse(100, forrigeAndelFom, forrigeAndelTom)))

            utførSteg(
                BehandlingType.REVURDERING,
                Opphør(opphørFom = opphørFom, begrunnelse = "null"),
                forrigeBehandlingId = UUID.randomUUID()
            )

            assertThat(slot.captured.andelerTilkjentYtelse).hasSize(1)
            assertThat(slot.captured.andelerTilkjentYtelse.first().periode.fom).isEqualTo(forventetNyAndelFom)
            assertThat(slot.captured.andelerTilkjentYtelse.first().periode.tom).isEqualTo(forventetNyAndelTom)
        }

        @Test
        internal fun `skal opphøre vedtak fra samme måned som forrige andel starter`() {
            val opphørFom = YearMonth.of(2021, 1)

            val forrigeAndelFom = LocalDate.of(2021, 1, 1)
            val forrigeAndelTom = LocalDate.of(2021, 12, 31)

            every { tilkjentYtelseService.hentForBehandling(any()) } returns
                lagTilkjentYtelse(listOf(lagAndelTilkjentYtelse(100, forrigeAndelFom, forrigeAndelTom)))

            utførSteg(
                BehandlingType.REVURDERING,
                Opphør(opphørFom = opphørFom, begrunnelse = "null"),
                forrigeBehandlingId = UUID.randomUUID()
            )

            assertThat(slot.captured.andelerTilkjentYtelse).hasSize(0)
        }

        @Test
        internal fun `skal kunne ha ingen stønadsperiode for en førstegangsbehandling`() {
            val innvilgetFom1 = YearMonth.of(2021, 1)
            val innvilgetTom1 = YearMonth.of(2021, 5)
            val opphørFom = YearMonth.of(2021, 6)
            val opphørTom = YearMonth.of(2021, 8)
            val innvilgetFom2 = YearMonth.of(2021, 9)
            val innvilgetTom2 = YearMonth.of(2022, 3)

//            every { tilkjentYtelseService.hentForBehandling(any()) } returns
//                    lagTilkjentYtelse(listOf(lagAndelTilkjentYtelse(100, forrigeAndelFom, forrigeAndelTom)))
            every { beregningService.beregnYtelse(any(), any()) } answers {
                firstArg<List<Månedsperiode>>().map { lagBeløpsperiode(it.fom, it.tom) }
            }

            val opphørsperiode = opphørsperiode(opphørFom, opphørTom)
            val innvilgetPeriode1 = innvilgetPeriode(innvilgetFom1, innvilgetTom1)
            val innvilgetPeriode2 = innvilgetPeriode(innvilgetFom2, innvilgetTom2)

            utførSteg(
                BehandlingType.FØRSTEGANGSBEHANDLING,
                innvilget(
                    listOf(innvilgetPeriode1, opphørsperiode, innvilgetPeriode2),
                    listOf(inntekt(innvilgetFom1))
                ),
                forrigeBehandlingId = UUID.randomUUID()
            )

            val andelerTilkjentYtelse = slot.captured.andelerTilkjentYtelse
            assertThat(andelerTilkjentYtelse.size).isEqualTo(2)
            assertThat(andelerTilkjentYtelse.firstOrNull()?.periode?.fomMåned).isEqualTo(innvilgetFom1)
            assertThat(andelerTilkjentYtelse.firstOrNull()?.periode?.tomMåned)
                .isEqualTo(opphørFom.minusMonths(1))
            assertThat(andelerTilkjentYtelse.lastOrNull()?.periode?.fomMåned).isEqualTo(innvilgetFom2)
            assertThat(andelerTilkjentYtelse.lastOrNull()?.periode?.tomMåned).isEqualTo(innvilgetTom2)
        }

        @Test
        internal fun `skal feile hvis nye perioder ikke er sammenhengende`() {
            val opphørFom = YearMonth.of(2021, 6)
            val opphørTom = YearMonth.of(2021, 8)
            val innvilgetFom = YearMonth.of(2021, 10)
            val innvilgetTom = YearMonth.of(2022, 3)
            val forrigeAndelFom = LocalDate.of(2021, 1, 1)
            val forrigeAndelTom = LocalDate.of(2021, 12, 31)

            every { tilkjentYtelseService.hentForBehandling(any()) } returns
                lagTilkjentYtelse(listOf(lagAndelTilkjentYtelse(100, forrigeAndelFom, forrigeAndelTom)))
            every { beregningService.beregnYtelse(any(), any()) } answers {
                firstArg<List<Månedsperiode>>().map { lagBeløpsperiode(it.fom, it.tom) }
            }

            val opphørsperiode = opphørsperiode(opphørFom, opphørTom)
            val innvilgetPeriode = innvilgetPeriode(innvilgetFom, innvilgetTom)
            assertThrows<ApiFeil> {
                utførSteg(
                    BehandlingType.REVURDERING,
                    innvilget(listOf(opphørsperiode, innvilgetPeriode), listOf(inntekt(innvilgetFom))),
                    forrigeBehandlingId = UUID.randomUUID()
                )
            }
        }

        @Test
        internal fun `skal innvilge med opphør som første periode`() {
            val opphørFom = YearMonth.of(2021, 6)
            val opphørTom = YearMonth.of(2021, 8)
            val innvilgetFom = YearMonth.of(2021, 9)
            val innvilgetTom = YearMonth.of(2022, 3)
            val forrigeAndelFom = LocalDate.of(2021, 1, 1)
            val forrigeAndelTom = LocalDate.of(2021, 12, 31)

            val forventetNyAndelFom1 = LocalDate.of(2021, 1, 1)
            val forventetNyAndelTom1 = LocalDate.of(2021, 5, 31)
            val forventetNyAndelFom2 = LocalDate.of(2021, 9, 1)
            val forventetNyAndelTom2 = LocalDate.of(2022, 3, 31)

            every { tilkjentYtelseService.hentForBehandling(any()) } returns
                lagTilkjentYtelse(listOf(lagAndelTilkjentYtelse(100, forrigeAndelFom, forrigeAndelTom)))
            every { beregningService.beregnYtelse(any(), any()) } answers {
                firstArg<List<Månedsperiode>>().map { lagBeløpsperiode(it.fom, it.tom) }
            }

            val opphørsperiode = opphørsperiode(opphørFom, opphørTom)
            val innvilgetPeriode = innvilgetPeriode(innvilgetFom, innvilgetTom)
            utførSteg(
                BehandlingType.REVURDERING,
                innvilget(
                    listOf(opphørsperiode, innvilgetPeriode),
                    listOf(inntekt(innvilgetFom))
                ),
                forrigeBehandlingId = UUID.randomUUID()
            )

            assertThat(slot.captured.andelerTilkjentYtelse).hasSize(2)
            assertThat(slot.captured.andelerTilkjentYtelse.first().periode.fom).isEqualTo(forventetNyAndelFom1)
            assertThat(slot.captured.andelerTilkjentYtelse.first().periode.tom).isEqualTo(forventetNyAndelTom1)
            assertThat(slot.captured.andelerTilkjentYtelse.last().periode.fom).isEqualTo(forventetNyAndelFom2)
            assertThat(slot.captured.andelerTilkjentYtelse.last().periode.tom).isEqualTo(forventetNyAndelTom2)
        }

        @Test
        internal fun `skal innvilge med opphør midt i perioden`() {
            val innvilgetFom1 = YearMonth.of(2021, 3)
            val innvilgetTom1 = YearMonth.of(2021, 5)
            val opphørFom = YearMonth.of(2021, 6)
            val opphørTom = YearMonth.of(2021, 8)
            val innvilgetFom2 = YearMonth.of(2021, 9)
            val innvilgetTom2 = YearMonth.of(2022, 3)
            val forrigeAndelFom = LocalDate.of(2021, 1, 1)
            val forrigeAndelTom = LocalDate.of(2021, 12, 31)

            val forventetNyAndelFom1 = LocalDate.of(2021, 1, 1)
            val forventetNyAndelTom1 = LocalDate.of(2021, 2, 28)
            val forventetNyAndelFom2 = LocalDate.of(2021, 3, 1)
            val forventetNyAndelTom2 = LocalDate.of(2021, 5, 31)
            val forventetNyAndelFom3 = LocalDate.of(2021, 9, 1)
            val forventetNyAndelTom3 = LocalDate.of(2022, 3, 31)

            every { tilkjentYtelseService.hentForBehandling(any()) } returns
                lagTilkjentYtelse(listOf(lagAndelTilkjentYtelse(100, forrigeAndelFom, forrigeAndelTom)))
            every { beregningService.beregnYtelse(any(), any()) } answers {
                firstArg<List<Månedsperiode>>().map { lagBeløpsperiode(it.fom, it.tom) }
            }

            val innvilgetPeriode1 = innvilgetPeriode(innvilgetFom1, innvilgetTom1)
            val opphørsperiode = opphørsperiode(opphørFom, opphørTom)
            val innvilgetPeriode2 = innvilgetPeriode(innvilgetFom2, innvilgetTom2)

            utførSteg(
                BehandlingType.REVURDERING,
                innvilget(
                    listOf(innvilgetPeriode1, opphørsperiode, innvilgetPeriode2),
                    listOf(inntekt(innvilgetFom1))
                ),
                forrigeBehandlingId = UUID.randomUUID()
            )

            assertThat(slot.captured.andelerTilkjentYtelse).hasSize(3)
            assertThat(slot.captured.andelerTilkjentYtelse[0].periode.fom).isEqualTo(forventetNyAndelFom1)
            assertThat(slot.captured.andelerTilkjentYtelse[0].periode.tom).isEqualTo(forventetNyAndelTom1)
            assertThat(slot.captured.andelerTilkjentYtelse[1].periode.fom).isEqualTo(forventetNyAndelFom2)
            assertThat(slot.captured.andelerTilkjentYtelse[1].periode.tom).isEqualTo(forventetNyAndelTom2)
            assertThat(slot.captured.andelerTilkjentYtelse[2].periode.fom).isEqualTo(forventetNyAndelFom3)
            assertThat(slot.captured.andelerTilkjentYtelse[2].periode.tom).isEqualTo(forventetNyAndelTom3)
        }

        @Test
        internal fun `skal få kun første periode hvis opphørsdato er starten på neste periode som er delt i to`() {
            val opphørFom = YearMonth.of(2021, 9)

            val andel1Fom = LocalDate.of(2021, 1, 1)
            val andel1Tom = LocalDate.of(2021, 6, 30)
            val andel2Fom = LocalDate.of(2021, 9, 1)
            val andel2Tom = LocalDate.of(2021, 12, 31)

            every { tilkjentYtelseService.hentForBehandling(any()) } returns
                lagTilkjentYtelse(
                    listOf(
                        lagAndelTilkjentYtelse(100, andel1Fom, andel1Tom),
                        lagAndelTilkjentYtelse(200, andel2Fom, andel2Tom)
                    )
                )

            utførSteg(
                BehandlingType.REVURDERING,
                Opphør(opphørFom = opphørFom, begrunnelse = "null"),
                forrigeBehandlingId = UUID.randomUUID()
            )

            assertThat(slot.captured.andelerTilkjentYtelse).hasSize(1)
            assertThat(slot.captured.andelerTilkjentYtelse.first().periode.fom).isEqualTo(andel1Fom)
            assertThat(slot.captured.andelerTilkjentYtelse.first().periode.tom).isEqualTo(andel1Tom)
        }

        @Test
        internal fun `skal ikke feile hvis opphørsdato er mellom to perioder`() {
            val opphørFom = YearMonth.of(2021, 8)

            val andel1Fom = LocalDate.of(2021, 1, 1)
            val andel1Tom = LocalDate.of(2021, 6, 30)
            val andel2Fom = LocalDate.of(2021, 9, 1)
            val andel2Tom = LocalDate.of(2021, 12, 31)

            every { tilkjentYtelseService.hentForBehandling(any()) } returns
                lagTilkjentYtelse(
                    listOf(
                        lagAndelTilkjentYtelse(100, andel1Fom, andel1Tom),
                        lagAndelTilkjentYtelse(200, andel2Fom, andel2Tom)
                    )
                )

            utførSteg(
                BehandlingType.REVURDERING,
                Opphør(opphørFom = opphørFom, begrunnelse = "null"),
                forrigeBehandlingId = UUID.randomUUID()
            )

            assertThat(slot.captured.andelerTilkjentYtelse).hasSize(1)
            assertThat(slot.captured.andelerTilkjentYtelse.first().periode.fom).isEqualTo(andel1Fom)
            assertThat(slot.captured.andelerTilkjentYtelse.first().periode.tom).isEqualTo(andel1Tom)
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

            every { tilkjentYtelseService.hentForBehandling(any()) } returns
                lagTilkjentYtelse(
                    listOf(
                        lagAndelTilkjentYtelse(100, andel1Fom, andel1Tom),
                        lagAndelTilkjentYtelse(200, andel2Fom, andel2Tom)
                    )
                )

            utførSteg(
                BehandlingType.REVURDERING,
                Opphør(opphørFom = opphørFom, begrunnelse = "null"),
                forrigeBehandlingId = UUID.randomUUID()
            )

            assertThat(slot.captured.andelerTilkjentYtelse).hasSize(1)
            assertThat(slot.captured.andelerTilkjentYtelse.first().periode.fom).isEqualTo(forventetNyAndelFom)
            assertThat(slot.captured.andelerTilkjentYtelse.first().periode.tom).isEqualTo(forventetNyAndelTom)
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

            every { tilkjentYtelseService.hentForBehandling(any()) } returns
                lagTilkjentYtelse(
                    listOf(
                        lagAndelTilkjentYtelse(100, andel1Fom, andel1Tom),
                        lagAndelTilkjentYtelse(200, andel2Fom, andel2Tom)
                    )
                )

            utførSteg(
                BehandlingType.REVURDERING,
                Opphør(opphørFom = opphørFom, begrunnelse = "null"),
                forrigeBehandlingId = UUID.randomUUID()
            )

            assertThat(slot.captured.andelerTilkjentYtelse).hasSize(2)
            assertThat(slot.captured.andelerTilkjentYtelse[0].periode.fom).isEqualTo(forventetAndelFom1)
            assertThat(slot.captured.andelerTilkjentYtelse[0].periode.tom).isEqualTo(forventetAndelTom1)
            assertThat(slot.captured.andelerTilkjentYtelse[1].periode.fom).isEqualTo(forventetAndelFom2)
            assertThat(slot.captured.andelerTilkjentYtelse[1].periode.tom).isEqualTo(forventetAndelTom2)
        }

        @Test
        internal fun `skal opphøre hvis opphørsdato samsvarer med startdato for første andel`() {
            val opphørFom = YearMonth.of(2021, 1)

            val andelFom = LocalDate.of(2021, 1, 1)
            val andelTom = LocalDate.of(2021, 6, 30)

            every { tilkjentYtelseService.hentForBehandling(any()) } returns
                lagTilkjentYtelse(listOf(lagAndelTilkjentYtelse(100, andelFom, andelTom)))

            utførSteg(
                BehandlingType.REVURDERING,
                Opphør(opphørFom = opphørFom, begrunnelse = "null"),
                forrigeBehandlingId = UUID.randomUUID()
            )

            assertThat(slot.captured.andelerTilkjentYtelse).hasSize(0)
        }

        @Test
        internal fun `skal feile ved opphør, dersom behandlingstype ikke er revurdering`() {
            val feil = assertThrows<ApiFeil> {
                utførSteg(
                    BehandlingType.FØRSTEGANGSBEHANDLING,
                    Opphør(opphørFom = YearMonth.of(2021, 6), begrunnelse = "null"),
                    forrigeBehandlingId = UUID.randomUUID()
                )
            }
            assertThat(feil.feil).contains("Kan kun opphøre ved revurdering")
        }

        @Test
        internal fun `skal slette tilbakekreving og simulering ved avslag`() {
            every { simuleringService.slettSimuleringForBehandling(any()) } just Runs
            every { tilbakekrevingService.slettTilbakekreving(any()) } just Runs
            utførSteg(
                type = BehandlingType.FØRSTEGANGSBEHANDLING,
                vedtak = Avslå(avslåBegrunnelse = "", avslåÅrsak = AvslagÅrsak.VILKÅR_IKKE_OPPFYLT)
            )

            verify { tilbakekrevingService.slettTilbakekreving(any()) }
            verify { simuleringService.slettSimuleringForBehandling(any()) }
        }

        @Test
        internal fun `skal kaste feil hvis man innvilger på feil type stønad`() {
            assertThatThrownBy {
                utførSteg(
                    lagSaksbehandling(stønadType = StønadType.BARNETILSYN),
                    innvilget(emptyList(), emptyList())
                )
            }.isInstanceOf(Feil::class.java)
                .hasMessageContaining("Stønadstype=BARNETILSYN har ikke støtte for InnvilgelseOvergangsstønad")

            assertThatThrownBy {
                utførSteg(
                    lagSaksbehandling(stønadType = StønadType.OVERGANGSSTØNAD),
                    innvilgetBarnetilsyn(YearMonth.of(2021, 1), YearMonth.of(2021, 1))
                )
            }.isInstanceOf(Feil::class.java)
                .hasMessageContaining("Stønadstype=OVERGANGSSTØNAD har ikke støtte for InnvilgelseBarnetilsyn")
        }
    }

    @Nested
    inner class SlåSammenAndelerSomSkalVidereføres {

        @Test
        internal fun `fjerner eksisternede andelere når nye perioder er før forrige andeler`() {
            val forrigeAndelFom = LocalDate.of(2022, 1, 1)
            val forrigeAndelTom = LocalDate.of(2022, 3, 31)
            val nyAndelFom = LocalDate.of(2021, 1, 1)
            val nyAndelTom = LocalDate.of(2021, 1, 31)

            val forrigeAndeler = listOf(lagAndelTilkjentYtelse(50, forrigeAndelFom, forrigeAndelTom))
            val beløpsperioder = listOf(lagAndelTilkjentYtelse(100, nyAndelFom, nyAndelTom))
            val nyeAndeler = steg.slåSammenAndelerSomSkalVidereføres(beløpsperioder, lagTilkjentYtelse(forrigeAndeler), listOf())

            assertThat(nyeAndeler).hasSize(1)
            assertThat(nyeAndeler[0].periode.fom).isEqualTo(nyAndelFom)
            assertThat(nyeAndeler[0].periode.tom).isEqualTo(nyAndelTom)
            assertThat(nyeAndeler[0].beløp).isEqualTo(100)

            assertThat(forrigeAndeler[0].kildeBehandlingId).isNotEqualTo(nyeAndeler[0].kildeBehandlingId)
            assertThat(nyeAndeler[0].kildeBehandlingId).isEqualTo(nyeAndeler[0].kildeBehandlingId)
        }

        @Test
        internal fun `legger på ny periode rett etter forrige periode`() {
            val forrigeAndelFom = LocalDate.of(2021, 1, 1)
            val forrigeAndelTom = LocalDate.of(2021, 10, 31)
            val nyAndelFom = LocalDate.of(2021, 11, 1)
            val nyAndelTom = LocalDate.of(2021, 11, 30)

            val forrigeAndeler = listOf(lagAndelTilkjentYtelse(50, forrigeAndelFom, forrigeAndelTom))
            val beløpsperioder = listOf(lagAndelTilkjentYtelse(100, nyAndelFom, nyAndelTom))
            val nyeAndeler = steg.slåSammenAndelerSomSkalVidereføres(beløpsperioder, lagTilkjentYtelse(forrigeAndeler), listOf())

            assertThat(nyeAndeler).hasSize(2)
            assertThat(nyeAndeler[0].periode.fom).isEqualTo(forrigeAndelFom)
            assertThat(nyeAndeler[0].periode.tom).isEqualTo(forrigeAndelTom)
            assertThat(nyeAndeler[0].beløp).isEqualTo(50)

            assertThat(nyeAndeler[1].periode.fom).isEqualTo(nyAndelFom)
            assertThat(nyeAndeler[1].periode.tom).isEqualTo(nyAndelTom)
            assertThat(nyeAndeler[1].beløp).isEqualTo(100)

            assertThat(nyeAndeler[0].kildeBehandlingId).isNotEqualTo(nyeAndeler[1].kildeBehandlingId)
        }

        @Test
        internal fun `skal avkorte den tidligere perioden og legge på nye perioder`() {
            val forrigeAndelFom = LocalDate.of(2021, 1, 1)
            val forrigeAndelTom = LocalDate.of(2021, 12, 31)
            val nyAndelFom = LocalDate.of(2021, 11, 1)
            val nyAndelTom = LocalDate.of(2021, 11, 30)

            val forrigeAndeler = listOf(lagAndelTilkjentYtelse(50, forrigeAndelFom, forrigeAndelTom))
            val beløpsperioder = listOf(lagAndelTilkjentYtelse(100, nyAndelFom, nyAndelTom))
            val nyeAndeler = steg.slåSammenAndelerSomSkalVidereføres(beløpsperioder, lagTilkjentYtelse(forrigeAndeler), listOf())

            assertThat(nyeAndeler).hasSize(2)
            assertThat(nyeAndeler[0].periode.fom).isEqualTo(forrigeAndelFom)
            assertThat(nyeAndeler[0].periode.tom).isEqualTo(LocalDate.of(2021, 10, 31))
            assertThat(nyeAndeler[0].beløp).isEqualTo(50)

            assertThat(nyeAndeler[1].periode.fom).isEqualTo(nyAndelFom)
            assertThat(nyeAndeler[1].periode.tom).isEqualTo(nyAndelTom)
            assertThat(nyeAndeler[1].beløp).isEqualTo(100)
        }

        @Test
        internal fun `bytter ut siste andelen med en ny andel`() {
            val forrigeAndelFom = LocalDate.of(2021, 1, 1)
            val forrigeAndelTom = LocalDate.of(2021, 10, 31)
            val forrigeAndelFom2 = LocalDate.of(2021, 11, 1)
            val forrigeAndelTom2 = LocalDate.of(2021, 11, 30)
            val nyAndelFom = LocalDate.of(2021, 11, 1)
            val nyAndelTom = LocalDate.of(2021, 11, 30)

            val forrigeAndeler = listOf(
                lagAndelTilkjentYtelse(50, forrigeAndelFom, forrigeAndelTom),
                lagAndelTilkjentYtelse(70, forrigeAndelFom2, forrigeAndelTom2)
            )
            val beløpsperioder = listOf(lagAndelTilkjentYtelse(100, nyAndelFom, nyAndelTom))
            val nyeAndeler = steg.slåSammenAndelerSomSkalVidereføres(beløpsperioder, lagTilkjentYtelse(forrigeAndeler), listOf())

            assertThat(nyeAndeler).hasSize(2)
            assertThat(nyeAndeler[0].periode.fom).isEqualTo(forrigeAndelFom)
            assertThat(nyeAndeler[0].periode.tom).isEqualTo(forrigeAndelTom)
            assertThat(nyeAndeler[0].beløp).isEqualTo(50)

            assertThat(nyeAndeler[1].periode.fom).isEqualTo(nyAndelFom)
            assertThat(nyeAndeler[1].periode.tom).isEqualTo(nyAndelTom)
            assertThat(nyeAndeler[1].beløp).isEqualTo(100)
        }

        @Test
        internal fun `legger på opphold og ny periode`() {
            val forrigeAndelFom = LocalDate.of(2021, 1, 1)
            val forrigeAndelTom = LocalDate.of(2021, 10, 31)
            val nyAndelFom = LocalDate.of(2021, 12, 1)
            val nyAndelTom = LocalDate.of(2021, 12, 31)
            val forrigeAndeler = listOf(lagAndelTilkjentYtelse(50, forrigeAndelFom, forrigeAndelTom))
            val opphørsperioder = listOf(Månedsperiode(YearMonth.of(2021, 11), YearMonth.of(2021, 11)))
            val beløpsperioder = listOf(lagAndelTilkjentYtelse(100, nyAndelFom, nyAndelTom))

            val nyeAndeler =
                steg.slåSammenAndelerSomSkalVidereføres(beløpsperioder, lagTilkjentYtelse(forrigeAndeler), opphørsperioder)

            assertThat(nyeAndeler).containsExactlyElementsOf(forrigeAndeler + beløpsperioder)
        }

        @Test
        internal fun `legger på ny periode med flere opphold`() {
            val forrigeAndelFom = LocalDate.of(2021, 1, 1)
            val forrigeAndelTom = LocalDate.of(2021, 10, 31)
            val nyAndelFom = LocalDate.of(2021, 11, 1)
            val nyAndelTom = LocalDate.of(2022, 12, 31)
            val opphør1 = Månedsperiode(YearMonth.of(2021, 12), YearMonth.of(2021, 12))
            val opphør2 = Månedsperiode(YearMonth.of(2022, 2), YearMonth.of(2022, 3))
            val opphør3 = Månedsperiode(YearMonth.of(2022, 6), YearMonth.of(2022, 8))
            val opphørsperioder = listOf(opphør1, opphør2, opphør3)
            val forrigeAndeler = listOf(lagAndelTilkjentYtelse(50, forrigeAndelFom, forrigeAndelTom))
            val beløpsperioder = listOf(lagAndelTilkjentYtelse(100, nyAndelFom, nyAndelTom))

            val nyeAndeler =
                steg.slåSammenAndelerSomSkalVidereføres(beløpsperioder, lagTilkjentYtelse(forrigeAndeler), opphørsperioder)

            assertThat(nyeAndeler).hasSize(5)
            assertThat(nyeAndeler[0].periode.fom).isEqualTo(forrigeAndelFom)
            assertThat(nyeAndeler[0].periode.tom).isEqualTo(forrigeAndelTom)
            assertThat(nyeAndeler[0].beløp).isEqualTo(50)
            assertThat(nyeAndeler[0].kildeBehandlingId).isEqualTo(forrigeAndeler[0].kildeBehandlingId)
            assertThat(nyeAndeler[1].periode.fom).isEqualTo(nyAndelFom)
            assertThat(nyeAndeler[1].periode.tomMåned).isEqualTo(opphør1.fom.minusMonths(1))
            assertThat(nyeAndeler[1].beløp).isEqualTo(100)
            assertThat(nyeAndeler[1].kildeBehandlingId).isEqualTo(beløpsperioder[0].kildeBehandlingId)
            assertThat(nyeAndeler[2].periode.fomMåned).isEqualTo(opphør1.tom.plusMonths(1))
            assertThat(nyeAndeler[2].periode.tomMåned).isEqualTo(opphør2.fom.minusMonths(1))
            assertThat(nyeAndeler[2].beløp).isEqualTo(100)
            assertThat(nyeAndeler[2].kildeBehandlingId).isEqualTo(beløpsperioder[0].kildeBehandlingId)
            assertThat(nyeAndeler[3].periode.fomMåned).isEqualTo(opphør2.tom.plusMonths(1))
            assertThat(nyeAndeler[3].periode.tomMåned).isEqualTo(opphør3.fom.minusMonths(1))
            assertThat(nyeAndeler[3].beløp).isEqualTo(100)
            assertThat(nyeAndeler[3].kildeBehandlingId).isEqualTo(beløpsperioder[0].kildeBehandlingId)
            assertThat(nyeAndeler[4].periode.fomMåned).isEqualTo(opphør3.tom.plusMonths(1))
            assertThat(nyeAndeler[4].periode.tom).isEqualTo(nyAndelTom)
            assertThat(nyeAndeler[4].beløp).isEqualTo(100)
            assertThat(nyeAndeler[4].kildeBehandlingId).isEqualTo(beløpsperioder[0].kildeBehandlingId)
            assertThat(nyeAndeler[0].kildeBehandlingId).isNotEqualTo(nyeAndeler[1].kildeBehandlingId)
        }

        @Test
        internal fun `lager opphold i eksisterende andel, ny beløpsperiode med nytt opphold`() {
            val forrigeAndelFom = LocalDate.of(2021, 1, 1)
            val forrigeAndelTom = LocalDate.of(2025, 10, 31)
            val opphør1 = Månedsperiode(YearMonth.of(2021, 12), YearMonth.of(2021, 12))
            val nyAndelFom1 = LocalDate.of(2022, 1, 1)
            val nyAndelTom1 = LocalDate.of(2022, 3, 31)
            val opphør2 = Månedsperiode(YearMonth.of(2022, 4), YearMonth.of(2022, 8))
            val nyAndelFom2 = LocalDate.of(2022, 9, 1)
            val nyAndelTom2 = LocalDate.of(2022, 9, 30)
            val opphørsperioder = listOf(opphør1, opphør2)
            val forrigeAndeler = listOf(lagAndelTilkjentYtelse(50, forrigeAndelFom, forrigeAndelTom))
            val beløpsperioder = listOf(
                lagAndelTilkjentYtelse(100, nyAndelFom1, nyAndelTom1),
                lagAndelTilkjentYtelse(150, nyAndelFom2, nyAndelTom2)
            )

            val nyeAndeler =
                steg.slåSammenAndelerSomSkalVidereføres(beløpsperioder, lagTilkjentYtelse(forrigeAndeler), opphørsperioder)

            assertThat(nyeAndeler).hasSize(3)
            assertThat(nyeAndeler[0].periode.fom).isEqualTo(forrigeAndelFom)
            assertThat(nyeAndeler[0].periode.tomMåned).isEqualTo(opphør1.fom.minusMonths(1))
            assertThat(nyeAndeler[0].beløp).isEqualTo(50)
            assertThat(nyeAndeler[0].kildeBehandlingId).isEqualTo(forrigeAndeler[0].kildeBehandlingId)
            assertThat(nyeAndeler[1].periode.fom).isEqualTo(nyAndelFom1)
            assertThat(nyeAndeler[1].periode.tom).isEqualTo(nyAndelTom1)
            assertThat(nyeAndeler[1].beløp).isEqualTo(100)
            assertThat(nyeAndeler[1].kildeBehandlingId).isNotEqualTo(forrigeAndeler[0].kildeBehandlingId)
            assertThat(nyeAndeler[2].periode.fom).isEqualTo(nyAndelFom2)
            assertThat(nyeAndeler[2].periode.tom).isEqualTo(nyAndelTom2)
            assertThat(nyeAndeler[2].beløp).isEqualTo(150)
            assertThat(nyeAndeler[2].kildeBehandlingId).isNotEqualTo(forrigeAndeler[0].kildeBehandlingId)
        }

        @Test
        fun `lager opphold på slutten av forrige andel uten feil`() {
            val forrigeAndelFom = LocalDate.of(2021, 1, 1)
            val forrigeAndelTom = LocalDate.of(2021, 10, 31)
            val nyAndelFom = LocalDate.of(2021, 10, 1)
            val nyAndelTom = LocalDate.of(2022, 12, 31)
            val opphør1 = Månedsperiode(YearMonth.of(2021, 9), YearMonth.of(2021, 9))
            val opphørsperioder = listOf(opphør1)
            val forrigeAndeler = listOf(lagAndelTilkjentYtelse(50, forrigeAndelFom, forrigeAndelTom))
            val beløpsperioder = listOf(lagAndelTilkjentYtelse(100, nyAndelFom, nyAndelTom))

            val nyeAndeler =
                steg.slåSammenAndelerSomSkalVidereføres(beløpsperioder, lagTilkjentYtelse(forrigeAndeler), opphørsperioder)

            assertThat(nyeAndeler).hasSize(2)
            assertThat(nyeAndeler[0].periode.fom).isEqualTo(forrigeAndelFom)
            assertThat(nyeAndeler[0].periode.tomMåned).isEqualTo(opphør1.fom.minusMonths(1))
            assertThat(nyeAndeler[0].beløp).isEqualTo(50)
            assertThat(nyeAndeler[0].kildeBehandlingId).isEqualTo(forrigeAndeler[0].kildeBehandlingId)
            assertThat(nyeAndeler[1].periode.fom).isEqualTo(nyAndelFom)
            assertThat(nyeAndeler[1].periode.tom).isEqualTo(nyAndelTom)
            assertThat(nyeAndeler[1].beløp).isEqualTo(100)
            assertThat(nyeAndeler[1].kildeBehandlingId).isNotEqualTo(forrigeAndeler[0].kildeBehandlingId)
        }

        @Test
        fun `lager opphold på begynnelsen av ny andel uten feil`() {
            val forrigeAndelFom = LocalDate.of(2021, 1, 1)
            val forrigeAndelTom = LocalDate.of(2024, 10, 31)
            val nyAndelFom1 = LocalDate.of(2021, 8, 1)
            val nyAndelTom1 = LocalDate.of(2021, 9, 30)
            val nyAndelFom2 = LocalDate.of(2021, 10, 1)
            val nyAndelTom2 = LocalDate.of(2022, 12, 31)
            val opphør1 = Månedsperiode(YearMonth.of(2021, 7), YearMonth.of(2021, 7))
            val opphør2 = Månedsperiode(YearMonth.of(2021, 10), YearMonth.of(2021, 10))
            val opphørsperioder = listOf(opphør1, opphør2)
            val forrigeAndeler = listOf(lagAndelTilkjentYtelse(50, forrigeAndelFom, forrigeAndelTom))
            val beløpsperioder = listOf(
                lagAndelTilkjentYtelse(200, nyAndelFom1, nyAndelTom1),
                lagAndelTilkjentYtelse(100, nyAndelFom2, nyAndelTom2)
            )

            val nyeAndeler =
                steg.slåSammenAndelerSomSkalVidereføres(beløpsperioder, lagTilkjentYtelse(forrigeAndeler), opphørsperioder)

            assertThat(nyeAndeler).hasSize(3)
            assertThat(nyeAndeler[0].periode.fom).isEqualTo(forrigeAndelFom)
            assertThat(nyeAndeler[0].periode.tomMåned).isEqualTo(opphør1.fom.minusMonths(1))
            assertThat(nyeAndeler[0].beløp).isEqualTo(50)
            assertThat(nyeAndeler[0].kildeBehandlingId).isEqualTo(forrigeAndeler[0].kildeBehandlingId)
            assertThat(nyeAndeler[1].periode.fom).isEqualTo(nyAndelFom1)
            assertThat(nyeAndeler[1].periode.tom).isEqualTo(nyAndelTom1)
            assertThat(nyeAndeler[1].beløp).isEqualTo(200)
            assertThat(nyeAndeler[1].kildeBehandlingId).isNotEqualTo(forrigeAndeler[0].kildeBehandlingId)
            assertThat(nyeAndeler[2].periode.fomMåned).isEqualTo(opphør2.tom.plusMonths(1))
            assertThat(nyeAndeler[2].periode.tom).isEqualTo(nyAndelTom2)
            assertThat(nyeAndeler[2].beløp).isEqualTo(100)
            assertThat(nyeAndeler[2].kildeBehandlingId).isNotEqualTo(forrigeAndeler[0].kildeBehandlingId)
        }

        @Test
        fun `takler opphold som dekker en hel periode`() {
            val forrigeAndelFom1 = LocalDate.of(2021, 1, 1)
            val forrigeAndelTom1 = LocalDate.of(2021, 10, 31)
            val forrigeAndelFom2 = LocalDate.of(2021, 12, 1)
            val forrigeAndelTom2 = LocalDate.of(2021, 12, 31)
            val opphør1 = Månedsperiode(YearMonth.of(2021, 7), YearMonth.of(2021, 12))
            val opphørsperioder = listOf(opphør1)
            val forrigeAndeler = listOf(
                lagAndelTilkjentYtelse(200, forrigeAndelFom1, forrigeAndelTom1),
                lagAndelTilkjentYtelse(100, forrigeAndelFom2, forrigeAndelTom2)
            )

            val nyeAndeler = steg.slåSammenAndelerSomSkalVidereføres(listOf(), lagTilkjentYtelse(forrigeAndeler), opphørsperioder)

            assertThat(nyeAndeler).hasSize(1)
            assertThat(nyeAndeler[0].periode.fom).isEqualTo(forrigeAndelFom1)
            assertThat(nyeAndeler[0].periode.tomMåned).isEqualTo(opphør1.fom.minusMonths(1))
            assertThat(nyeAndeler[0].beløp).isEqualTo(200)
            assertThat(nyeAndeler[0].kildeBehandlingId).isEqualTo(forrigeAndeler[0].kildeBehandlingId)
        }

        @Test
        fun `takler opphold som dekker flere perioder`() {
            val forrigeAndelFom1 = LocalDate.of(2021, 1, 1)
            val forrigeAndelTom1 = LocalDate.of(2021, 12, 31)
            val opphør1 = Månedsperiode(YearMonth.of(2021, 3), YearMonth.of(2021, 6))
            val opphør2 = Månedsperiode(YearMonth.of(2021, 8), YearMonth.of(2021, 11))
            val opphørsperioder = listOf(opphør1, opphør2)
            val forrigeAndeler = listOf(lagAndelTilkjentYtelse(200, forrigeAndelFom1, forrigeAndelTom1))

            val nyeAndeler = steg.slåSammenAndelerSomSkalVidereføres(listOf(), lagTilkjentYtelse(forrigeAndeler), opphørsperioder)

            assertThat(nyeAndeler).hasSize(1)
            assertThat(nyeAndeler[0].periode.fom).isEqualTo(LocalDate.of(2021, 1, 1))
            assertThat(nyeAndeler[0].periode.tom).isEqualTo(LocalDate.of(2021, 2, 28))
        }

        @Test
        fun `takler to opphørsperioder som i praksis omslutter hele andelen`() {
            val forrigeAndelFom1 = LocalDate.of(2021, 1, 1)
            val forrigeAndelTom1 = LocalDate.of(2021, 12, 31)
            val opphør1 = Månedsperiode(YearMonth.of(2021, 1), YearMonth.of(2021, 6))
            val opphør2 = Månedsperiode(YearMonth.of(2021, 7), YearMonth.of(2021, 12))
            val opphørsperioder = listOf(opphør1, opphør2)
            val forrigeAndeler = listOf(lagAndelTilkjentYtelse(200, forrigeAndelFom1, forrigeAndelTom1))

            val nyeAndeler = steg.slåSammenAndelerSomSkalVidereføres(listOf(), lagTilkjentYtelse(forrigeAndeler), opphørsperioder)

            assertThat(nyeAndeler).hasSize(0)
        }
    }

    @Nested
    inner class InnvilgetMedOpphør {

        @Test
        internal fun `skal kunne innvilge med opphør bak i tid`() {
            val opphørFom = YearMonth.of(2021, 5)
            val andelFom = YearMonth.of(2021, 6)
            val andelTom = YearMonth.of(2022, 6)

            every { tilkjentYtelseService.hentForBehandling(any()) } returns
                lagTilkjentYtelse(listOf(lagAndelTilkjentYtelse(100, andelFom.atDay(1), andelTom.atEndOfMonth())))
            every { beregningService.beregnYtelse(any(), any()) } answers {
                firstArg<List<Månedsperiode>>().map { lagBeløpsperiode(it.fom, it.tom) }
            }

            val opphørsperiode = opphørsperiode(opphørFom, opphørFom)
            val innvilgetPeriode1 = innvilgetPeriode(andelFom, andelTom)

            utførSteg(
                BehandlingType.REVURDERING,
                innvilget(
                    listOf(opphørsperiode, innvilgetPeriode1),
                    listOf(inntekt(andelTom))
                ),
                forrigeBehandlingId = UUID.randomUUID()
            )

            assertThat(slot.captured.andelerTilkjentYtelse).hasSize(1)
            assertThat(slot.captured.andelerTilkjentYtelse[0].periode.fomMåned).isEqualTo(andelFom)
            assertThat(slot.captured.andelerTilkjentYtelse[0].periode.tomMåned).isEqualTo(andelTom)
            assertThat(slot.captured.startmåned).isEqualTo(opphørFom)
        }

        @Test
        internal fun `skal kunne innvilge med opphør med opphør midt i en periode - bruker forrige startdato`() {
            val opphørFom = YearMonth.of(2021, 8)
            val andelFom = LocalDate.of(2021, 6, 1)
            val andelTom = LocalDate.of(2022, 6, 30)
            val innvilgetFom = opphørFom.plusMonths(1)
            val innvilgetTom = YearMonth.from(andelTom)

            every { tilkjentYtelseService.hentForBehandling(any()) } returns
                lagTilkjentYtelse(listOf(lagAndelTilkjentYtelse(100, andelFom, andelTom)))
            every { beregningService.beregnYtelse(any(), any()) } answers {
                firstArg<List<Månedsperiode>>().map { lagBeløpsperiode(it.fom, it.tom) }
            }

            val opphørsperiode = opphørsperiode(opphørFom, opphørFom)
            val innvilgetPeriode1 = innvilgetPeriode(innvilgetFom, innvilgetTom)

            utførSteg(
                BehandlingType.REVURDERING,
                innvilget(
                    listOf(opphørsperiode, innvilgetPeriode1),
                    listOf(inntekt(innvilgetTom))
                ),
                forrigeBehandlingId = UUID.randomUUID()
            )

            val andelerTilkjentYtelse = slot.captured.andelerTilkjentYtelse.sortedBy { it.periode.fom }
            assertThat(andelerTilkjentYtelse).hasSize(2)
            assertThat(andelerTilkjentYtelse[0].periode.fom).isEqualTo(andelFom)
            assertThat(andelerTilkjentYtelse[0].periode.tomMåned).isEqualTo(opphørFom.minusMonths(1))
            assertThat(andelerTilkjentYtelse[1].periode.fomMåned).isEqualTo(innvilgetFom)
            assertThat(andelerTilkjentYtelse[1].periode.tomMåned).isEqualTo(innvilgetTom)
            assertThat(slot.captured.startmåned).isEqualTo(YearMonth.from(andelFom))
        }

        @Test
        internal fun `opphør etter innvilget periode - beholder startdato`() {
            val opphørFom = YearMonth.of(2022, 7)
            val andelFom = YearMonth.of(2021, 6)
            val andelTom = YearMonth.of(2022, 6)

            every { tilkjentYtelseService.hentForBehandling(any()) } returns
                lagTilkjentYtelse(listOf(lagAndelTilkjentYtelse(100, andelFom.atDay(1), andelTom.atEndOfMonth())))
            every { beregningService.beregnYtelse(any(), any()) } answers {
                firstArg<List<Månedsperiode>>().map { lagBeløpsperiode(it.fom, it.tom) }
            }

            val opphørsperiode = opphørsperiode(opphørFom, opphørFom)
            val innvilgetPeriode1 = innvilgetPeriode(andelFom, andelTom)

            utførSteg(
                BehandlingType.REVURDERING,
                innvilget(
                    listOf(innvilgetPeriode1, opphørsperiode),
                    listOf(inntekt(andelTom))
                ),
                forrigeBehandlingId = UUID.randomUUID()
            )

            assertThat(slot.captured.andelerTilkjentYtelse).hasSize(1)
            assertThat(slot.captured.andelerTilkjentYtelse[0].periode.fomMåned).isEqualTo(andelFom)
            assertThat(slot.captured.andelerTilkjentYtelse[0].periode.tomMåned).isEqualTo(andelTom)
            assertThat(slot.captured.startmåned).isEqualTo(andelFom)
        }

        @Test
        internal fun `kan ikke innvilge med opphør, uten opphørsperioder`() {
            val opphørFom = YearMonth.of(2021, 1)
            val andelFom = LocalDate.of(2021, 6, 1)
            val andelTom = LocalDate.of(2022, 6, 30)

            every { tilkjentYtelseService.hentForBehandling(any()) } returns
                lagTilkjentYtelse(listOf(lagAndelTilkjentYtelse(100, andelFom, andelTom)))

            assertThatThrownBy {
                utførSteg(
                    BehandlingType.REVURDERING,
                    innvilget(listOf(opphørsperiode(opphørFom, opphørFom)), listOf(inntekt(opphørFom))),
                    forrigeBehandlingId = UUID.randomUUID()
                )
            }.hasMessageContaining("Må ha innvilgelsesperioder i tillegg til opphørsperioder")
        }

        @Test
        internal fun `kan ikke innvilge med opphør før innvilget perioder når man ikke har tidligere behandling`() {
            val opphørFom = YearMonth.of(2021, 1)
            val innvilgetMåned = opphørFom.plusMonths(1)

            every { beregningService.beregnYtelse(any(), any()) } answers {
                firstArg<List<Månedsperiode>>().map { lagBeløpsperiode(it.fom, it.tom) }
            }

            assertThatThrownBy {
                utførSteg(
                    BehandlingType.REVURDERING,
                    innvilget(
                        listOf(
                            opphørsperiode(opphørFom, opphørFom),
                            innvilgetPeriode(innvilgetMåned, innvilgetMåned)
                        ),
                        listOf(inntekt(opphørFom))
                    )
                )
            }.hasMessageContaining("Har ikke støtte for å innvilge med opphør først, når man mangler tidligere behandling å opphøre")
        }

        @Test
        internal fun `kan innvilge med opphør, når tidligere andeler kun inneholder 0-beløp`() {
            val opphørFom = YearMonth.of(2021, 1)
            val andelFom = YearMonth.of(2021, 6).atDay(1)
            val andelTom = YearMonth.of(2021, 6).atEndOfMonth()
            val innvilgetMåned = opphørFom.plusMonths(1)

            every { tilkjentYtelseService.hentForBehandling(any()) } returns
                lagTilkjentYtelse(listOf(lagAndelTilkjentYtelse(0, andelFom, andelTom)))
            every { beregningService.beregnYtelse(any(), any()) } answers {
                firstArg<List<Månedsperiode>>().map { lagBeløpsperiode(it.fom, it.tom) }
            }

            utførSteg(
                BehandlingType.REVURDERING,
                innvilget(
                    listOf(
                        opphørsperiode(opphørFom, opphørFom),
                        innvilgetPeriode(innvilgetMåned, innvilgetMåned)
                    ),
                    listOf(inntekt(opphørFom))
                ),
                forrigeBehandlingId = UUID.randomUUID()
            )

            assertThat(slot.captured.startmåned).isEqualTo(opphørFom)
            assertThat(slot.captured.andelerTilkjentYtelse).hasSize(1)
            assertThat(slot.captured.andelerTilkjentYtelse[0].periode.fomMåned).isEqualTo(innvilgetMåned)
            assertThat(slot.captured.andelerTilkjentYtelse[0].periode.tomMåned).isEqualTo(innvilgetMåned)
        }

        @Test
        internal fun `skal kunne innvilge når vi kun har 0-perioder fra før`() {
            val andelFom = YearMonth.of(2021, 6).atDay(1)
            val andelTom = YearMonth.of(2021, 6).atEndOfMonth()
            val innvilgetMåned = YearMonth.of(2021, 1)

            every { tilkjentYtelseService.hentForBehandling(any()) } returns
                lagTilkjentYtelse(listOf(lagAndelTilkjentYtelse(0, andelFom, andelTom)))
            every { beregningService.beregnYtelse(any(), any()) } answers {
                firstArg<List<Månedsperiode>>().map { lagBeløpsperiode(it.fom, it.tom) }
            }

            utførSteg(
                BehandlingType.REVURDERING,
                innvilget(listOf(innvilgetPeriode(innvilgetMåned, innvilgetMåned)), listOf(inntekt(innvilgetMåned))),
                forrigeBehandlingId = UUID.randomUUID()
            )

            assertThat(slot.captured.startmåned).isEqualTo(innvilgetMåned)
            assertThat(slot.captured.andelerTilkjentYtelse).hasSize(1)
            assertThat(slot.captured.andelerTilkjentYtelse[0].periode.fomMåned).isEqualTo(innvilgetMåned)
            assertThat(slot.captured.andelerTilkjentYtelse[0].periode.tomMåned).isEqualTo(innvilgetMåned)
        }

        @Test
        internal fun `skal kunne innvilge når vi kun har 0-perioder og opphørsdato fra før`() {
            val opphørFom = YearMonth.of(2021, 1)
            val andelFom = YearMonth.of(2021, 6).atDay(1)
            val andelTom = YearMonth.of(2021, 6).atEndOfMonth()
            val innvilgetMåned = opphørFom.plusMonths(1)

            every { tilkjentYtelseService.hentForBehandling(any()) } returns
                lagTilkjentYtelse(
                    listOf(lagAndelTilkjentYtelse(0, andelFom, andelTom)),
                    startmåned = opphørFom
                )
            every { beregningService.beregnYtelse(any(), any()) } answers {
                firstArg<List<Månedsperiode>>().map { lagBeløpsperiode(it.fom, it.tom) }
            }

            utførSteg(
                BehandlingType.REVURDERING,
                innvilget(
                    listOf(innvilgetPeriode(innvilgetMåned, innvilgetMåned)),
                    listOf(inntekt(innvilgetMåned))
                ),
                forrigeBehandlingId = UUID.randomUUID()
            )

            assertThat(slot.captured.startmåned).isEqualTo(opphørFom)
            assertThat(slot.captured.andelerTilkjentYtelse).hasSize(1)
            assertThat(slot.captured.andelerTilkjentYtelse[0].periode.fomMåned).isEqualTo(innvilgetMåned)
            assertThat(slot.captured.andelerTilkjentYtelse[0].periode.tomMåned).isEqualTo(innvilgetMåned)
        }

        @Test
        internal fun `skal kunne innvilge med opphør før andeler sitt startdato, med ny innvilget periode - setter nytt opphørsdato`() {
            val opphørFom = YearMonth.of(2021, 1)
            val nyttInnvilgetFom = opphørFom.plusMonths(1)
            val andelFom = YearMonth.of(2021, 6).atDay(1)
            val andelTom = YearMonth.of(2021, 6).atEndOfMonth()

            every { tilkjentYtelseService.hentForBehandling(any()) } returns
                lagTilkjentYtelse(listOf(lagAndelTilkjentYtelse(100, andelFom, andelTom)))
            every { beregningService.beregnYtelse(any(), any()) } answers {
                firstArg<List<Månedsperiode>>().map { lagBeløpsperiode(it.fom, it.tom) }
            }

            utførSteg(
                BehandlingType.REVURDERING,
                innvilget(
                    listOf(
                        opphørsperiode(opphørFom, opphørFom),
                        innvilgetPeriode(nyttInnvilgetFom, nyttInnvilgetFom)
                    ),
                    listOf(inntekt(opphørFom))
                ),
                forrigeBehandlingId = UUID.randomUUID()
            )

            assertThat(slot.captured.startmåned).isEqualTo(opphørFom)
            assertThat(slot.captured.andelerTilkjentYtelse).hasSize(1)
            assertThat(slot.captured.andelerTilkjentYtelse[0].periode.fomMåned).isEqualTo(nyttInnvilgetFom)
            assertThat(slot.captured.andelerTilkjentYtelse[0].periode.tomMåned).isEqualTo(nyttInnvilgetFom)
        }

        @Test
        internal fun `finnes tidligere opphørsdato fra før, og vurderer med opphør etter det datoet - beholder tidligere opphørsdato`() {
            val tidligereOpphør = YearMonth.of(2020, 1)
            val nyttOpphørsdato = YearMonth.of(2021, 1)
            val nyttInnvilgetFom = nyttOpphørsdato.plusMonths(1)
            val andelFom = YearMonth.of(2021, 6).atDay(1)
            val andelTom = YearMonth.of(2021, 6).atEndOfMonth()

            every { tilkjentYtelseService.hentForBehandling(any()) } returns
                lagTilkjentYtelse(
                    listOf(lagAndelTilkjentYtelse(100, andelFom, andelTom)),
                    startmåned = tidligereOpphør
                )
            every { beregningService.beregnYtelse(any(), any()) } answers {
                firstArg<List<Månedsperiode>>().map { lagBeløpsperiode(it.fom, it.tom) }
            }

            utførSteg(
                BehandlingType.REVURDERING,
                innvilget(
                    listOf(
                        opphørsperiode(nyttOpphørsdato, nyttOpphørsdato),
                        innvilgetPeriode(nyttInnvilgetFom, nyttInnvilgetFom)
                    ),
                    listOf(inntekt(nyttOpphørsdato))
                ),
                forrigeBehandlingId = UUID.randomUUID()
            )

            assertThat(slot.captured.startmåned).isEqualTo(tidligereOpphør)
            assertThat(slot.captured.andelerTilkjentYtelse).hasSize(1)
            assertThat(slot.captured.andelerTilkjentYtelse[0].periode.fomMåned).isEqualTo(nyttInnvilgetFom)
            assertThat(slot.captured.andelerTilkjentYtelse[0].periode.tomMåned).isEqualTo(nyttInnvilgetFom)
        }

        @Test
        internal fun `finnes tidligere opphørsdato fra før, og vurderer med opphør etter før det datoet`() {
            val tidligereOpphør = YearMonth.of(2021, 6)
            val nyttOpphørsdato = YearMonth.of(2021, 1)
            val nyttInnvilgetFom = nyttOpphørsdato.plusMonths(1)
            val andelFom = YearMonth.of(2021, 6).atDay(1)
            val andelTom = YearMonth.of(2021, 6).atEndOfMonth()

            every { tilkjentYtelseService.hentForBehandling(any()) } returns
                lagTilkjentYtelse(
                    listOf(lagAndelTilkjentYtelse(100, andelFom, andelTom)),
                    startmåned = tidligereOpphør
                )
            every { beregningService.beregnYtelse(any(), any()) } answers {
                firstArg<List<Månedsperiode>>().map { lagBeløpsperiode(it.fom, it.tom) }
            }

            utførSteg(
                BehandlingType.REVURDERING,
                innvilget(
                    listOf(
                        opphørsperiode(nyttOpphørsdato, nyttOpphørsdato),
                        innvilgetPeriode(nyttInnvilgetFom, nyttInnvilgetFom)
                    ),
                    listOf(inntekt(nyttOpphørsdato))
                ),
                forrigeBehandlingId = UUID.randomUUID()
            )

            assertThat(slot.captured.startmåned).isEqualTo(nyttOpphørsdato)
            assertThat(slot.captured.andelerTilkjentYtelse).hasSize(1)
            assertThat(slot.captured.andelerTilkjentYtelse[0].periode.fomMåned).isEqualTo(nyttInnvilgetFom)
            assertThat(slot.captured.andelerTilkjentYtelse[0].periode.tomMåned).isEqualTo(nyttInnvilgetFom)
        }
    }

    @Nested
    inner class Opphørt {

        @Test
        internal fun `skal kunne opphøre bak i tid - skal sette opphørsdato på tilkjent ytelse`() {
            val opphørFom = YearMonth.of(2021, 1)
            val andelFom = YearMonth.of(2021, 6).atDay(1)
            val andelTom = YearMonth.of(2021, 6).atEndOfMonth()

            every { tilkjentYtelseService.hentForBehandling(any()) } returns
                lagTilkjentYtelse(listOf(lagAndelTilkjentYtelse(100, andelFom, andelTom)))

            utførSteg(
                BehandlingType.REVURDERING,
                Opphør(opphørFom = opphørFom, begrunnelse = "null"),
                forrigeBehandlingId = UUID.randomUUID()
            )

            assertThat(slot.captured.andelerTilkjentYtelse).hasSize(0)
            assertThat(slot.captured.startmåned).isEqualTo(opphørFom)
        }

        @Test
        internal fun `skal kunne opphøre midt i en tidligere periode`() {
            val opphørFom = YearMonth.of(2021, 8)
            val andelFom = YearMonth.of(2021, 6).atDay(1)
            val andelTom = YearMonth.of(2022, 6).atEndOfMonth()

            every { tilkjentYtelseService.hentForBehandling(any()) } returns
                lagTilkjentYtelse(listOf(lagAndelTilkjentYtelse(100, andelFom, andelTom)))

            utførSteg(
                BehandlingType.REVURDERING,
                Opphør(opphørFom = opphørFom, begrunnelse = "null"),
                forrigeBehandlingId = UUID.randomUUID()
            )

            assertThat(slot.captured.startmåned).isEqualTo(YearMonth.from(andelFom))
            assertThat(slot.captured.andelerTilkjentYtelse).hasSize(1)
            assertThat(slot.captured.andelerTilkjentYtelse[0].periode.fom).isEqualTo(andelFom)
            assertThat(slot.captured.andelerTilkjentYtelse[0].periode.tomMåned).isEqualTo(opphørFom.minusMonths(1))
        }

        @Test
        internal fun `skal ikke kunne opphøre frem i tid`() {
            val opphørFom = YearMonth.of(2022, 1)
            val andelFom = YearMonth.of(2021, 6).atDay(1)
            val andelTom = YearMonth.of(2021, 6).atEndOfMonth()

            every { tilkjentYtelseService.hentForBehandling(any()) } returns
                lagTilkjentYtelse(listOf(lagAndelTilkjentYtelse(100, andelFom, andelTom)))

            assertThatThrownBy {
                utførSteg(
                    BehandlingType.REVURDERING,
                    Opphør(opphørFom = opphørFom, begrunnelse = "null"),
                    forrigeBehandlingId = UUID.randomUUID()
                )
            }.hasMessageContaining("Kan ikke opphøre frem i tiden")
        }

        @Test
        internal fun `skal kunne opphøre før datoet for ett tidligere opphør`() {
            val opphørFom = YearMonth.of(2021, 1)
            val tidligereAndelFom = YearMonth.of(2022, 1).atDay(1)
            val tidligereAndelTom = YearMonth.of(2022, 1).atEndOfMonth()

            every { tilkjentYtelseService.hentForBehandling(any()) } returns
                lagTilkjentYtelse(
                    listOf(lagAndelTilkjentYtelse(100, tidligereAndelFom, tidligereAndelTom)),
                    startmåned = opphørFom.plusMonths(1)
                )

            utførSteg(
                BehandlingType.REVURDERING,
                Opphør(opphørFom = opphørFom, begrunnelse = "null"),
                forrigeBehandlingId = UUID.randomUUID()
            )
            assertThat(slot.captured.startmåned).isEqualTo(opphørFom)
            assertThat(slot.captured.andelerTilkjentYtelse).isEmpty()
        }

        @Test
        internal fun `skal ikke kunne opphøre etter datoet for ett tidligere opphør`() {
            val opphørFom = YearMonth.of(2022, 1)

            every { tilkjentYtelseService.hentForBehandling(any()) } returns
                lagTilkjentYtelse(listOf(), startmåned = opphørFom.minusMonths(1))

            assertThatThrownBy {
                utførSteg(
                    BehandlingType.REVURDERING,
                    Opphør(opphørFom = opphørFom, begrunnelse = "null"),
                    forrigeBehandlingId = UUID.randomUUID()
                )
            }.hasMessageContaining("Forrige vedtak er allerede opphørt")
        }

        @Test
        internal fun `skal opphøre før ett tidligere opphør, skal sette nytt opphørsdato`() {
            val tidligereOpphør = YearMonth.of(2022, 1)
            val opphørFom = YearMonth.of(2021, 1)
            val andelFom = YearMonth.of(2022, 6).atDay(1)
            val andelTom = YearMonth.of(2022, 6).atEndOfMonth()

            every { tilkjentYtelseService.hentForBehandling(any()) } returns
                lagTilkjentYtelse(
                    listOf(lagAndelTilkjentYtelse(100, andelFom, andelTom)),
                    startmåned = tidligereOpphør
                )

            utførSteg(
                BehandlingType.REVURDERING,
                Opphør(opphørFom = opphørFom, begrunnelse = "null"),
                forrigeBehandlingId = UUID.randomUUID()
            )
            assertThat(slot.captured.startmåned).isEqualTo(opphørFom)
            assertThat(slot.captured.andelerTilkjentYtelse).isEmpty()
        }

        @Test
        internal fun `skal opphøre etter datoet for ett tidligere opphør, men før tidligere andeler - beholder tidligere opphørsdato`() {
            val tidligereOpphør = YearMonth.of(2021, 1)
            val opphørFom = YearMonth.of(2022, 1)
            val andelFom = YearMonth.of(2022, 6).atDay(1)
            val andelTom = YearMonth.of(2022, 6).atEndOfMonth()

            every { tilkjentYtelseService.hentForBehandling(any()) } returns
                lagTilkjentYtelse(
                    listOf(lagAndelTilkjentYtelse(100, andelFom, andelTom)),
                    startmåned = tidligereOpphør
                )

            utførSteg(
                BehandlingType.REVURDERING,
                Opphør(opphørFom = opphørFom, begrunnelse = "null"),
                forrigeBehandlingId = UUID.randomUUID()
            )
            assertThat(slot.captured.startmåned).isEqualTo(tidligereOpphør)
            assertThat(slot.captured.andelerTilkjentYtelse).isEmpty()
        }

        @Test
        internal fun `skal opphøre etter datoet for ett tidligere opphør, men etter tidligere andeler - beholder tidligere opphørsdato`() {
            val tidligereOpphør = YearMonth.of(2021, 1)
            val opphørFom = YearMonth.of(2022, 8)
            val andelFom = YearMonth.of(2022, 6).atDay(1)
            val andelTom = YearMonth.of(2022, 10).atEndOfMonth()

            every { tilkjentYtelseService.hentForBehandling(any()) } returns
                lagTilkjentYtelse(
                    listOf(lagAndelTilkjentYtelse(100, andelFom, andelTom)),
                    startmåned = tidligereOpphør
                )

            utførSteg(
                BehandlingType.REVURDERING,
                Opphør(opphørFom = opphørFom, begrunnelse = "null"),
                forrigeBehandlingId = UUID.randomUUID()
            )
            assertThat(slot.captured.startmåned).isEqualTo(tidligereOpphør)
            assertThat(slot.captured.andelerTilkjentYtelse).hasSize(1)
            assertThat(slot.captured.andelerTilkjentYtelse[0].periode.fom).isEqualTo(andelFom)
            assertThat(slot.captured.andelerTilkjentYtelse[0].periode.tomMåned).isEqualTo(opphørFom.minusMonths(1))
        }
    }

    @Nested
    inner class Sanksjon {

        @Test
        internal fun `skal splitte tidligere periode og beholde startdato når man utfør sanksjon`() {
            val startMåned = YearMonth.of(2021, 6)
            val andelFom = startMåned.atDay(1)
            val andelTom = YearMonth.of(2021, 8).atEndOfMonth()

            every { tilkjentYtelseService.hentForBehandling(any()) } returns
                lagTilkjentYtelse(
                    listOf(lagAndelTilkjentYtelse(100, andelFom, andelTom)),
                    startmåned = YearMonth.from(andelFom)
                )
            every { beregningService.beregnYtelse(any(), any()) } answers {
                firstArg<List<Månedsperiode>>().map { lagBeløpsperiode(it.fom, it.tom) }
            }

            utførSteg(
                BehandlingType.REVURDERING,
                sanksjon(startMåned.plusMonths(1)),
                forrigeBehandlingId = UUID.randomUUID()
            )

            assertThat(slot.captured.startmåned).isEqualTo(YearMonth.from(andelFom))
            assertThat(slot.captured.andelerTilkjentYtelse).hasSize(2)
            assertThat(slot.captured.andelerTilkjentYtelse[0].periode.fom).isEqualTo(andelFom)
            assertThat(slot.captured.andelerTilkjentYtelse[0].periode.tomMåned).isEqualTo(startMåned)
            assertThat(slot.captured.andelerTilkjentYtelse[1].periode.fom).isEqualTo(andelFom.plusMonths(2))
            assertThat(slot.captured.andelerTilkjentYtelse[1].periode.tom).isEqualTo(andelTom)
        }
    }

    @Nested
    inner class Sanksjonsrevurdering {

        @Test
        internal fun `skal ikke kunne opphøre før forrige sanksjonsbehandling`() {
            every { featureToggleService.isEnabled(any()) } returns false
            val startMåned = YearMonth.of(2021, 6)
            val sluttMåned = YearMonth.of(2021, 12)
            val opphørFom = YearMonth.of(2021, 6)
            val sankskjonsMåned = YearMonth.of(2021, 8)

            every {
                andelsHistorikkService.hentHistorikk(any(), any())
            } returns listOf(
                andelhistorikkInnvilget(startMåned, sankskjonsMåned.minusMonths(1)),
                andelhistorikkSanksjon(sankskjonsMåned),
                andelhistorikkInnvilget(sankskjonsMåned.plusMonths(1), sluttMåned)
            )

            assertThrows<Feil> {
                utførSteg(
                    BehandlingType.REVURDERING,
                    Opphør(opphørFom, "ok"),
                    forrigeBehandlingId = UUID.randomUUID()
                )
            }
            verify { andelsHistorikkService.hentHistorikk(any(), any()) }
        }

        @Test
        internal fun `skal ikke kunne innvilge med periode før forrige sanksjonsbehandling`() {
            every { featureToggleService.isEnabled(any()) } returns false
            val startMåned = YearMonth.of(2021, 6)
            val sluttMåned = YearMonth.of(2021, 12)
            val sankskjonsMåned = YearMonth.of(2021, 8)

            every {
                andelsHistorikkService.hentHistorikk(any(), any())
            } returns listOf(
                andelhistorikkInnvilget(startMåned, sankskjonsMåned.minusMonths(1)),
                andelhistorikkSanksjon(sankskjonsMåned),
                andelhistorikkInnvilget(sankskjonsMåned.plusMonths(1), sluttMåned)
            )

            assertThrows<Feil> {
                utførSteg(
                    BehandlingType.REVURDERING,
                    innvilget(listOf(innvilgetPeriode(startMåned, sluttMåned)), listOf(inntekt(startMåned))),
                    forrigeBehandlingId = UUID.randomUUID()
                )
            }
            verify { andelsHistorikkService.hentHistorikk(any(), any()) }
        }

        @Test
        internal fun `tidligere sanksjon er fjernet, skal få revurdere som vanlig`() {
            every { featureToggleService.isEnabled(any()) } returns false
            val startMåned = YearMonth.of(2021, 6)
            val sluttMåned = YearMonth.of(2021, 12)
            val sankskjonsMåned = YearMonth.of(2021, 8)

            every {
                andelsHistorikkService.hentHistorikk(any(), any())
            } returns listOf(
                andelhistorikkInnvilget(startMåned, sankskjonsMåned.minusMonths(1)),
                andelhistorikkSanksjon(sankskjonsMåned, fjernetHistorikkEndring),
                andelhistorikkInnvilget(sankskjonsMåned.plusMonths(1), sluttMåned)
            )
            every { tilkjentYtelseService.hentForBehandling(any()) } returns lagTilkjentYtelse(
                listOf(
                    lagAndelTilkjentYtelse(100, startMåned.plusMonths(1).atDay(1), sluttMåned.atEndOfMonth())
                )
            )
            every { beregningService.beregnYtelse(any(), any()) } answers {
                firstArg<List<Månedsperiode>>().map { lagBeløpsperiode(it.fom, it.tom) }
            }

            utførSteg(
                BehandlingType.REVURDERING,
                innvilget(listOf(innvilgetPeriode(startMåned, sluttMåned)), listOf(inntekt(startMåned))),
                forrigeBehandlingId = UUID.randomUUID()
            )

            assertThat(slot.captured.startmåned).isEqualTo(startMåned)
            verify { tilkjentYtelseService.opprettTilkjentYtelse(any()) }
            verify { andelsHistorikkService.hentHistorikk(any(), any()) }
        }
    }

    @Nested
    inner class Barnetilsyn {

        @BeforeEach
        internal fun setUp() {
            every { beregningBarnetilsynService.beregnYtelseBarnetilsyn(any(), any(), any()) } returns
                listOf(
                    BeløpsperiodeBarnetilsynDto(
                        periode = Månedsperiode(YearMonth.now()),
                        beløp = 1,
                        beløpFørFratrekkOgSatsjustering = 1,
                        sats = 6284,
                        beregningsgrunnlag = grunnlag()
                    )
                )
        }

        @Test
        internal fun `innvilger barnetilsyn skal validere at barn finnes`() {
            utførSteg(
                lagSaksbehandling(stønadType = StønadType.BARNETILSYN),
                innvilgetBarnetilsyn(YearMonth.of(2021, 1), YearMonth.of(2021, 1), barn = listOf(UUID.randomUUID()))
            )

            verify(exactly = 1) { barnService.validerBarnFinnesPåBehandling(any(), any()) }
        }

        @Test
        internal fun `revurdering - nye andeler legges til etter forrige andeler`() {
            val forrigeAndelFom = LocalDate.of(2021, 1, 1)
            val forrigeAndelTom = LocalDate.of(2021, 3, 31)
            val nyAndelFom = YearMonth.of(2022, 1)
            val nyAndelTom = YearMonth.of(2022, 1)

            every { tilkjentYtelseService.hentForBehandling(any()) } returns
                lagTilkjentYtelse(listOf(lagAndelTilkjentYtelse(100, forrigeAndelFom, forrigeAndelTom)))
            every { beregningBarnetilsynService.beregnYtelseBarnetilsyn(any(), any(), any()) } returns
                listOf(
                    BeløpsperiodeBarnetilsynDto(
                        periode = Månedsperiode(nyAndelFom, nyAndelTom),
                        beløp = 1,
                        beløpFørFratrekkOgSatsjustering = 1,
                        sats = 6284,
                        beregningsgrunnlag = grunnlag()
                    )
                )

            utførSteg(
                saksbehandling(
                    fagsak = fagsak(stønadstype = StønadType.BARNETILSYN),
                    type = BehandlingType.REVURDERING,
                    forrigeBehandlingId = UUID.randomUUID()
                ),
                innvilgetBarnetilsyn(nyAndelFom, nyAndelTom, barn = listOf(UUID.randomUUID()))
            )

            val andeler = slot.captured.andelerTilkjentYtelse
            assertThat(andeler).hasSize(2)
            assertThat(andeler[0].periode.fom).isEqualTo(forrigeAndelFom)
            assertThat(andeler[0].periode.tom).isEqualTo(forrigeAndelTom)

            assertThat(andeler[1].periode.fomMåned).isEqualTo(nyAndelFom)
            assertThat(andeler[1].periode.tomMåned).isEqualTo(nyAndelTom)

            assertThat(andeler[0].kildeBehandlingId).isNotEqualTo(andeler[1].kildeBehandlingId)
            verify(exactly = 1) {
                simuleringService.hentOgLagreSimuleringsresultat(any())
            }
        }

        @Test
        internal fun `revurdering - førstegangsbehandling er avslått - kun nye andeler som skal gjelde`() {
            val nyAndelFom = YearMonth.of(2022, 1)
            val nyAndelTom = YearMonth.of(2022, 1)

            every { tilkjentYtelseService.hentForBehandling(any()) } throws IllegalArgumentException("Hjelp")
            every { beregningBarnetilsynService.beregnYtelseBarnetilsyn(any(), any(), any()) } returns
                listOf(
                    BeløpsperiodeBarnetilsynDto(
                        periode = Månedsperiode(nyAndelFom, nyAndelTom),
                        beløp = 1,
                        beløpFørFratrekkOgSatsjustering = 1,
                        sats = 6284,
                        beregningsgrunnlag = grunnlag()
                    )
                )

            utførSteg(
                saksbehandling(
                    fagsak = fagsak(stønadstype = StønadType.BARNETILSYN),
                    type = BehandlingType.REVURDERING,
                    forrigeBehandlingId = null
                ),
                innvilgetBarnetilsyn(nyAndelFom, nyAndelTom, barn = listOf(UUID.randomUUID()))
            )

            val andeler = slot.captured.andelerTilkjentYtelse
            assertThat(andeler).hasSize(1)
            assertThat(andeler[0].periode.fomMåned).isEqualTo(nyAndelFom)
            assertThat(andeler[0].periode.tomMåned).isEqualTo(nyAndelTom)

            verify(exactly = 1) {
                simuleringService.hentOgLagreSimuleringsresultat(any())
            }
        }

        @Test
        internal fun `dersom kontantstøttebeløp er større enn utgiftsbeløp skal det kastes feil dersom resultatypen er innvilget`() {
            val nyAndelFom = YearMonth.of(2022, 1)
            val nyAndelTom = YearMonth.of(2022, 1)

            every { tilkjentYtelseService.hentForBehandling(any()) } throws IllegalArgumentException("Hjelp")
            every { beregningBarnetilsynService.beregnYtelseBarnetilsyn(any(), any(), any()) } returns
                listOf(
                    BeløpsperiodeBarnetilsynDto(
                        periode = Månedsperiode(nyAndelFom, nyAndelTom),
                        beløp = 0,
                        beløpFørFratrekkOgSatsjustering = 0,
                        sats = 6284,
                        beregningsgrunnlag = BeregningsgrunnlagBarnetilsynDto(
                            utgifter = BigDecimal.TEN,
                            kontantstøttebeløp = BigDecimal.TEN,
                            tilleggsstønadsbeløp = BigDecimal.ZERO,
                            1,
                            emptyList()
                        )
                    )
                )

            assertThrows<ApiFeil> {
                utførSteg(
                    saksbehandling(
                        fagsak = fagsak(stønadstype = StønadType.BARNETILSYN),
                        type = BehandlingType.FØRSTEGANGSBEHANDLING,
                        forrigeBehandlingId = null
                    ),
                    innvilgetBarnetilsyn(nyAndelFom, nyAndelTom).copy(resultatType = ResultatType.INNVILGE)
                )
            }
        }
    }

    @Test
    internal fun `skal ikke kunne lagre andel som er midlertidig opphør dersom det finnes et barn på andelen`() {
        val barn = listOf<UUID>(UUID.randomUUID())
        val andelFom = YearMonth.of(2022, 1)
        val andelTom = YearMonth.of(2022, 1)

        every { barnService.finnBarnPåBehandling(any()) } returns identerTilBehandlingBarn(barn)

        val feil: ApiFeil = assertThrows {
            utførSteg(
                saksbehandling(
                    fagsak = fagsak(stønadstype = StønadType.BARNETILSYN),
                    type = BehandlingType.FØRSTEGANGSBEHANDLING,
                    forrigeBehandlingId = null
                ),
                innvilgetBarnetilsyn(andelFom, andelTom, barn, utgifter = 0, erMidlertidigOpphør = true)
            )
        }
        assertThat(feil.feil).contains("Kan ikke ta med barn på en periode som er et midlertidig opphør, på behandling=")
    }

    @Test
    internal fun `skal ikke kunne lagre andel som er midlertidig opphør det finnes en utgift større enn null på andelen`() {
        val andelFom = YearMonth.of(2022, 1)
        val andelTom = YearMonth.of(2022, 1)

        every { barnService.finnBarnPåBehandling(any()) } returns identerTilBehandlingBarn(emptyList())

        val feil: ApiFeil = assertThrows {
            utførSteg(
                saksbehandling(
                    fagsak = fagsak(stønadstype = StønadType.BARNETILSYN),
                    type = BehandlingType.FØRSTEGANGSBEHANDLING,
                    forrigeBehandlingId = null
                ),
                innvilgetBarnetilsyn(andelFom, andelTom, utgifter = 2500, erMidlertidigOpphør = true)
            )
        }
        assertThat(feil.feil).contains("kan ikke ha utgifter større enn null på en periode som er et midlertidig opphør, på behandling=")
    }

    @Test
    internal fun `skal ikke kunne lagre andel som er midlertidig opphør som første andel i en førstegangsbehandling`() {
        val andelFom = YearMonth.of(2022, 1)
        val andelTom = YearMonth.of(2022, 1)

        every { barnService.finnBarnPåBehandling(any()) } returns identerTilBehandlingBarn(emptyList())

        val feil: ApiFeil = assertThrows {
            utførSteg(
                saksbehandling(
                    fagsak = fagsak(stønadstype = StønadType.BARNETILSYN),
                    type = BehandlingType.FØRSTEGANGSBEHANDLING,
                    forrigeBehandlingId = null
                ),
                innvilgetBarnetilsyn(andelFom, andelTom, utgifter = 0, erMidlertidigOpphør = true)
            )
        }
        assertThat(feil.feil).contains("Første periode kan ikke ha et nullbeløp, på førstegangsbehandling=")
    }

    @Test
    internal fun `skal ikke kunne lagre andel som er midlertidig opphør dersom det ikke har blitt innvilget beløp på tidligere vedtak`() {
        val andelFom = YearMonth.of(2022, 1)
        val andelTom = YearMonth.of(2022, 1)

        every { barnService.finnBarnPåBehandling(any()) } returns identerTilBehandlingBarn(emptyList())
        every { tilkjentYtelseService.hentForBehandling(any()) } returns tilkjentYtelse(
            behandlingId = UUID.randomUUID(),
            personIdent = "",
            startmåned = YearMonth.of(2021, 12),
            beløp = 0
        )

        val feil: ApiFeil = assertThrows {
            utførSteg(
                saksbehandling(
                    fagsak = fagsak(stønadstype = StønadType.BARNETILSYN),
                    type = BehandlingType.REVURDERING,
                    forrigeBehandlingId = UUID.randomUUID()
                ),
                innvilgetBarnetilsyn(andelFom, andelTom, utgifter = 0, erMidlertidigOpphør = true)
            )
        }
        assertThat(feil.feil).contains("Første periode kan ikke ha et nullbeløp dersom det ikke har blitt innvilget beløp på et tidligere vedtak, på behandling=")
    }

    @Test
    internal fun `skal ikke kunne lagre andel som er midlertidig opphør dersom andelen ikke henger sammen med neste andelsperiode`() {
        val utgiftFom1 = YearMonth.of(2022, 1)
        val utgiftTom1 = YearMonth.of(2022, 1)
        val andelMidlertidigOpphørFom = YearMonth.of(2022, 3)
        val andelMidlertidigOpphørTom = YearMonth.of(2022, 3)
        val utgiftFom2 = YearMonth.of(2022, 4)
        val utgiftTom2 = YearMonth.of(2022, 4)

        val data = listOf(
            DatoBarnOgUtgifter(utgiftFom1, utgiftTom1, listOf(UUID.randomUUID()), utgifter = 2500),
            DatoBarnOgUtgifter(andelMidlertidigOpphørFom, andelMidlertidigOpphørTom, listOf(), utgifter = 0),
            DatoBarnOgUtgifter(utgiftFom2, utgiftTom2, listOf(UUID.randomUUID()), utgifter = 2500)
        )

        every { barnService.finnBarnPåBehandling(any()) } returns identerTilBehandlingBarn(emptyList())

        val feil: ApiFeil = assertThrows {
            utførSteg(
                saksbehandling(
                    fagsak = fagsak(stønadstype = StønadType.BARNETILSYN),
                    type = BehandlingType.FØRSTEGANGSBEHANDLING,
                    forrigeBehandlingId = null
                ),
                innvilgetBarnetilsynMedFlerePerioder(data)
            )
        }
        assertThat(feil.feil).contains("Periodene må være sammenhengende")
    }

    private fun identerTilBehandlingBarn(identer: List<UUID>) = identer.map { BehandlingBarn(it, UUID.randomUUID()) }

    private fun innvilget(
        perioder: List<VedtaksperiodeDto>,
        inntekter: List<Inntekt>
    ) =
        InnvilgelseOvergangsstønad(
            perioder = perioder,
            inntekter = inntekter,
            inntektBegrunnelse = "null",
            periodeBegrunnelse = "null"
        )

    private fun innvilgetBarnetilsyn(
        startmåned: YearMonth,
        sluttmåned: YearMonth,
        barn: List<UUID>? = null,
        utgifter: Int? = null,
        erMidlertidigOpphør: Boolean? = null
    ) =
        InnvilgelseBarnetilsyn(
            perioder = listOf(
                UtgiftsperiodeDto(
                    årMånedFra = startmåned,
                    årMånedTil = sluttmåned,
                    periode = Månedsperiode(startmåned, sluttmåned),
                    barn = barn ?: emptyList(),
                    utgifter = utgifter ?: 2500,
                    erMidlertidigOpphør = erMidlertidigOpphør ?: false
                )
            ),
            perioderKontantstøtte = emptyList(),
            tilleggsstønad = TilleggsstønadDto(true, emptyList(), null),
            begrunnelse = null
        )

    data class DatoBarnOgUtgifter(val andelFom: YearMonth, val andelTom: YearMonth, val barn: List<UUID>, val utgifter: Int)

    private fun innvilgetBarnetilsynMedFlerePerioder(data: List<DatoBarnOgUtgifter>) = InnvilgelseBarnetilsyn(
        perioder = data.map {
            UtgiftsperiodeDto(
                årMånedFra = YearMonth.from(it.andelFom),
                årMånedTil = YearMonth.from(it.andelTom),
                periode = Månedsperiode(YearMonth.from(it.andelFom), YearMonth.from(it.andelTom)),
                barn = if (it.utgifter > 0) it.barn else emptyList(),
                utgifter = it.utgifter,
                erMidlertidigOpphør = if (it.utgifter > 0) false else true
            )
        },
        perioderKontantstøtte = emptyList(),
        tilleggsstønad = TilleggsstønadDto(
            true,
            emptyList(),
            null
        ),
        begrunnelse = null
    )

    private fun sanksjon(årMåned: YearMonth) =
        Sanksjonert(
            sanksjonsårsak = Sanksjonsårsak.SAGT_OPP_STILLING,
            periode = SanksjonertPeriodeDto(
                årMånedFra = årMåned,
                årMånedTil = årMåned,
                fom = årMåned,
                tom = årMåned
            ),
            internBegrunnelse = ""
        )

    private fun andelhistorikkInnvilget(fom: YearMonth, tom: YearMonth) =
        AndelHistorikkDto(
            behandlingId = UUID.randomUUID(),
            behandlingType = BehandlingType.REVURDERING,
            behandlingÅrsak = BehandlingÅrsak.NYE_OPPLYSNINGER,
            vedtakstidspunkt = LocalDateTime.now(),
            saksbehandler = "",
            andel = andelDto(1, fom, tom),
            aktivitet = AktivitetType.IKKE_AKTIVITETSPLIKT,
            periodeType = VedtaksperiodeType.HOVEDPERIODE,
            endring = null,
            aktivitetArbeid = null,
            erSanksjon = false,
            sanksjonsårsak = null
        )

    private val fjernetHistorikkEndring = HistorikkEndring(EndringType.FJERNET, UUID.randomUUID(), LocalDateTime.now())

    private fun andelhistorikkSanksjon(sanksjonMåned: YearMonth, endring: HistorikkEndring? = null) =
        AndelHistorikkDto(
            behandlingId = UUID.randomUUID(),
            behandlingType = BehandlingType.REVURDERING,
            behandlingÅrsak = BehandlingÅrsak.SANKSJON_1_MND,
            vedtakstidspunkt = LocalDateTime.now(),
            saksbehandler = "",
            andel = andelDto(0, sanksjonMåned, sanksjonMåned),
            aktivitet = AktivitetType.IKKE_AKTIVITETSPLIKT,
            periodeType = VedtaksperiodeType.SANKSJON,
            endring = endring,
            aktivitetArbeid = null,
            erSanksjon = true,
            sanksjonsårsak = Sanksjonsårsak.SAGT_OPP_STILLING
        )

    private fun andelDto(beløp: Int, fom: YearMonth, tom: YearMonth) =
        AndelMedGrunnlagDto(
            beløp = beløp,
            periode = Månedsperiode(fom, tom),
            inntekt = 0,
            inntektsreduksjon = 0,
            samordningsfradrag = 0,
            kontantstøtte = 0,
            tilleggsstønad = 0,
            antallBarn = 0,
            utgifter = BigDecimal.ZERO,
            barn = emptyList(),
            sats = 0,
            beløpFørFratrekkOgSatsJustering = 0
        )

    private fun lagBeløpsperiode(fom: YearMonth, tom: YearMonth) =
        Beløpsperiode(
            periode = Månedsperiode(fom, tom),
            beregningsgrunnlag = null,
            beløp = BigDecimal.ZERO,
            beløpFørSamordning = BigDecimal.ZERO
        )

    private fun opphørsperiode(opphørFom: YearMonth, opphørTom: YearMonth) =
        VedtaksperiodeDto(
            årMånedFra = opphørFom,
            årMånedTil = opphørTom,
            periode = Månedsperiode(opphørFom, opphørTom),
            aktivitet = AktivitetType.IKKE_AKTIVITETSPLIKT,
            periodeType = VedtaksperiodeType.MIDLERTIDIG_OPPHØR
        )

    private fun innvilgetPeriode(andelFom: YearMonth, andelTom: YearMonth) =
        VedtaksperiodeDto(
            årMånedFra = andelFom,
            årMånedTil = andelTom,
            periode = Månedsperiode(andelFom, andelTom),
            aktivitet = AktivitetType.FORLENGELSE_STØNAD_PÅVENTE_ARBEID,
            periodeType = VedtaksperiodeType.HOVEDPERIODE
        )

    private fun inntekt(andelTom: YearMonth) =
        Inntekt(
            andelTom,
            BigDecimal(100000),
            samordningsfradrag = BigDecimal.ZERO
        )

    private fun utførSteg(
        type: BehandlingType,
        vedtak: VedtakDto = InnvilgelseOvergangsstønad(
            periodeBegrunnelse = "",
            inntektBegrunnelse = ""
        ),
        forrigeBehandlingId: UUID? = null
    ) {
        utførSteg(saksbehandling(type = type, forrigeBehandlingId = forrigeBehandlingId), vedtak)
    }

    private fun utførSteg(
        saksbehandling: Saksbehandling = saksbehandling(),
        vedtak: VedtakDto = InnvilgelseOvergangsstønad(
            periodeBegrunnelse = "",
            inntektBegrunnelse = ""
        )
    ) {
        steg.utførSteg(saksbehandling = saksbehandling, data = vedtak)
    }

    private fun lagSaksbehandling(
        stønadType: StønadType = StønadType.OVERGANGSSTØNAD,
        type: BehandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
        forrigeBehandlingId: UUID? = null
    ): Saksbehandling {
        val fagsak = fagsak(stønadstype = stønadType)
        return saksbehandling(fagsak, behandling(fagsak, type = type, forrigeBehandlingId = forrigeBehandlingId))
    }

    private fun grunnlag() = BeregningsgrunnlagBarnetilsynDto(
        BigDecimal.ONE,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        0,
        emptyList()
    )
}
