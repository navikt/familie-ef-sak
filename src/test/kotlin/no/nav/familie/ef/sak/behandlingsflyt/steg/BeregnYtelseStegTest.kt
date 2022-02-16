package no.nav.familie.ef.sak.behandlingsflyt.steg

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.beregning.Beløpsperiode
import no.nav.familie.ef.sak.beregning.BeregningService
import no.nav.familie.ef.sak.beregning.Inntekt
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.felles.dto.Periode
import no.nav.familie.ef.sak.felles.util.mockFeatureToggleService
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.fagsakpersoner
import no.nav.familie.ef.sak.simulering.SimuleringService
import no.nav.familie.ef.sak.simulering.Simuleringsresultat
import no.nav.familie.ef.sak.tilbakekreving.TilbakekrevingService
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.ef.sak.tilkjentytelse.domain.TilkjentYtelse
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vedtak.domain.AktivitetType
import no.nav.familie.ef.sak.vedtak.domain.AvslagÅrsak
import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType
import no.nav.familie.ef.sak.vedtak.dto.Avslå
import no.nav.familie.ef.sak.vedtak.dto.Innvilget
import no.nav.familie.ef.sak.vedtak.dto.Opphør
import no.nav.familie.ef.sak.vedtak.dto.ResultatType
import no.nav.familie.ef.sak.vedtak.dto.VedtakDto
import no.nav.familie.ef.sak.vedtak.dto.VedtaksperiodeDto
import no.nav.familie.ef.sak.økonomi.lagAndelTilkjentYtelse
import no.nav.familie.ef.sak.økonomi.lagTilkjentYtelse
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
import java.time.YearMonth
import java.util.UUID

internal class BeregnYtelseStegTest {

    private val tilkjentYtelseService = mockk<TilkjentYtelseService>(relaxed = true)
    private val beregningService = mockk<BeregningService>()
    private val vedtakService = mockk<VedtakService>(relaxed = true)
    private val simuleringService = mockk<SimuleringService>()
    private val tilbakekrevingService = mockk<TilbakekrevingService>(relaxed = true)
    private val fagsakService = mockk<FagsakService>(relaxed = true)
    private val featureToggleService = mockFeatureToggleService()

    private val steg = BeregnYtelseSteg(tilkjentYtelseService,
                                        beregningService,
                                        simuleringService,
                                        vedtakService,
                                        tilbakekrevingService,
                                        fagsakService,
                                        featureToggleService)

    private val slot = slot<TilkjentYtelse>()

    @BeforeEach
    internal fun setUp() {
        every { fagsakService.fagsakMedOppdatertPersonIdent(any()) } returns fagsak(fagsakpersoner(setOf("123")))
        every { simuleringService.hentOgLagreSimuleringsresultat(any()) }
                .returns(Simuleringsresultat(behandlingId = UUID.randomUUID(),
                                             data = DetaljertSimuleringResultat(emptyList()),
                                             beriketData = BeriketSimuleringsresultat(mockk(),
                                                                                      mockk())))
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
            every { beregningService.beregnYtelse(any(), any()) } returns listOf(lagBeløpsperiode(LocalDate.now(),
                                                                                                  LocalDate.now()))
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

            utførSteg(BehandlingType.REVURDERING,
                      Opphør(opphørFom = opphørFom, begrunnelse = "null"),
                      forrigeBehandlingId = UUID.randomUUID())

            assertThat(slot.captured.andelerTilkjentYtelse).hasSize(1)
            assertThat(slot.captured.andelerTilkjentYtelse.first().stønadFom).isEqualTo(forventetNyAndelFom)
            assertThat(slot.captured.andelerTilkjentYtelse.first().stønadTom).isEqualTo(forventetNyAndelTom)
        }

        @Test
        internal fun `skal opphøre vedtak fra samme måned som forrige andel starter`() {
            val opphørFom = YearMonth.of(2021, 1)

            val forrigeAndelFom = LocalDate.of(2021, 1, 1)
            val forrigeAndelTom = LocalDate.of(2021, 12, 31)

            val slot = slot<TilkjentYtelse>()
            every { tilkjentYtelseService.opprettTilkjentYtelse(capture(slot)) } answers { firstArg() }
            every { tilkjentYtelseService.hentForBehandling(any()) } returns
                    lagTilkjentYtelse(listOf(lagAndelTilkjentYtelse(100, forrigeAndelFom, forrigeAndelTom)))

            utførSteg(BehandlingType.REVURDERING,
                      Opphør(opphørFom = opphørFom, begrunnelse = "null"),
                      forrigeBehandlingId = UUID.randomUUID())

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
                firstArg<List<Periode>>().map { lagBeløpsperiode(it.fradato, it.tildato) }
            }

            val opphørsperiode = opphørsperiode(opphørFom, opphørTom)
            val innvilgetPeriode1 = innvilgetPeriode(innvilgetFom1, innvilgetTom1)
            val innvilgetPeriode2 = innvilgetPeriode(innvilgetFom2, innvilgetTom2)

            utførSteg(BehandlingType.FØRSTEGANGSBEHANDLING,
                      innvilget(listOf(innvilgetPeriode1, opphørsperiode, innvilgetPeriode2),
                                listOf(inntekt(innvilgetFom1))),
                      forrigeBehandlingId = UUID.randomUUID())

            val andelerTilkjentYtelse = slot.captured.andelerTilkjentYtelse
            assertThat(andelerTilkjentYtelse.size).isEqualTo(2)
            assertThat(andelerTilkjentYtelse.firstOrNull()?.stønadFom).isEqualTo(innvilgetFom1.atDay(1))
            assertThat(andelerTilkjentYtelse.firstOrNull()?.stønadTom).isEqualTo(opphørFom.minusMonths(1)
                                                                                         .atEndOfMonth())
            assertThat(andelerTilkjentYtelse.lastOrNull()?.stønadFom).isEqualTo(innvilgetFom2.atDay(1))
            assertThat(andelerTilkjentYtelse.lastOrNull()?.stønadTom).isEqualTo(innvilgetTom2.atEndOfMonth())
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
                firstArg<List<Periode>>().map { lagBeløpsperiode(it.fradato, it.tildato) }
            }

            val opphørsperiode = opphørsperiode(opphørFom, opphørTom)
            val innvilgetPeriode = innvilgetPeriode(innvilgetFom, innvilgetTom)
            assertThrows<Feil> {
                utførSteg(BehandlingType.REVURDERING,
                          innvilget(listOf(opphørsperiode, innvilgetPeriode), listOf(inntekt(innvilgetFom))),
                          forrigeBehandlingId = UUID.randomUUID())
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
                firstArg<List<Periode>>().map { lagBeløpsperiode(it.fradato, it.tildato) }
            }

            val opphørsperiode = opphørsperiode(opphørFom, opphørTom)
            val innvilgetPeriode = innvilgetPeriode(innvilgetFom, innvilgetTom)
            utførSteg(BehandlingType.REVURDERING,
                      innvilget(listOf(opphørsperiode, innvilgetPeriode),
                                listOf(inntekt(innvilgetFom))),
                      forrigeBehandlingId = UUID.randomUUID())

            assertThat(slot.captured.andelerTilkjentYtelse).hasSize(2)
            assertThat(slot.captured.andelerTilkjentYtelse.first().stønadFom).isEqualTo(forventetNyAndelFom1)
            assertThat(slot.captured.andelerTilkjentYtelse.first().stønadTom).isEqualTo(forventetNyAndelTom1)
            assertThat(slot.captured.andelerTilkjentYtelse.last().stønadFom).isEqualTo(forventetNyAndelFom2)
            assertThat(slot.captured.andelerTilkjentYtelse.last().stønadTom).isEqualTo(forventetNyAndelTom2)
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
                firstArg<List<Periode>>().map { lagBeløpsperiode(it.fradato, it.tildato) }
            }

            val innvilgetPeriode1 = innvilgetPeriode(innvilgetFom1, innvilgetTom1)
            val opphørsperiode = opphørsperiode(opphørFom, opphørTom)
            val innvilgetPeriode2 = innvilgetPeriode(innvilgetFom2, innvilgetTom2)

            utførSteg(BehandlingType.REVURDERING,
                      innvilget(listOf(innvilgetPeriode1, opphørsperiode, innvilgetPeriode2),
                                listOf(inntekt(innvilgetFom1))),
                      forrigeBehandlingId = UUID.randomUUID())

            assertThat(slot.captured.andelerTilkjentYtelse).hasSize(3)
            assertThat(slot.captured.andelerTilkjentYtelse[0].stønadFom).isEqualTo(forventetNyAndelFom1)
            assertThat(slot.captured.andelerTilkjentYtelse[0].stønadTom).isEqualTo(forventetNyAndelTom1)
            assertThat(slot.captured.andelerTilkjentYtelse[1].stønadFom).isEqualTo(forventetNyAndelFom2)
            assertThat(slot.captured.andelerTilkjentYtelse[1].stønadTom).isEqualTo(forventetNyAndelTom2)
            assertThat(slot.captured.andelerTilkjentYtelse[2].stønadFom).isEqualTo(forventetNyAndelFom3)
            assertThat(slot.captured.andelerTilkjentYtelse[2].stønadTom).isEqualTo(forventetNyAndelTom3)
        }

        @Test
        internal fun `skal få kun første periode hvis opphørsdato er starten på neste periode som er delt i to`() {
            val opphørFom = YearMonth.of(2021, 9)

            val andel1Fom = LocalDate.of(2021, 1, 1)
            val andel1Tom = LocalDate.of(2021, 6, 30)
            val andel2Fom = LocalDate.of(2021, 9, 1)
            val andel2Tom = LocalDate.of(2021, 12, 31)

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
        internal fun `skal ikke feile hvis opphørsdato er mellom to perioder`() {
            val opphørFom = YearMonth.of(2021, 8)

            val andel1Fom = LocalDate.of(2021, 1, 1)
            val andel1Tom = LocalDate.of(2021, 6, 30)
            val andel2Fom = LocalDate.of(2021, 9, 1)
            val andel2Tom = LocalDate.of(2021, 12, 31)

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
        internal fun `skal opphøre hvis opphørsdato samsvarer med startdato for andel`() {
            val opphørFom = YearMonth.of(2021, 7)

            val andel1Fom = LocalDate.of(2021, 1, 1)
            val andel1Tom = LocalDate.of(2021, 6, 30)
            val andel2Fom = LocalDate.of(2021, 7, 1)
            val andel2Tom = LocalDate.of(2021, 12, 31)

            val forventetNyAndelFom = LocalDate.of(2021, 1, 1)
            val forventetNyAndelTom = LocalDate.of(2021, 6, 30)

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
                          Opphør(opphørFom = YearMonth.of(2021, 6), begrunnelse = "null"),
                          forrigeBehandlingId = UUID.randomUUID())
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
            assertThat(nyeAndeler[0].stønadFom).isEqualTo(nyAndelFom)
            assertThat(nyeAndeler[0].stønadTom).isEqualTo(nyAndelTom)
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
            assertThat(nyeAndeler[0].stønadFom).isEqualTo(forrigeAndelFom)
            assertThat(nyeAndeler[0].stønadTom).isEqualTo(forrigeAndelTom)
            assertThat(nyeAndeler[0].beløp).isEqualTo(50)

            assertThat(nyeAndeler[1].stønadFom).isEqualTo(nyAndelFom)
            assertThat(nyeAndeler[1].stønadTom).isEqualTo(nyAndelTom)
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
            assertThat(nyeAndeler[0].stønadFom).isEqualTo(forrigeAndelFom)
            assertThat(nyeAndeler[0].stønadTom).isEqualTo(LocalDate.of(2021, 10, 31))
            assertThat(nyeAndeler[0].beløp).isEqualTo(50)

            assertThat(nyeAndeler[1].stønadFom).isEqualTo(nyAndelFom)
            assertThat(nyeAndeler[1].stønadTom).isEqualTo(nyAndelTom)
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

            val forrigeAndeler = listOf(lagAndelTilkjentYtelse(50, forrigeAndelFom, forrigeAndelTom),
                                        lagAndelTilkjentYtelse(70, forrigeAndelFom2, forrigeAndelTom2))
            val beløpsperioder = listOf(lagAndelTilkjentYtelse(100, nyAndelFom, nyAndelTom))
            val nyeAndeler = steg.slåSammenAndelerSomSkalVidereføres(beløpsperioder, lagTilkjentYtelse(forrigeAndeler), listOf())

            assertThat(nyeAndeler).hasSize(2)
            assertThat(nyeAndeler[0].stønadFom).isEqualTo(forrigeAndelFom)
            assertThat(nyeAndeler[0].stønadTom).isEqualTo(LocalDate.of(2021, 10, 31))
            assertThat(nyeAndeler[0].beløp).isEqualTo(50)

            assertThat(nyeAndeler[1].stønadFom).isEqualTo(nyAndelFom)
            assertThat(nyeAndeler[1].stønadTom).isEqualTo(nyAndelTom)
            assertThat(nyeAndeler[1].beløp).isEqualTo(100)
        }

        @Test
        internal fun `legger på opphold og ny periode`() {
            val forrigeAndelFom = LocalDate.of(2021, 1, 1)
            val forrigeAndelTom = LocalDate.of(2021, 10, 31)
            val nyAndelFom = LocalDate.of(2021, 12, 1)
            val nyAndelTom = LocalDate.of(2021, 12, 31)
            val forrigeAndeler = listOf(lagAndelTilkjentYtelse(50, forrigeAndelFom, forrigeAndelTom))
            val opphørsperioder = listOf(Periode(LocalDate.of(2021, 11, 1), LocalDate.of(2021, 11, 30)))
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
            val opphør1 = Periode(LocalDate.of(2021, 12, 1), LocalDate.of(2021, 12, 31))
            val opphør2 = Periode(LocalDate.of(2022, 2, 1), LocalDate.of(2022, 3, 31))
            val opphør3 = Periode(LocalDate.of(2022, 6, 1), LocalDate.of(2022, 8, 31))
            val opphørsperioder = listOf(opphør1, opphør2, opphør3)
            val forrigeAndeler = listOf(lagAndelTilkjentYtelse(50, forrigeAndelFom, forrigeAndelTom))
            val beløpsperioder = listOf(lagAndelTilkjentYtelse(100, nyAndelFom, nyAndelTom))

            val nyeAndeler =
                    steg.slåSammenAndelerSomSkalVidereføres(beløpsperioder, lagTilkjentYtelse(forrigeAndeler), opphørsperioder)

            assertThat(nyeAndeler).hasSize(5)
            assertThat(nyeAndeler[0].stønadFom).isEqualTo(forrigeAndelFom)
            assertThat(nyeAndeler[0].stønadTom).isEqualTo(forrigeAndelTom)
            assertThat(nyeAndeler[0].beløp).isEqualTo(50)
            assertThat(nyeAndeler[0].kildeBehandlingId).isEqualTo(forrigeAndeler[0].kildeBehandlingId)
            assertThat(nyeAndeler[1].stønadFom).isEqualTo(nyAndelFom)
            assertThat(nyeAndeler[1].stønadTom).isEqualTo(opphør1.fradato.minusDays(1))
            assertThat(nyeAndeler[1].beløp).isEqualTo(100)
            assertThat(nyeAndeler[1].kildeBehandlingId).isEqualTo(beløpsperioder[0].kildeBehandlingId)
            assertThat(nyeAndeler[2].stønadFom).isEqualTo(opphør1.tildato.plusDays(1))
            assertThat(nyeAndeler[2].stønadTom).isEqualTo(opphør2.fradato.minusDays(1))
            assertThat(nyeAndeler[2].beløp).isEqualTo(100)
            assertThat(nyeAndeler[2].kildeBehandlingId).isEqualTo(beløpsperioder[0].kildeBehandlingId)
            assertThat(nyeAndeler[3].stønadFom).isEqualTo(opphør2.tildato.plusDays(1))
            assertThat(nyeAndeler[3].stønadTom).isEqualTo(opphør3.fradato.minusDays(1))
            assertThat(nyeAndeler[3].beløp).isEqualTo(100)
            assertThat(nyeAndeler[3].kildeBehandlingId).isEqualTo(beløpsperioder[0].kildeBehandlingId)
            assertThat(nyeAndeler[4].stønadFom).isEqualTo(opphør3.tildato.plusDays(1))
            assertThat(nyeAndeler[4].stønadTom).isEqualTo(nyAndelTom)
            assertThat(nyeAndeler[4].beløp).isEqualTo(100)
            assertThat(nyeAndeler[4].kildeBehandlingId).isEqualTo(beløpsperioder[0].kildeBehandlingId)
            assertThat(nyeAndeler[0].kildeBehandlingId).isNotEqualTo(nyeAndeler[1].kildeBehandlingId)
        }

        @Test
        internal fun `lager opphold i eksisterende andel, ny beløpsperiode med nytt opphold`() {
            val forrigeAndelFom = LocalDate.of(2021, 1, 1)
            val forrigeAndelTom = LocalDate.of(2025, 10, 31)
            val opphør1 = Periode(LocalDate.of(2021, 12, 1), LocalDate.of(2021, 12, 31))
            val nyAndelFom1 = LocalDate.of(2022, 1, 1)
            val nyAndelTom1 = LocalDate.of(2022, 3, 31)
            val opphør2 = Periode(LocalDate.of(2022, 4, 1), LocalDate.of(2022, 8, 31))
            val nyAndelFom2 = LocalDate.of(2022, 9, 1)
            val nyAndelTom2 = LocalDate.of(2022, 9, 30)
            val opphørsperioder = listOf(opphør1, opphør2)
            val forrigeAndeler = listOf(lagAndelTilkjentYtelse(50, forrigeAndelFom, forrigeAndelTom))
            val beløpsperioder = listOf(lagAndelTilkjentYtelse(100, nyAndelFom1, nyAndelTom1),
                                        lagAndelTilkjentYtelse(150, nyAndelFom2, nyAndelTom2))

            val nyeAndeler =
                    steg.slåSammenAndelerSomSkalVidereføres(beløpsperioder, lagTilkjentYtelse(forrigeAndeler), opphørsperioder)

            assertThat(nyeAndeler).hasSize(3)
            assertThat(nyeAndeler[0].stønadFom).isEqualTo(forrigeAndelFom)
            assertThat(nyeAndeler[0].stønadTom).isEqualTo(opphør1.fradato.minusDays(1))
            assertThat(nyeAndeler[0].beløp).isEqualTo(50)
            assertThat(nyeAndeler[0].kildeBehandlingId).isEqualTo(forrigeAndeler[0].kildeBehandlingId)
            assertThat(nyeAndeler[1].stønadFom).isEqualTo(nyAndelFom1)
            assertThat(nyeAndeler[1].stønadTom).isEqualTo(nyAndelTom1)
            assertThat(nyeAndeler[1].beløp).isEqualTo(100)
            assertThat(nyeAndeler[1].kildeBehandlingId).isNotEqualTo(forrigeAndeler[0].kildeBehandlingId)
            assertThat(nyeAndeler[2].stønadFom).isEqualTo(nyAndelFom2)
            assertThat(nyeAndeler[2].stønadTom).isEqualTo(nyAndelTom2)
            assertThat(nyeAndeler[2].beløp).isEqualTo(150)
            assertThat(nyeAndeler[2].kildeBehandlingId).isNotEqualTo(forrigeAndeler[0].kildeBehandlingId)

        }

        @Test
        fun `lager opphold på slutten av forrige andel uten feil`() {
            val forrigeAndelFom = LocalDate.of(2021, 1, 1)
            val forrigeAndelTom = LocalDate.of(2021, 10, 31)
            val nyAndelFom = LocalDate.of(2021, 10, 1)
            val nyAndelTom = LocalDate.of(2022, 12, 31)
            val opphør1 = Periode(LocalDate.of(2021, 9, 1), LocalDate.of(2021, 9, 30))
            val opphørsperioder = listOf(opphør1)
            val forrigeAndeler = listOf(lagAndelTilkjentYtelse(50, forrigeAndelFom, forrigeAndelTom))
            val beløpsperioder = listOf(lagAndelTilkjentYtelse(100, nyAndelFom, nyAndelTom))

            val nyeAndeler =
                    steg.slåSammenAndelerSomSkalVidereføres(beløpsperioder, lagTilkjentYtelse(forrigeAndeler), opphørsperioder)

            assertThat(nyeAndeler).hasSize(2)
            assertThat(nyeAndeler[0].stønadFom).isEqualTo(forrigeAndelFom)
            assertThat(nyeAndeler[0].stønadTom).isEqualTo(opphør1.fradato.minusDays(1))
            assertThat(nyeAndeler[0].beløp).isEqualTo(50)
            assertThat(nyeAndeler[0].kildeBehandlingId).isEqualTo(forrigeAndeler[0].kildeBehandlingId)
            assertThat(nyeAndeler[1].stønadFom).isEqualTo(nyAndelFom)
            assertThat(nyeAndeler[1].stønadTom).isEqualTo(nyAndelTom)
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
            val opphør1 = Periode(LocalDate.of(2021, 7, 1), LocalDate.of(2021, 7, 31))
            val opphør2 = Periode(LocalDate.of(2021, 10, 1), LocalDate.of(2021, 10, 31))
            val opphørsperioder = listOf(opphør1, opphør2)
            val forrigeAndeler = listOf(lagAndelTilkjentYtelse(50, forrigeAndelFom, forrigeAndelTom))
            val beløpsperioder = listOf(lagAndelTilkjentYtelse(200, nyAndelFom1, nyAndelTom1),
                                        lagAndelTilkjentYtelse(100, nyAndelFom2, nyAndelTom2))

            val nyeAndeler =
                    steg.slåSammenAndelerSomSkalVidereføres(beløpsperioder, lagTilkjentYtelse(forrigeAndeler), opphørsperioder)

            assertThat(nyeAndeler).hasSize(3)
            assertThat(nyeAndeler[0].stønadFom).isEqualTo(forrigeAndelFom)
            assertThat(nyeAndeler[0].stønadTom).isEqualTo(opphør1.fradato.minusDays(1))
            assertThat(nyeAndeler[0].beløp).isEqualTo(50)
            assertThat(nyeAndeler[0].kildeBehandlingId).isEqualTo(forrigeAndeler[0].kildeBehandlingId)
            assertThat(nyeAndeler[1].stønadFom).isEqualTo(nyAndelFom1)
            assertThat(nyeAndeler[1].stønadTom).isEqualTo(nyAndelTom1)
            assertThat(nyeAndeler[1].beløp).isEqualTo(200)
            assertThat(nyeAndeler[1].kildeBehandlingId).isNotEqualTo(forrigeAndeler[0].kildeBehandlingId)
            assertThat(nyeAndeler[2].stønadFom).isEqualTo(opphør2.tildato.plusDays(1))
            assertThat(nyeAndeler[2].stønadTom).isEqualTo(nyAndelTom2)
            assertThat(nyeAndeler[2].beløp).isEqualTo(100)
            assertThat(nyeAndeler[2].kildeBehandlingId).isNotEqualTo(forrigeAndeler[0].kildeBehandlingId)
        }

        @Test
        fun `takler opphold som dekker en hel periode`() {
            val forrigeAndelFom1 = LocalDate.of(2021, 1, 1)
            val forrigeAndelTom1 = LocalDate.of(2021, 10, 31)
            val forrigeAndelFom2 = LocalDate.of(2021, 12, 1)
            val forrigeAndelTom2 = LocalDate.of(2021, 12, 31)
            val opphør1 = Periode(LocalDate.of(2021, 7, 1), LocalDate.of(2021, 12, 31))
            val opphørsperioder = listOf(opphør1)
            val forrigeAndeler = listOf(lagAndelTilkjentYtelse(200, forrigeAndelFom1, forrigeAndelTom1),
                                        lagAndelTilkjentYtelse(100, forrigeAndelFom2, forrigeAndelTom2))

            val nyeAndeler = steg.slåSammenAndelerSomSkalVidereføres(listOf(), lagTilkjentYtelse(forrigeAndeler), opphørsperioder)

            assertThat(nyeAndeler).hasSize(1)
            assertThat(nyeAndeler[0].stønadFom).isEqualTo(forrigeAndelFom1)
            assertThat(nyeAndeler[0].stønadTom).isEqualTo(opphør1.fradato.minusDays(1))
            assertThat(nyeAndeler[0].beløp).isEqualTo(200)
            assertThat(nyeAndeler[0].kildeBehandlingId).isEqualTo(forrigeAndeler[0].kildeBehandlingId)

        }


        @Test
        fun `takler opphold som dekker flere perioder`() {
            val forrigeAndelFom1 = LocalDate.of(2021, 1, 1)
            val forrigeAndelTom1 = LocalDate.of(2021, 12, 31)
            val opphør1 = Periode(LocalDate.of(2021, 3, 1), LocalDate.of(2021, 6, 30))
            val opphør2 = Periode(LocalDate.of(2021, 8, 1), LocalDate.of(2021, 11, 30))
            val opphørsperioder = listOf(opphør1, opphør2)
            val forrigeAndeler = listOf(lagAndelTilkjentYtelse(200, forrigeAndelFom1, forrigeAndelTom1))

            val nyeAndeler = steg.slåSammenAndelerSomSkalVidereføres(listOf(), lagTilkjentYtelse(forrigeAndeler), opphørsperioder)

            assertThat(nyeAndeler).hasSize(1)
            assertThat(nyeAndeler[0].stønadFom).isEqualTo(LocalDate.of(2021, 1, 1))
            assertThat(nyeAndeler[0].stønadTom).isEqualTo(LocalDate.of(2021, 2, 28))
        }


        @Test
        fun `takler to opphørsperioder som i praksis omslutter hele andelen`() {
            val forrigeAndelFom1 = LocalDate.of(2021, 1, 1)
            val forrigeAndelTom1 = LocalDate.of(2021, 12, 31)
            val opphør1 = Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 6, 30))
            val opphør2 = Periode(LocalDate.of(2021, 7, 1), LocalDate.of(2021, 12, 31))
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

            val slot = slot<TilkjentYtelse>()
            every { tilkjentYtelseService.opprettTilkjentYtelse(capture(slot)) } answers { firstArg() }
            every { tilkjentYtelseService.hentForBehandling(any()) } returns
                    lagTilkjentYtelse(listOf(lagAndelTilkjentYtelse(100, andelFom.atDay(1), andelTom.atEndOfMonth())))
            every { beregningService.beregnYtelse(any(), any()) } answers {
                firstArg<List<Periode>>().map { lagBeløpsperiode(it.fradato, it.tildato) }
            }

            val opphørsperiode = opphørsperiode(opphørFom, opphørFom)
            val innvilgetPeriode1 = innvilgetPeriode(andelFom, andelTom)

            utførSteg(BehandlingType.REVURDERING,
                      Innvilget(resultatType = ResultatType.INNVILGE,
                                perioder = listOf(opphørsperiode, innvilgetPeriode1),
                                inntekter = listOf(inntekt(andelTom)),
                                inntektBegrunnelse = "null",
                                periodeBegrunnelse = "null"
                      ),
                      forrigeBehandlingId = UUID.randomUUID())

            assertThat(slot.captured.andelerTilkjentYtelse).hasSize(1)
            assertThat(slot.captured.andelerTilkjentYtelse[0].stønadFom).isEqualTo(andelFom.atDay(1))
            assertThat(slot.captured.andelerTilkjentYtelse[0].stønadTom).isEqualTo(andelTom.atEndOfMonth())
            assertThat(slot.captured.opphørsdato).isEqualTo(opphørFom.atDay(1))
        }

        @Test
        internal fun `skal kunne innvilge med opphør med opphør midt i en periode - setter ikke opphørsdato`() {
            val opphørFom = YearMonth.of(2021, 8)
            val andelFom = YearMonth.of(2021, 6)
            val andelTom = YearMonth.of(2022, 6)
            val innvilgetFom = opphørFom.plusMonths(1)
            val innvilgetTom = andelTom

            val slot = slot<TilkjentYtelse>()
            every { tilkjentYtelseService.opprettTilkjentYtelse(capture(slot)) } answers { firstArg() }
            every { tilkjentYtelseService.hentForBehandling(any()) } returns
                    lagTilkjentYtelse(listOf(lagAndelTilkjentYtelse(100, andelFom.atDay(1), andelTom.atEndOfMonth())))
            every { beregningService.beregnYtelse(any(), any()) } answers {
                firstArg<List<Periode>>().map { lagBeløpsperiode(it.fradato, it.tildato) }
            }

            val opphørsperiode = opphørsperiode(opphørFom, opphørFom)
            val innvilgetPeriode1 = innvilgetPeriode(innvilgetFom, innvilgetTom)

            utførSteg(BehandlingType.REVURDERING,
                      Innvilget(resultatType = ResultatType.INNVILGE,
                                perioder = listOf(opphørsperiode, innvilgetPeriode1),
                                inntekter = listOf(inntekt(andelTom)),
                                inntektBegrunnelse = "null",
                                periodeBegrunnelse = "null"
                      ),
                      forrigeBehandlingId = UUID.randomUUID())

            val andelerTilkjentYtelse = slot.captured.andelerTilkjentYtelse.sortedBy { it.stønadFom }
            assertThat(andelerTilkjentYtelse).hasSize(2)
            assertThat(andelerTilkjentYtelse[0].stønadFom).isEqualTo(andelFom.atDay(1))
            assertThat(andelerTilkjentYtelse[0].stønadTom).isEqualTo(opphørFom.atDay(1).minusDays(1))
            assertThat(andelerTilkjentYtelse[1].stønadFom).isEqualTo(innvilgetFom.atDay(1))
            assertThat(andelerTilkjentYtelse[1].stønadTom).isEqualTo(innvilgetTom.atEndOfMonth())
            assertThat(slot.captured.opphørsdato).isNull()
        }

        @Test
        internal fun `opphør etter innvilget periode - setter ikke opphørsdato`() {
            val opphørFom = YearMonth.of(2022, 7)
            val andelFom = YearMonth.of(2021, 6)
            val andelTom = YearMonth.of(2022, 6)

            val slot = slot<TilkjentYtelse>()
            every { tilkjentYtelseService.opprettTilkjentYtelse(capture(slot)) } answers { firstArg() }
            every { tilkjentYtelseService.hentForBehandling(any()) } returns
                    lagTilkjentYtelse(listOf(lagAndelTilkjentYtelse(100, andelFom.atDay(1), andelTom.atEndOfMonth())))
            every { beregningService.beregnYtelse(any(), any()) } answers {
                firstArg<List<Periode>>().map { lagBeløpsperiode(it.fradato, it.tildato) }
            }

            val opphørsperiode = opphørsperiode(opphørFom, opphørFom)
            val innvilgetPeriode1 = innvilgetPeriode(andelFom, andelTom)

            utførSteg(BehandlingType.REVURDERING,
                      Innvilget(resultatType = ResultatType.INNVILGE,
                                perioder = listOf(innvilgetPeriode1, opphørsperiode),
                                inntekter = listOf(inntekt(andelTom)),
                                inntektBegrunnelse = "null",
                                periodeBegrunnelse = "null"
                      ),
                      forrigeBehandlingId = UUID.randomUUID())

            assertThat(slot.captured.andelerTilkjentYtelse).hasSize(1)
            assertThat(slot.captured.andelerTilkjentYtelse[0].stønadFom).isEqualTo(andelFom.atDay(1))
            assertThat(slot.captured.andelerTilkjentYtelse[0].stønadTom).isEqualTo(andelTom.atEndOfMonth())
            assertThat(slot.captured.opphørsdato).isNull()
        }

    }

    @Nested
    inner class InnvilgeMedOpphør {

        @Test
        internal fun `kan ikke innvilge med opphør, uten opphørsperioder`() {
            val opphørFom = YearMonth.of(2021, 1)
            val andelFom = YearMonth.of(2021, 6).atDay(1)
            val andelTom = YearMonth.of(2021, 6).atEndOfMonth()

            every { tilkjentYtelseService.hentForBehandling(any()) } returns
                    lagTilkjentYtelse(listOf(lagAndelTilkjentYtelse(100, andelFom, andelTom)))

            assertThatThrownBy {
                utførSteg(BehandlingType.REVURDERING,
                          innvilget(listOf(opphørsperiode(opphørFom, opphørFom)), listOf(inntekt(opphørFom))),
                          forrigeBehandlingId = UUID.randomUUID())
            }.hasMessageContaining("Må ha innvilgelsesperioder i tillegg til opphørsperioder")
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
                firstArg<List<Periode>>().map { lagBeløpsperiode(it.fradato, it.tildato) }
            }

            utførSteg(BehandlingType.REVURDERING,
                      innvilget(listOf(opphørsperiode(opphørFom, opphørFom),
                                       innvilgetPeriode(nyttInnvilgetFom, nyttInnvilgetFom)),
                                listOf(inntekt(opphørFom))),
                      forrigeBehandlingId = UUID.randomUUID())

            assertThat(slot.captured.opphørsdato).isEqualTo(opphørFom.atDay(1))
            assertThat(slot.captured.andelerTilkjentYtelse).hasSize(1)
            assertThat(slot.captured.andelerTilkjentYtelse[0].stønadFom).isEqualTo(nyttInnvilgetFom.atDay(1))
            assertThat(slot.captured.andelerTilkjentYtelse[0].stønadTom).isEqualTo(nyttInnvilgetFom.atEndOfMonth())
        }

        @Test
        internal fun `finnes tidligere opphørsdato fra før, og vurderer med opphør etter det datoet - beholder tidligere opphørsdato`() {
            val tidligereOpphør = YearMonth.of(2020, 1).atDay(1)
            val nyttOpphørsdato = YearMonth.of(2021, 1)
            val nyttInnvilgetFom = nyttOpphørsdato.plusMonths(1)
            val andelFom = YearMonth.of(2021, 6).atDay(1)
            val andelTom = YearMonth.of(2021, 6).atEndOfMonth()

            every { tilkjentYtelseService.hentForBehandling(any()) } returns
                    lagTilkjentYtelse(listOf(lagAndelTilkjentYtelse(100, andelFom, andelTom)),
                                      opphørsdato = tidligereOpphør)
            every { beregningService.beregnYtelse(any(), any()) } answers {
                firstArg<List<Periode>>().map { lagBeløpsperiode(it.fradato, it.tildato) }
            }

            utførSteg(BehandlingType.REVURDERING,
                      innvilget(listOf(opphørsperiode(nyttOpphørsdato, nyttOpphørsdato),
                                       innvilgetPeriode(nyttInnvilgetFom, nyttInnvilgetFom)),
                                listOf(inntekt(nyttOpphørsdato))),
                      forrigeBehandlingId = UUID.randomUUID())

            assertThat(slot.captured.opphørsdato).isEqualTo(tidligereOpphør)
            assertThat(slot.captured.andelerTilkjentYtelse).hasSize(1)
            assertThat(slot.captured.andelerTilkjentYtelse[0].stønadFom).isEqualTo(nyttInnvilgetFom.atDay(1))
            assertThat(slot.captured.andelerTilkjentYtelse[0].stønadTom).isEqualTo(nyttInnvilgetFom.atEndOfMonth())
        }

        @Test
        internal fun `finnes tidligere opphørsdato fra før, og vurderer med opphør etter før det datoet`() {
            val tidligereOpphør = YearMonth.of(2021, 6).atDay(1)
            val nyttOpphørsdato = YearMonth.of(2021, 1)
            val nyttInnvilgetFom = nyttOpphørsdato.plusMonths(1)
            val andelFom = YearMonth.of(2021, 6).atDay(1)
            val andelTom = YearMonth.of(2021, 6).atEndOfMonth()

            every { tilkjentYtelseService.hentForBehandling(any()) } returns
                    lagTilkjentYtelse(listOf(lagAndelTilkjentYtelse(100, andelFom, andelTom)),
                                      opphørsdato = tidligereOpphør)
            every { beregningService.beregnYtelse(any(), any()) } answers {
                firstArg<List<Periode>>().map { lagBeløpsperiode(it.fradato, it.tildato) }
            }

            utførSteg(BehandlingType.REVURDERING,
                      innvilget(listOf(opphørsperiode(nyttOpphørsdato, nyttOpphørsdato),
                                       innvilgetPeriode(nyttInnvilgetFom, nyttInnvilgetFom)),
                                listOf(inntekt(nyttOpphørsdato))),
                      forrigeBehandlingId = UUID.randomUUID())

            assertThat(slot.captured.opphørsdato).isEqualTo(nyttOpphørsdato.atDay(1))
            assertThat(slot.captured.andelerTilkjentYtelse).hasSize(1)
            assertThat(slot.captured.andelerTilkjentYtelse[0].stønadFom).isEqualTo(nyttInnvilgetFom.atDay(1))
            assertThat(slot.captured.andelerTilkjentYtelse[0].stønadTom).isEqualTo(nyttInnvilgetFom.atEndOfMonth())
        }

    }

    @Nested
    inner class Opphør {

        @Test
        internal fun `skal kunne opphøre bak i tid - skal sette opphørsdato på tilkjent ytelse`() {
            val opphørFom = YearMonth.of(2021, 1)
            val andelFom = YearMonth.of(2021, 6).atDay(1)
            val andelTom = YearMonth.of(2021, 6).atEndOfMonth()

            every { tilkjentYtelseService.hentForBehandling(any()) } returns
                    lagTilkjentYtelse(listOf(lagAndelTilkjentYtelse(100, andelFom, andelTom)))

            utførSteg(BehandlingType.REVURDERING,
                      Opphør(opphørFom = opphørFom, begrunnelse = "null"),
                      forrigeBehandlingId = UUID.randomUUID())

            assertThat(slot.captured.andelerTilkjentYtelse).hasSize(0)
            assertThat(slot.captured.opphørsdato).isEqualTo(opphørFom.atDay(1))
        }

        @Test
        internal fun `skal kunne opphøre midt i en tidligere periode`() {
            val opphørFom = YearMonth.of(2021, 8)
            val andelFom = YearMonth.of(2021, 6).atDay(1)
            val andelTom = YearMonth.of(2022, 6).atEndOfMonth()

            every { tilkjentYtelseService.hentForBehandling(any()) } returns
                    lagTilkjentYtelse(listOf(lagAndelTilkjentYtelse(100, andelFom, andelTom)))

            utførSteg(BehandlingType.REVURDERING,
                      Opphør(opphørFom = opphørFom, begrunnelse = "null"),
                      forrigeBehandlingId = UUID.randomUUID())

            assertThat(slot.captured.opphørsdato).isNull()
            assertThat(slot.captured.andelerTilkjentYtelse).hasSize(1)
            assertThat(slot.captured.andelerTilkjentYtelse[0].stønadFom).isEqualTo(andelFom)
            assertThat(slot.captured.andelerTilkjentYtelse[0].stønadTom).isEqualTo(opphørFom.atDay(1).minusDays(1))
        }

        @Test
        internal fun `skal ikke kunne opphøre frem i tid`() {
            val opphørFom = YearMonth.of(2022, 1)
            val andelFom = YearMonth.of(2021, 6).atDay(1)
            val andelTom = YearMonth.of(2021, 6).atEndOfMonth()

            every { tilkjentYtelseService.hentForBehandling(any()) } returns
                    lagTilkjentYtelse(listOf(lagAndelTilkjentYtelse(100, andelFom, andelTom)))

            assertThatThrownBy {
                utførSteg(BehandlingType.REVURDERING,
                          Opphør(opphørFom = opphørFom, begrunnelse = "null"),
                          forrigeBehandlingId = UUID.randomUUID())
            }.hasMessageContaining("Kan ikke opphøre frem i tiden")
        }

        @Test
        internal fun `skal kunne opphøre før datoet for ett tidligere opphør`() {
            val opphørFom = YearMonth.of(2022, 1)

            every { tilkjentYtelseService.hentForBehandling(any()) } returns
                    lagTilkjentYtelse(listOf(), opphørsdato = opphørFom.atDay(1).plusMonths(1))


            utførSteg(BehandlingType.REVURDERING,
                      Opphør(opphørFom = opphørFom, begrunnelse = "null"),
                      forrigeBehandlingId = UUID.randomUUID())
            assertThat(slot.captured.opphørsdato).isEqualTo(opphørFom.atDay(1))
            assertThat(slot.captured.andelerTilkjentYtelse).isEmpty()
        }

        @Test
        internal fun `skal ikke kunne opphøre etter datoet for ett tidligere opphør`() {
            val opphørFom = YearMonth.of(2022, 1)

            every { tilkjentYtelseService.hentForBehandling(any()) } returns
                    lagTilkjentYtelse(listOf(), opphørsdato = opphørFom.atDay(1).minusDays(1))

            assertThatThrownBy {
                utførSteg(BehandlingType.REVURDERING,
                          Opphør(opphørFom = opphørFom, begrunnelse = "null"),
                          forrigeBehandlingId = UUID.randomUUID())
            }.hasMessageContaining("Forrige vedtak er allerede opphørt")
        }

        @Test
        internal fun `skal opphøre før ett tidligere opphør, skal sette nytt opphørsdato`() {
            val tidligereOpphør = YearMonth.of(2022, 1).atDay(1)
            val opphørFom = YearMonth.of(2021, 1)
            val andelFom = YearMonth.of(2022, 6).atDay(1)
            val andelTom = YearMonth.of(2022, 6).atEndOfMonth()

            every { tilkjentYtelseService.hentForBehandling(any()) } returns
                    lagTilkjentYtelse(listOf(lagAndelTilkjentYtelse(100, andelFom, andelTom)),
                                      opphørsdato = tidligereOpphør)

            utførSteg(BehandlingType.REVURDERING,
                      Opphør(opphørFom = opphørFom, begrunnelse = "null"),
                      forrigeBehandlingId = UUID.randomUUID())
            assertThat(slot.captured.opphørsdato).isEqualTo(opphørFom.atDay(1))
            assertThat(slot.captured.andelerTilkjentYtelse).isEmpty()
        }

        @Test
        internal fun `skal opphøre etter datoet for ett tidligere opphør, men før tidligere andeler - beholder tidligere opphørsdato`() {
            val tidligereOpphør = YearMonth.of(2021, 1).atDay(1)
            val opphørFom = YearMonth.of(2022, 1)
            val andelFom = YearMonth.of(2022, 6).atDay(1)
            val andelTom = YearMonth.of(2022, 6).atEndOfMonth()

            every { tilkjentYtelseService.hentForBehandling(any()) } returns
                    lagTilkjentYtelse(listOf(lagAndelTilkjentYtelse(100, andelFom, andelTom)),
                                      opphørsdato = tidligereOpphør)

            utførSteg(BehandlingType.REVURDERING,
                      Opphør(opphørFom = opphørFom, begrunnelse = "null"),
                      forrigeBehandlingId = UUID.randomUUID())
            assertThat(slot.captured.opphørsdato).isEqualTo(tidligereOpphør)
            assertThat(slot.captured.andelerTilkjentYtelse).isEmpty()
        }

        @Test
        internal fun `skal opphøre etter datoet for ett tidligere opphør, men etter tidligere andeler - beholder tidligere opphørsdato`() {
            val tidligereOpphør = YearMonth.of(2021, 1).atDay(1)
            val opphørFom = YearMonth.of(2022, 8)
            val andelFom = YearMonth.of(2022, 6).atDay(1)
            val andelTom = YearMonth.of(2022, 10).atEndOfMonth()

            every { tilkjentYtelseService.hentForBehandling(any()) } returns
                    lagTilkjentYtelse(listOf(lagAndelTilkjentYtelse(100, andelFom, andelTom)),
                                      opphørsdato = tidligereOpphør)

            utførSteg(BehandlingType.REVURDERING,
                      Opphør(opphørFom = opphørFom, begrunnelse = "null"),
                      forrigeBehandlingId = UUID.randomUUID())
            assertThat(slot.captured.opphørsdato).isEqualTo(tidligereOpphør)
            assertThat(slot.captured.andelerTilkjentYtelse).hasSize(1)
            assertThat(slot.captured.andelerTilkjentYtelse[0].stønadFom).isEqualTo(andelFom)
            assertThat(slot.captured.andelerTilkjentYtelse[0].stønadTom).isEqualTo(opphørFom.atDay(1).minusDays(1))
        }

    }

    private fun innvilget(perioder: List<VedtaksperiodeDto>,
                          inntekter: List<Inntekt>) =
            Innvilget(resultatType = ResultatType.INNVILGE,
                      perioder = perioder,
                      inntekter = inntekter,
                      inntektBegrunnelse = "null",
                      periodeBegrunnelse = "null")

    private fun lagBeløpsperiode(fom: LocalDate, tom: LocalDate) =
            Beløpsperiode(Periode(fom, tom), null, BigDecimal.ZERO, BigDecimal.ZERO)

    private fun opphørsperiode(opphørFom: YearMonth, opphørTom: YearMonth) =
            VedtaksperiodeDto(årMånedFra = opphørFom,
                              årMånedTil = opphørTom,
                              aktivitet = AktivitetType.IKKE_AKTIVITETSPLIKT,
                              periodeType = VedtaksperiodeType.MIDLERTIDIG_OPPHØR)

    private fun innvilgetPeriode(andelFom: YearMonth, andelTom: YearMonth) =
            VedtaksperiodeDto(årMånedFra = andelFom,
                              årMånedTil = andelTom,
                              aktivitet = AktivitetType.FORLENGELSE_STØNAD_PÅVENTE_ARBEID,
                              periodeType = VedtaksperiodeType.HOVEDPERIODE)

    private fun inntekt(andelTom: YearMonth) =
            Inntekt(andelTom,
                    BigDecimal(100000),
                    samordningsfradrag = BigDecimal.ZERO)

    private fun utførSteg(type: BehandlingType,
                          vedtak: VedtakDto = Innvilget(resultatType = ResultatType.INNVILGE,
                                                        periodeBegrunnelse = "",
                                                        inntektBegrunnelse = ""),
                          forrigeBehandlingId: UUID? = null) {
        steg.utførSteg(behandling(fagsak(), type = type, forrigeBehandlingId = forrigeBehandlingId), data = vedtak)
    }
}
