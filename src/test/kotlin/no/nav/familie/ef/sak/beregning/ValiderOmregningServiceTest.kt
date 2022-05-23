package no.nav.familie.ef.sak.no.nav.familie.ef.sak.beregning

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.beregning.BeregningService
import no.nav.familie.ef.sak.beregning.ValiderOmregningService
import no.nav.familie.ef.sak.infrastruktur.exception.ApiFeil
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.saksbehandling
import no.nav.familie.ef.sak.repository.vedtak
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseRepository
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.økonomi.lagAndelTilkjentYtelse
import no.nav.familie.ef.sak.økonomi.lagTilkjentYtelse
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class ValiderOmregningServiceTest {

    val vedtakService = mockk<VedtakService>()
    val tilkjentYtelseRepository = mockk<TilkjentYtelseRepository>()
    val beregningService = BeregningService()
    val validerOmregningService = ValiderOmregningService(vedtakService, tilkjentYtelseRepository, beregningService)

    @Test
    internal fun `validerHarGammelGOgKanLagres har beregnet med feil beløp`() {
        val saksbehandling = saksbehandling(fagsak = fagsak(), behandling(forrigeBehandlingId = UUID.randomUUID()))
        mockVedtakOgForrigeTilkjentYtelse(saksbehandling)
        mockNyTilkjentYtelse(saksbehandling, medRiktigBeløp = false)

        assertThatThrownBy { validerOmregningService.validerHarGammelGOgKanLagres(saksbehandling) }
                .isInstanceOf(ApiFeil::class.java)
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
                lagTilkjentYtelse(andelerTilkjentYtelse = listOf(
                        lagAndelTilkjentYtelse(fraOgMed = LocalDate.of(2022, 4, 1),
                                               tilOgMed = LocalDate.of(2022, 8, 30),
                                               samordningsfradrag = 5000,
                                               beløp = 0)),
                                  grunnbeløpsdato = LocalDate.of(2021, 5, 1))

        validerOmregningService.validerHarGammelGOgKanLagres(saksbehandling)
    }

    private fun mockNyTilkjentYtelse(saksbehandling: Saksbehandling, medRiktigBeløp: Boolean = true) {
        every { tilkjentYtelseRepository.findByBehandlingId(saksbehandling.id) } returns
                lagTilkjentYtelse(andelerTilkjentYtelse = listOf(
                        lagAndelTilkjentYtelse(fraOgMed = LocalDate.of(2022, 4, 1),
                                               tilOgMed = LocalDate.of(2022, 4, 30),
                                               samordningsfradrag = 5000,
                                               beløp = 14950),
                        lagAndelTilkjentYtelse(fraOgMed = LocalDate.of(2022, 5, 1),
                                               tilOgMed = LocalDate.of(2022, 8, 30),
                                               samordningsfradrag = 5000,
                                               beløp = if (medRiktigBeløp) 15902 else 0
                        )))
    }

    private fun mockVedtakOgForrigeTilkjentYtelse(saksbehandling: Saksbehandling) {
        every { tilkjentYtelseRepository.findByBehandlingId(saksbehandling.forrigeBehandlingId!!) } returns
                lagTilkjentYtelse(emptyList(), grunnbeløpsdato = LocalDate.of(2021, 5, 1))
        every { vedtakService.hentVedtak(saksbehandling.id) } returns vedtak(saksbehandling.id)
    }
}