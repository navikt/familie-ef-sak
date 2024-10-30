package no.nav.familie.ef.sak.no.nav.familie.ef.sak.beregning

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.beregning.BeregningService
import no.nav.familie.ef.sak.beregning.Grunnbeløpsperioder
import no.nav.familie.ef.sak.beregning.ValiderOmregningService
import no.nav.familie.ef.sak.infrastruktur.exception.ApiFeil
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext.SYSTEM_FORKORTELSE
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.saksbehandling
import no.nav.familie.ef.sak.repository.vedtak
import no.nav.familie.ef.sak.testutil.mockTestMedGrunnbeløpFra2022
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseRepository
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vedtak.domain.AktivitetType
import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType
import no.nav.familie.ef.sak.vedtak.dto.InnvilgelseOvergangsstønad
import no.nav.familie.ef.sak.vedtak.dto.VedtaksperiodeDto
import no.nav.familie.ef.sak.vedtak.historikk.VedtakHistorikkService
import no.nav.familie.ef.sak.økonomi.lagAndelTilkjentYtelse
import no.nav.familie.ef.sak.økonomi.lagTilkjentYtelse
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.felles.Månedsperiode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

class ValiderOmregningServiceTest {
    val vedtakService = mockk<VedtakService>()
    val tilkjentYtelseRepository = mockk<TilkjentYtelseRepository>()
    val beregningService = BeregningService()
    val vedtakHistorikkService = mockk<VedtakHistorikkService>()
    val validerOmregningService =
        ValiderOmregningService(
            vedtakService,
            tilkjentYtelseRepository,
            beregningService,
            vedtakHistorikkService,
        )

    @Test
    internal fun `validerHarGammelGOgKanLagres har beregnet med feil beløp`() {
        val saksbehandling = saksbehandling(fagsak = fagsak(), behandling(forrigeBehandlingId = UUID.randomUUID()))
        mockVedtakOgForrigeTilkjentYtelse(saksbehandling)
        mockNyTilkjentYtelse(saksbehandling, medRiktigBeløp = false)
        mockTestMedGrunnbeløpFra2022 {
            assertThatThrownBy { validerOmregningService.validerHarGammelGOgKanLagres(saksbehandling) }
                .isInstanceOf(ApiFeil::class.java)
        }
    }

    @Test
    internal fun `validerHarGammelGOgKanLagres har beregnet med nytt g fra før`() {
        val saksbehandling = saksbehandling(fagsak = fagsak(), behandling(forrigeBehandlingId = UUID.randomUUID()))
        mockVedtakOgForrigeTilkjentYtelse(saksbehandling)
        mockNyTilkjentYtelse(saksbehandling, medRiktigBeløp = true)

        validerOmregningService.validerHarGammelGOgKanLagres(saksbehandling)
    }

    @Test
    internal fun `validerHarGammelGOgKanLagres - tilkjent ytelse inneholder gammelt G`() {
        val saksbehandling = saksbehandling(fagsak = fagsak(), behandling(forrigeBehandlingId = UUID.randomUUID()))
        mockVedtakOgForrigeTilkjentYtelse(saksbehandling)

        every { tilkjentYtelseRepository.findByBehandlingId(saksbehandling.id) } returns
            lagTilkjentYtelse(
                andelerTilkjentYtelse =
                    listOf(
                        lagAndelTilkjentYtelse(
                            fraOgMed = LocalDate.of(2022, 4, 1),
                            tilOgMed = LocalDate.of(2022, 8, 30),
                            samordningsfradrag = 5000,
                            beløp = 0,
                        ),
                    ),
                grunnbeløpsmåned = YearMonth.of(2021, 5),
            )

        validerOmregningService.validerHarGammelGOgKanLagres(saksbehandling)
    }

    @Nested
    inner class ValiderHarSammePerioderSomTidligereVedtak {
        private val år = Grunnbeløpsperioder.nyesteGrunnbeløpGyldigFraOgMed.year

        @Test
        internal fun `skal ikke validere hvis det er maskinell g-omregning`() {
            val saksbehandling =
                saksbehandling(årsak = BehandlingÅrsak.G_OMREGNING).copy(opprettetAv = SYSTEM_FORKORTELSE)
            val vedtak = InnvilgelseOvergangsstønad(null, null, listOf())

            validerOmregningService.validerHarSammePerioderSomTidligereVedtak(vedtak, saksbehandling)
        }

        @Test
        internal fun `skal ikke validere hvis årsak er nye opplysninger`() {
            val saksbehandling =
                saksbehandling(årsak = BehandlingÅrsak.NYE_OPPLYSNINGER).copy(opprettetAv = "saksbehandler")
            val vedtak = InnvilgelseOvergangsstønad(null, null, listOf())

            validerOmregningService.validerHarSammePerioderSomTidligereVedtak(vedtak, saksbehandling)
        }

        @Test
        internal fun `skal validere ok hvis perioder er like`() {
            val saksbehandling = manuellGOmregning()
            val fra = YearMonth.of(år, 5)
            val til = fra
            val aktivitet = AktivitetType.BARNET_ER_SYKT
            val periodeType = VedtaksperiodeType.PERIODE_FØR_FØDSEL
            every { vedtakHistorikkService.hentVedtakForOvergangsstønadFraDato(any(), any()) } returns
                innvilge(VedtaksperiodeDto(fra, til, Månedsperiode(fra, til), aktivitet, periodeType))

            val vedtak = innvilge(VedtaksperiodeDto(fra, til, Månedsperiode(fra, til), aktivitet, periodeType))

            validerOmregningService.validerHarSammePerioderSomTidligereVedtak(vedtak, saksbehandling)
        }

        @Test
        internal fun `skal validere ok hvis perioder er, men aktivitet og periodetype er migrering`() {
            val saksbehandling = manuellGOmregning()
            val fra = YearMonth.of(år, 5)
            val til = fra
            val aktivitet = AktivitetType.BARNET_ER_SYKT
            val periodeType = VedtaksperiodeType.PERIODE_FØR_FØDSEL

            every { vedtakHistorikkService.hentVedtakForOvergangsstønadFraDato(any(), any()) } returns
                innvilge(
                    VedtaksperiodeDto(
                        fra,
                        til,
                        Månedsperiode(fra, til),
                        AktivitetType.MIGRERING,
                        VedtaksperiodeType.MIGRERING,
                    ),
                )

            val vedtak = innvilge(VedtaksperiodeDto(fra, til, Månedsperiode(fra, til), aktivitet, periodeType))
            validerOmregningService.validerHarSammePerioderSomTidligereVedtak(vedtak, saksbehandling)
        }

        @Test
        internal fun `skal ikke g-omregne behandlinger som ikke har perioder etter g-dato`() {
            val saksbehandling = manuellGOmregning()
            every { vedtakHistorikkService.hentVedtakForOvergangsstønadFraDato(any(), any()) } returns innvilge()

            val vedtak = InnvilgelseOvergangsstønad(null, null, listOf())

            assertThatThrownBy {
                validerOmregningService.validerHarSammePerioderSomTidligereVedtak(
                    vedtak,
                    saksbehandling,
                )
            }.isInstanceOf(ApiFeil::class.java)
                .hasMessageContaining("ikke har noen tidligere perioder")
        }

        @Test
        internal fun `skal ikke tillate annen aktivitet enn tidligere aktivitet`() {
            val saksbehandling = manuellGOmregning()
            val fra = YearMonth.of(år, 5)
            val til = fra
            val aktivitet = AktivitetType.BARNET_ER_SYKT
            val periodeType = VedtaksperiodeType.PERIODE_FØR_FØDSEL
            every { vedtakHistorikkService.hentVedtakForOvergangsstønadFraDato(any(), any()) } returns
                innvilge(
                    VedtaksperiodeDto(
                        fra,
                        til,
                        Månedsperiode(fra, til),
                        AktivitetType.IKKE_AKTIVITETSPLIKT,
                        VedtaksperiodeType.MIGRERING,
                    ),
                )

            val vedtak = innvilge(VedtaksperiodeDto(fra, til, Månedsperiode(fra, til), aktivitet, periodeType))

            assertThatThrownBy {
                validerOmregningService.validerHarSammePerioderSomTidligereVedtak(
                    vedtak,
                    saksbehandling,
                )
            }.isInstanceOf(ApiFeil::class.java)
                .hasMessageContaining("har annen aktivitet")
        }

        @Test
        internal fun `skal ikke tillate annen periodetype enn tidligere aktivitet`() {
            val saksbehandling = manuellGOmregning()
            val fra = YearMonth.of(år, 5)
            val til = fra
            val aktivitet = AktivitetType.BARNET_ER_SYKT
            val periodeType = VedtaksperiodeType.PERIODE_FØR_FØDSEL
            every { vedtakHistorikkService.hentVedtakForOvergangsstønadFraDato(any(), any()) } returns
                innvilge(
                    VedtaksperiodeDto(
                        fra,
                        til,
                        Månedsperiode(fra, til),
                        AktivitetType.MIGRERING,
                        VedtaksperiodeType.HOVEDPERIODE,
                    ),
                )

            val vedtak = innvilge(VedtaksperiodeDto(fra, til, Månedsperiode(fra, til), aktivitet, periodeType))

            assertThatThrownBy {
                validerOmregningService.validerHarSammePerioderSomTidligereVedtak(
                    vedtak,
                    saksbehandling,
                )
            }.isInstanceOf(ApiFeil::class.java)
                .hasMessageContaining("annen type periode")
        }

        @Test
        internal fun `skal ikke tillate tom-dato som endret seg`() {
            val saksbehandling = manuellGOmregning()
            val fra = YearMonth.of(år, 5)
            val til = fra
            val revurderingTil = til.plusMonths(1)
            val aktivitet = AktivitetType.BARNET_ER_SYKT
            val periodeType = VedtaksperiodeType.PERIODE_FØR_FØDSEL
            every { vedtakHistorikkService.hentVedtakForOvergangsstønadFraDato(any(), any()) } returns
                innvilge(
                    VedtaksperiodeDto(
                        fra,
                        til,
                        Månedsperiode(fra, til),
                        AktivitetType.MIGRERING,
                        VedtaksperiodeType.HOVEDPERIODE,
                    ),
                )

            val vedtak =
                innvilge(
                    VedtaksperiodeDto(
                        fra,
                        revurderingTil,
                        Månedsperiode(fra, revurderingTil),
                        aktivitet,
                        periodeType,
                    ),
                )

            assertThatThrownBy {
                validerOmregningService.validerHarSammePerioderSomTidligereVedtak(
                    vedtak,
                    saksbehandling,
                )
            }.isInstanceOf(ApiFeil::class.java)
                .hasMessageContaining("har annet tom-dato")
        }

        @Test
        internal fun `antall perioder er ulikt`() {
            val saksbehandling = manuellGOmregning()
            val fra = YearMonth.of(år, 5)
            val til = fra
            every { vedtakHistorikkService.hentVedtakForOvergangsstønadFraDato(any(), any()) } returns
                innvilge(
                    VedtaksperiodeDto(
                        fra,
                        til,
                        Månedsperiode(fra, til),
                        AktivitetType.MIGRERING,
                        VedtaksperiodeType.HOVEDPERIODE,
                    ),
                )

            val vedtak = InnvilgelseOvergangsstønad(null, null, listOf())

            assertThatThrownBy {
                validerOmregningService.validerHarSammePerioderSomTidligereVedtak(
                    vedtak,
                    saksbehandling,
                )
            }.isInstanceOf(ApiFeil::class.java)
                .hasMessageContaining("Antall vedtaksperioder er ulikt fra tidligere vedtak")
        }

        @Test
        internal fun `revurdering skal ta første datoet fra nytt g, og ikke revurdere fra april i dette tilfellet`() {
            val saksbehandling = manuellGOmregning()
            val fra = YearMonth.of(år, 5)
            val til = YearMonth.of(år, 8)
            val aktivitet = AktivitetType.BARNET_ER_SYKT
            val periodeType = VedtaksperiodeType.PERIODE_FØR_FØDSEL
            every { vedtakHistorikkService.hentVedtakForOvergangsstønadFraDato(any(), any()) } returns
                innvilge(VedtaksperiodeDto(fra, til, Månedsperiode(fra, til), aktivitet, periodeType))

            val vedtak = innvilge(VedtaksperiodeDto(fra, til, Månedsperiode(fra, til), aktivitet, periodeType))

            validerOmregningService.validerHarSammePerioderSomTidligereVedtak(vedtak, saksbehandling)
        }

        @Test
        internal fun `revurdering skal ta første datoet fra nytt g, utenom sanksjoner`() {
            val saksbehandling = manuellGOmregning()
            val fra = YearMonth.of(år, 5)
            val andrePeriodeFra = fra.plusMonths(1)
            val til = YearMonth.of(år, 8)
            val aktivitet = AktivitetType.BARNET_ER_SYKT
            val periodeType = VedtaksperiodeType.PERIODE_FØR_FØDSEL
            every { vedtakHistorikkService.hentVedtakForOvergangsstønadFraDato(any(), any()) } returns
                innvilge(
                    VedtaksperiodeDto(
                        fra,
                        fra,
                        Månedsperiode(fra, fra),
                        AktivitetType.IKKE_AKTIVITETSPLIKT,
                        VedtaksperiodeType.SANKSJON,
                    ),
                    VedtaksperiodeDto(
                        andrePeriodeFra,
                        til,
                        Månedsperiode(andrePeriodeFra, til),
                        aktivitet,
                        periodeType,
                    ),
                )

            val vedtak =
                innvilge(
                    VedtaksperiodeDto(
                        andrePeriodeFra,
                        til,
                        Månedsperiode(andrePeriodeFra, til),
                        aktivitet,
                        periodeType,
                    ),
                )

            validerOmregningService.validerHarSammePerioderSomTidligereVedtak(vedtak, saksbehandling)
        }

        private fun innvilge(vararg perioder: VedtaksperiodeDto) = InnvilgelseOvergangsstønad(null, null, perioder.toList())

        private fun manuellGOmregning() = saksbehandling(årsak = BehandlingÅrsak.G_OMREGNING).copy(opprettetAv = "saksbehandler")
    }

    private fun mockNyTilkjentYtelse(
        saksbehandling: Saksbehandling,
        medRiktigBeløp: Boolean = true,
    ) {
        every { tilkjentYtelseRepository.findByBehandlingId(saksbehandling.id) } returns
            lagTilkjentYtelse(
                andelerTilkjentYtelse =
                    listOf(
                        lagAndelTilkjentYtelse(
                            fraOgMed = LocalDate.of(2022, 4, 1),
                            tilOgMed = LocalDate.of(2022, 4, 30),
                            samordningsfradrag = 5000,
                            beløp = 14950,
                        ),
                        lagAndelTilkjentYtelse(
                            fraOgMed = LocalDate.of(2022, 5, 1),
                            tilOgMed = LocalDate.of(2022, 8, 30),
                            samordningsfradrag = 5000,
                            beløp = if (medRiktigBeløp) 15902 else 0,
                        ),
                    ),
            )
    }

    private fun mockVedtakOgForrigeTilkjentYtelse(saksbehandling: Saksbehandling) {
        every { tilkjentYtelseRepository.findByBehandlingId(saksbehandling.forrigeBehandlingId!!) } returns
            lagTilkjentYtelse(emptyList(), grunnbeløpsmåned = YearMonth.of(2021, 5))
        every { vedtakService.hentVedtak(saksbehandling.id) } returns vedtak(saksbehandling.id)
    }
}
