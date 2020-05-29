package no.nav.familie.ef.sak.no.nav.familie.ef.sak.økonomi

import io.mockk.*
import no.nav.familie.ef.sak.repository.CustomRepository
import no.nav.familie.ef.sak.økonomi.*
import no.nav.familie.ef.sak.økonomi.Utbetalingsoppdrag.lagUtbetalingsoppdrag
import no.nav.familie.ef.sak.økonomi.domain.TilkjentYtelse
import no.nav.familie.ef.sak.økonomi.domain.TilkjentYtelseStatus
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.oppdrag.OppdragId
import no.nav.familie.kontrakter.felles.oppdrag.OppdragStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import java.time.LocalDate
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag as UtbetalingsoppdragDto

class TilkjentYtelseServiceTest {

    private val customRepository = mockk<CustomRepository<TilkjentYtelse>>()
    private val tilkjentYtelseRepository = mockk<TilkjentYtelseRepository>()
    private val økonomiKlient = mockk<ØkonomiKlient>()

    private val tilkjentYtelseService =
            TilkjentYtelseService(økonomiKlient, tilkjentYtelseRepository, customRepository)

    @BeforeEach
    fun beforeEach() {
        mockkObject(Utbetalingsoppdrag)
    }

    @AfterEach
    fun afterEach() {
        confirmVerified(tilkjentYtelseRepository, økonomiKlient, customRepository)
    }

    @Test
    fun `opprett tilkjent ytelse`() {
        val tilkjentYtelseDto = DataGenerator.tilfeldigTilkjentYtelseDto()
        val tilkjentYtelse = tilkjentYtelseDto.tilTilkjentYtelse(TilkjentYtelseStatus.OPPRETTET)
        val slot = slot<TilkjentYtelse>()
        every { tilkjentYtelseRepository.findByPersonident(tilkjentYtelse.personident) } returns null
        every { customRepository.persist(capture(slot)) } returns tilkjentYtelse

        tilkjentYtelseService.opprettTilkjentYtelse(tilkjentYtelseDto)

        verify { tilkjentYtelseRepository.findByPersonident(tilkjentYtelse.personident) }
        verify { customRepository.persist(slot.captured)}
        assertThat(slot.captured).isEqualToIgnoringGivenFields(tilkjentYtelse, "id")

    }

    @Test
    fun `hent tilkjent-ytelse-dto`() {
        val tilkjentYtelse = DataGenerator.tilfeldigTilkjentYtelse(3)
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
        val tilkjentYtelse = DataGenerator.tilfeldigTilkjentYtelse()
        val id = tilkjentYtelse.id
        val oppdragId = OppdragId("EF",
                                  tilkjentYtelse.personident,
                                  tilkjentYtelse.id.toString())
        every { tilkjentYtelseRepository.findByIdOrNull(id) } returns tilkjentYtelse
        every { økonomiKlient.hentStatus(oppdragId) } returns Ressurs.success(OppdragStatus.KVITTERT_OK)

        tilkjentYtelseService.hentStatus(id)

        verify { tilkjentYtelseRepository.findByIdOrNull(id) }
        verify { økonomiKlient.hentStatus(oppdragId) }
    }

    @Test
    fun `iverksett utbetalingsoppdrag`() {
        val tilkjentYtelse = DataGenerator.tilfeldigTilkjentYtelse(3)
                .copy(status = TilkjentYtelseStatus.OPPRETTET)
        val id = tilkjentYtelse.id
        val utbetalingsoppdrag = lagUtbetalingsoppdrag("VL", tilkjentYtelse)
        val ytelseSlot = slot<TilkjentYtelse>()
        val oppdragSlot = slot<UtbetalingsoppdragDto>()
        val oppdatertTilkjentYtelse =
                tilkjentYtelse.copy(status = TilkjentYtelseStatus.SENDT_TIL_IVERKSETTING,
                                    utbetalingsoppdrag = utbetalingsoppdrag)
        every { tilkjentYtelseRepository.findByIdOrNull(id) } returns tilkjentYtelse
        every { økonomiKlient.iverksettOppdrag(capture(oppdragSlot)) } returns Ressurs.success("")
        every { tilkjentYtelseRepository.save(capture(ytelseSlot)) } returns oppdatertTilkjentYtelse

        tilkjentYtelseService.iverksettUtbetalingsoppdrag(id)

        verify { tilkjentYtelseRepository.findByIdOrNull(id) }
        verify { økonomiKlient.iverksettOppdrag(oppdragSlot.captured) }
        verify { tilkjentYtelseRepository.save(ytelseSlot.captured) }
        assertThat(ytelseSlot.captured).isEqualToIgnoringGivenFields(oppdatertTilkjentYtelse, "utbetalingsoppdrag")
        assertThat(ytelseSlot.captured.utbetalingsoppdrag).isEqualToIgnoringGivenFields(utbetalingsoppdrag, "avstemmingTidspunkt")
        assertThat(oppdragSlot.captured).isEqualToIgnoringGivenFields(utbetalingsoppdrag, "avstemmingTidspunkt")
    }

    @Test
    fun `opphør aktiv tilkjent ytelse`() {
        val opphørDato = LocalDate.now()
        val originalTilkjentYtelse =
                DataGenerator.tilfeldigTilkjentYtelse(3).copy(status = TilkjentYtelseStatus.AKTIV)
        val avsluttetOriginalTilkjentYtelse = originalTilkjentYtelse.copy(status = TilkjentYtelseStatus.AVSLUTTET)
        val opphørtTilkjentYtelse = originalTilkjentYtelse.tilOpphør(opphørDato)
        val id = originalTilkjentYtelse.id
        val utbetalingsoppdrag =
                lagUtbetalingsoppdrag("VL", opphørtTilkjentYtelse)
        val opphørtTilkjentYtelseSendtUtbetalingsoppdrag =
                opphørtTilkjentYtelse.copy(status = TilkjentYtelseStatus.SENDT_TIL_IVERKSETTING,
                                           utbetalingsoppdrag = utbetalingsoppdrag)
        val utbetalingSlot = slot<UtbetalingsoppdragDto>()
        val ytelseSlot = slot<TilkjentYtelse>()
        every { tilkjentYtelseRepository.findByIdOrNull(id) } returns originalTilkjentYtelse
        every { tilkjentYtelseRepository.save(avsluttetOriginalTilkjentYtelse) } returns avsluttetOriginalTilkjentYtelse
        every { customRepository.persist(any<TilkjentYtelse>()) } returns opphørtTilkjentYtelse
        every { økonomiKlient.iverksettOppdrag(capture(utbetalingSlot)) } returns Ressurs.success("")
        every { tilkjentYtelseRepository.save(capture(ytelseSlot)) }
                .returns(opphørtTilkjentYtelseSendtUtbetalingsoppdrag)

        tilkjentYtelseService.opphørUtbetalingsoppdrag(id, opphørDato)

        verify { tilkjentYtelseRepository.findByIdOrNull(id) }
        verify { tilkjentYtelseRepository.save(avsluttetOriginalTilkjentYtelse) }
        verify { customRepository.persist(any<TilkjentYtelse>()) }
        verify { økonomiKlient.iverksettOppdrag(utbetalingSlot.captured) }
        verify { tilkjentYtelseRepository.save(ytelseSlot.captured) }
        assertThat(ytelseSlot.captured).isEqualToIgnoringGivenFields(opphørtTilkjentYtelseSendtUtbetalingsoppdrag, "utbetalingsoppdrag")
        assertThat(ytelseSlot.captured.utbetalingsoppdrag).isEqualToIgnoringGivenFields(utbetalingsoppdrag, "avstemmingTidspunkt")
        assertThat(utbetalingSlot.captured).isEqualToIgnoringGivenFields(utbetalingsoppdrag, "avstemmingTidspunkt")
    }
}