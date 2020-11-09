package no.nav.familie.ef.sak.økonomi

import io.mockk.*
import no.nav.familie.ef.sak.integration.ØkonomiKlient
import no.nav.familie.ef.sak.mapper.tilOpphør
import no.nav.familie.ef.sak.mapper.tilTilkjentYtelse
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.TilkjentYtelseRepository
import no.nav.familie.ef.sak.repository.domain.*
import no.nav.familie.ef.sak.service.BehandlingService
import no.nav.familie.ef.sak.service.FagsakService
import no.nav.familie.ef.sak.service.TilkjentYtelseService
import no.nav.familie.ef.sak.økonomi.UtbetalingsoppdragGenerator.lagTilkjentYtelseMedUtbetalingsoppdrag
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.oppdrag.OppdragId
import no.nav.familie.kontrakter.felles.oppdrag.OppdragStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import java.time.LocalDate
import java.util.*
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag as UtbetalingsoppdragDto

class TilkjentYtelseServiceTest {

    private val tilkjentYtelseRepository = mockk<TilkjentYtelseRepository>()
    private val økonomiKlient = mockk<ØkonomiKlient>()

    private val tilkjentYtelseService = TilkjentYtelseService(økonomiKlient, tilkjentYtelseRepository)

    @AfterEach
    fun afterEach() {
        confirmVerified(tilkjentYtelseRepository, økonomiKlient)
    }

    @Test
    fun `hent tilkjent-ytelse-dto`() {
        val tilkjentYtelse = DataGenerator.tilfeldigTilkjentYtelse(3, UUID.randomUUID())
        val id = tilkjentYtelse.id
        every { tilkjentYtelseRepository.findByIdOrNull(id) } returns tilkjentYtelse

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
        val tilkjentYtelse = DataGenerator.tilfeldigTilkjentYtelse(behandlingId = UUID.randomUUID())
        val id = tilkjentYtelse.id
        val oppdragId = OppdragId("EFOG",
                                  tilkjentYtelse.personident,
                                  tilkjentYtelse.id.toString())
        every { tilkjentYtelseRepository.findByIdOrNull(id) } returns tilkjentYtelse
        every { økonomiKlient.hentStatus(oppdragId) } returns Ressurs.success(OppdragStatus.KVITTERT_OK)

        tilkjentYtelseService.hentStatus(id)

        verify { tilkjentYtelseRepository.findByIdOrNull(id) }
        verify { økonomiKlient.hentStatus(oppdragId) }
    }
}