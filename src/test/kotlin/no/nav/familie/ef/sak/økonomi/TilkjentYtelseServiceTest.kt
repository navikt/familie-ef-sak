package no.nav.familie.ef.sak.økonomi

import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ef.sak.integration.OppdragClient
import no.nav.familie.ef.sak.repository.TilkjentYtelseRepository
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.service.BehandlingService
import no.nav.familie.ef.sak.service.FagsakService
import no.nav.familie.ef.sak.service.TilkjentYtelseService
import no.nav.familie.kontrakter.felles.oppdrag.OppdragId
import no.nav.familie.kontrakter.felles.oppdrag.OppdragStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull

class TilkjentYtelseServiceTest {

    private val tilkjentYtelseRepository = mockk<TilkjentYtelseRepository>()
    private val økonomiKlient = mockk<OppdragClient>()
    private val behandlingService  = mockk<BehandlingService>()
    private val fagsakService = mockk<FagsakService>()

    private val tilkjentYtelseService = TilkjentYtelseService(økonomiKlient, behandlingService, fagsakService, tilkjentYtelseRepository)

    @AfterEach
    fun afterEach() {
        confirmVerified(økonomiKlient)
    }

    @Test
    fun `hent tilkjent-ytelse-dto`() {
        val tilkjentYtelse = DataGenerator.tilfeldigTilkjentYtelse(antallAndelerTilkjentYtelse = 3)
        val id = tilkjentYtelse.id
        every { tilkjentYtelseRepository.findByIdOrNull(id) } returns tilkjentYtelse
        every { behandlingService.hentBehandling(any()) } returns behandling
        every { fagsakService.hentFagsak(any()) } returns fagsak
        val dto = tilkjentYtelseService.hentTilkjentYtelseDto(id)

        assertThat(dto.id).isEqualTo(id)
        assertThat(dto.andelerTilkjentYtelse.size).isEqualTo(3)
        (0..2).forEach {
            assertThat(dto.andelerTilkjentYtelse[it].beløp).isEqualTo(tilkjentYtelse.andelerTilkjentYtelse[it].beløp)
        }
        verify { tilkjentYtelseRepository.findByIdOrNull(id) }
    }

    @Test
    fun `hent status fra oppdragstjenesten`() {
        val tilkjentYtelse = DataGenerator.tilfeldigTilkjentYtelse()
        val oppdragId = OppdragId("EFOG",
                                  tilkjentYtelse.personident,
                                  behandling.eksternId.id.toString())
        every { tilkjentYtelseRepository.findByBehandlingId(behandling.id) } returns tilkjentYtelse
        every { behandlingService.hentBehandling(any()) } returns behandling
        every { fagsakService.hentFagsak(any()) } returns fagsak
        every { økonomiKlient.hentStatus(oppdragId) } returns OppdragStatus.KVITTERT_OK

        tilkjentYtelseService.hentStatus(behandling)

        verify { økonomiKlient.hentStatus(oppdragId) }
    }

    companion object {
        val fagsak = fagsak()
        val behandling = behandling(fagsak = fagsak)
    }
}