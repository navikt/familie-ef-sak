package no.nav.familie.ef.sak.økonomi

import io.mockk.every
import io.mockk.mockk
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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class TilkjentYtelseServiceTest {

    private val tilkjentYtelseRepository = mockk<TilkjentYtelseRepository>()
    private val behandlingService = mockk<BehandlingService>()

    private val tilkjentYtelseService = TilkjentYtelseService(behandlingService = behandlingService,
                                                              tilkjentYtelseRepository = tilkjentYtelseRepository)

    private val datoForAvstemming = LocalDate.of(2021, 2, 1)
    private val stønadstype = Stønadstype.OVERGANGSSTØNAD
    private val behandling = behandling(fagsak())

    private val andel1 = lagAndelTilkjentYtelse(1, LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 31))
    private val andel2 = lagAndelTilkjentYtelse(2, LocalDate.of(2021, 2, 1), LocalDate.of(2021, 2, 28))
    private val andel3 = lagAndelTilkjentYtelse(3, LocalDate.of(2021, 3, 1), LocalDate.of(2021, 3, 31))
    private val andel4 = lagAndelTilkjentYtelse(4, LocalDate.of(2021, 1, 1), LocalDate.of(2021, 3, 31))

    @BeforeEach
    internal fun setUp() {
        every { behandlingService.finnSisteIverksatteBehandlinger(any()) } returns setOf(behandling.id)
    }

    @Test
    internal fun `konsistensavstemming - filtrer andeler har tom dato som er lik eller etter dato for konsistensavstemming`() {
        val andelerTilkjentYtelse = listOf(andel1, andel2, andel3, andel4)
        val tilkjentYtelse = DataGenerator.tilfeldigTilkjentYtelse(behandling).copy(andelerTilkjentYtelse = andelerTilkjentYtelse)

        every { behandlingService.hentEksterneIder(setOf(behandling.id)) } returns setOf(EksternId(behandling.id, 1, 1))
        every {
            tilkjentYtelseRepository.finnTilkjentYtelserTilKonsistensavstemming(setOf(behandling.id), any())
        } returns listOf(tilkjentYtelse)

        val tilkjentYtelser = tilkjentYtelseService.finnTilkjentYtelserTilKonsistensavstemming(stønadstype, datoForAvstemming)
        assertThat(tilkjentYtelser).hasSize(1)
        assertThat(tilkjentYtelser[0].andelerTilkjentYtelse).hasSize(3)
        assertThat(tilkjentYtelser[0].andelerTilkjentYtelse.map { it.beløp }).containsExactlyInAnyOrder(2, 3, 4)
    }

    @Test
    internal fun `konsistensavstemming - skal kaste feil hvis den ikke finner eksterneIder til behandling`() {
        val andelTilkjentYtelse = lagAndelTilkjentYtelse(1, LocalDate.of(2021, 1, 1), LocalDate.of(2023, 1, 31))
        val tilkjentYtelse = DataGenerator.tilfeldigTilkjentYtelse(behandling)
                .copy(andelerTilkjentYtelse = listOf(andelTilkjentYtelse))

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