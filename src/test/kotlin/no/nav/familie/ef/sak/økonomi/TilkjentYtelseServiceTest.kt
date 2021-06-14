package no.nav.familie.ef.sak.økonomi

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.økonomi.lagAndelTilkjentYtelse
import no.nav.familie.ef.sak.repository.TilkjentYtelseRepository
import no.nav.familie.ef.sak.repository.domain.EksternId
import no.nav.familie.ef.sak.repository.domain.Stønadstype
import no.nav.familie.ef.sak.service.BehandlingService
import no.nav.familie.ef.sak.service.TilkjentYtelseService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import java.time.LocalDate

class TilkjentYtelseServiceTest {

    private val tilkjentYtelseRepository = mockk<TilkjentYtelseRepository>()
    private val behandlingService = mockk<BehandlingService>()

    private val tilkjentYtelseService = TilkjentYtelseService(behandlingService = behandlingService,
                                                              tilkjentYtelseRepository = tilkjentYtelseRepository)

    @Test
    fun `hent tilkjent-ytelse-dto`() {
        val tilkjentYtelse = DataGenerator.tilfeldigTilkjentYtelse(antallAndelerTilkjentYtelse = 3)
        val id = tilkjentYtelse.id
        every { tilkjentYtelseRepository.findByIdOrNull(id) } returns tilkjentYtelse
        every { behandlingService.hentBehandling(any()) } returns behandling
        val dto = tilkjentYtelseService.hentTilkjentYtelseDto(id)

        assertThat(dto.id).isEqualTo(id)
        assertThat(dto.andelerTilkjentYtelse.size).isEqualTo(3)
        (0..2).forEach {
            assertThat(dto.andelerTilkjentYtelse[it].beløp).isEqualTo(tilkjentYtelse.andelerTilkjentYtelse[it].beløp)
        }
        verify { tilkjentYtelseRepository.findByIdOrNull(id) }
    }

    @Test
    internal fun `konsistensavstemming - filtrer andeler har tom dato som er etter`() {
        val datoForAvstemming = LocalDate.of(2021, 2, 1)
        val stønadstype = Stønadstype.OVERGANGSSTØNAD
        val behandling = behandling(fagsak())
        val andelTilkjentYtelse = lagAndelTilkjentYtelse(1, LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 31))
        val andelTilkjentYtelse2 = lagAndelTilkjentYtelse(2, LocalDate.of(2021, 2, 1), LocalDate.of(2021, 2, 28))
        val andelTilkjentYtelse3 = lagAndelTilkjentYtelse(3, LocalDate.of(2021, 3, 1), LocalDate.of(2021, 3, 31))
        val tilkjentYtelse = DataGenerator.tilfeldigTilkjentYtelse(behandling)
                .copy(andelerTilkjentYtelse = listOf(andelTilkjentYtelse, andelTilkjentYtelse2, andelTilkjentYtelse3))

        every { behandlingService.finnSisteIverksatteBehandlinger(any()) } returns setOf(behandling.id)
        every { behandlingService.hentEksterneIder(setOf(behandling.id)) } returns setOf(EksternId(behandling.id, 1, 1))
        every {
            tilkjentYtelseRepository.finnTilkjentYtelserTilKonsistensavstemming(setOf(behandling.id), any())
        } returns listOf(tilkjentYtelse)

        val tilkjentYtelser = tilkjentYtelseService.finnTilkjentYtelserTilKonsistensavstemming(stønadstype, datoForAvstemming)
        assertThat(tilkjentYtelser).hasSize(1)
        assertThat(tilkjentYtelser[0].andelerTilkjentYtelse).hasSize(2)
        assertThat(tilkjentYtelser[0].andelerTilkjentYtelse.map { it.beløp }).containsExactlyInAnyOrder(2, 3)
    }

    @Test
    internal fun `konsistensavstemming - skal kaste feil hvis den ikke finner eksterneIder til behandling`() {
        val datoForAvstemming = LocalDate.of(2021, 2, 1)
        val behandling = behandling(fagsak())
        val andelTilkjentYtelse = lagAndelTilkjentYtelse(1, LocalDate.of(2021, 1, 1), LocalDate.of(2023, 1, 31))
        val tilkjentYtelse = DataGenerator.tilfeldigTilkjentYtelse(behandling)
                .copy(andelerTilkjentYtelse = listOf(andelTilkjentYtelse))

        every { behandlingService.finnSisteIverksatteBehandlinger(any()) } returns setOf(behandling.id)
        every { behandlingService.hentEksterneIder(any()) } returns emptySet()
        every {
            tilkjentYtelseRepository.finnTilkjentYtelserTilKonsistensavstemming(setOf(behandling.id), any())
        } returns listOf(tilkjentYtelse)

        assertThat(catchThrowable {
            tilkjentYtelseService.finnTilkjentYtelserTilKonsistensavstemming(Stønadstype.OVERGANGSSTØNAD,
                                                                             datoForAvstemming)
        }).hasMessageContaining(behandling.id.toString())
    }

    companion object {

        val fagsak = fagsak()
        val behandling = behandling(fagsak = fagsak)
    }
}