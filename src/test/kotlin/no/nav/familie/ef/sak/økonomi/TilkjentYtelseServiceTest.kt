package no.nav.familie.ef.sak.no.nav.familie.ef.sak.økonomi

import io.mockk.*
import no.nav.familie.ef.sak.økonomi.*
import no.nav.familie.ef.sak.økonomi.Utbetalingsoppdrag.finnAvstemmingTidspunkt
import no.nav.familie.ef.sak.økonomi.Utbetalingsoppdrag.lagUtbetalingsoppdrag
import no.nav.familie.ef.sak.økonomi.domain.TilkjentYtelse
import no.nav.familie.ef.sak.økonomi.domain.TilkjentYtelseStatus
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.oppdrag.OppdragId
import no.nav.familie.kontrakter.felles.oppdrag.OppdragStatus
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.ResponseEntity
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertEquals

class TilkjentYtelseServiceTest {

    private val tilkjentYtelseRepository = mockk<TilkjentYtelseRepository>()
    private val andelTilkjentYtelseRepository = mockk<AndelTilkjentYtelseRepository>()
    private val økonomiKlient = mockk<ØkonomiKlient>()

    private val tilkjentYtelseService = TilkjentYtelseService(økonomiKlient,tilkjentYtelseRepository,andelTilkjentYtelseRepository)

    @BeforeEach
    fun beforeEach() {
        mockkObject(Utbetalingsoppdrag)
        every { finnAvstemmingTidspunkt() } returns LocalDateTime.MIN
    }

    @AfterEach
    fun afterEach() {
        confirmVerified(tilkjentYtelseRepository, andelTilkjentYtelseRepository, økonomiKlient)
    }

    @Test
    fun `opprett tilkjent ytelse`() {
        val tilkjentYtelseDto = DataGenerator.tilfeldigTilkjentYtelseDto()

        val tilkjentYtelse = tilkjentYtelseDto.tilTilkjentYtelse(TilkjentYtelseStatus.OPPRETTET)
        val andelerTilkjentYtelse = tilkjentYtelseDto.tilAndelerTilkjentYtelse(tilkjentYtelse.id)

        every { tilkjentYtelseRepository.findByPersonIdentifikatorOrNull(tilkjentYtelse.personIdentifikator) } returns null
        every { tilkjentYtelseRepository.save(tilkjentYtelse) } returns tilkjentYtelse
        every { andelTilkjentYtelseRepository.saveAll(andelerTilkjentYtelse) } returns andelerTilkjentYtelse

        tilkjentYtelseService.opprettTilkjentYtelse(tilkjentYtelseDto)

        verify { tilkjentYtelseRepository.findByPersonIdentifikatorOrNull(tilkjentYtelse.personIdentifikator) }
        verify { tilkjentYtelseRepository.save(tilkjentYtelse) }
        verify { andelTilkjentYtelseRepository.saveAll(andelerTilkjentYtelse) }
    }

    @Test
    fun `hent tilkjent-ytelse-dto`() {
        val tilkjentYtelse = DataGenerator.tilfeldigTilkjentYtelse().copy(id=1)
        val andelerTilkjentYtelse = DataGenerator.flereTilfeldigeAndelerTilkjentYtelse(tilkjentYtelse.id,3)
        val eksternId = tilkjentYtelse.eksternId

        every { tilkjentYtelseRepository.findByEksternIdOrNull(eksternId) } returns tilkjentYtelse
        every { andelTilkjentYtelseRepository.findByTilkjentYtelseId(tilkjentYtelse.id) } returns andelerTilkjentYtelse

        val dto = tilkjentYtelseService.hentTilkjentYtelseDto(eksternId)
        assertEquals(eksternId, dto.eksternId)
        assertEquals(3, dto.andelerTilkjentYtelse.size)
        assertEquals(andelerTilkjentYtelse[2].beløp, dto.andelerTilkjentYtelse[2].beløp)

        verify { tilkjentYtelseRepository.findByEksternIdOrNull(eksternId) }
        verify { andelTilkjentYtelseRepository.findByTilkjentYtelseId(tilkjentYtelse.id) }
    }

    @Test
    fun `hent status fra oppdragstjenesten`() {

        val tilkjentYtelse = DataGenerator.tilfeldigTilkjentYtelse().copy(id=1)
        val eksternId = tilkjentYtelse.eksternId

        val oppdragId = OppdragId(
                 "EF",
                 tilkjentYtelse.personIdentifikator,
                 tilkjentYtelse.id.toString()
         )

        every { tilkjentYtelseRepository.findByEksternIdOrNull(eksternId) } returns tilkjentYtelse
        every { økonomiKlient.hentStatus(oppdragId) } returns Ressurs.success(OppdragStatus.KVITTERT_OK)

        tilkjentYtelseService.hentStatus(eksternId)

        verify { tilkjentYtelseRepository.findByEksternIdOrNull(eksternId) }
        verify { økonomiKlient.hentStatus(oppdragId) }
    }

    @Test
    fun `iverksett utbetalingsoppdrag`() {

        val tilkjentYtelse = DataGenerator.tilfeldigTilkjentYtelse().copy(
                id=1,
                status = TilkjentYtelseStatus.OPPRETTET
        )

        val eksternId = tilkjentYtelse.eksternId

        val andelerTilkjentYtelse = DataGenerator.flereTilfeldigeAndelerTilkjentYtelse(tilkjentYtelse.id,3)
        val utbetalingsoppdrag = lagUtbetalingsoppdrag("VL",tilkjentYtelse,andelerTilkjentYtelse)
        val oppdatertTilkjentYtelse = tilkjentYtelse.copy(
                status = TilkjentYtelseStatus.SENDT_TIL_IVERKSETTING,
                utbetalingsoppdrag = utbetalingsoppdrag
        )

        every { tilkjentYtelseRepository.findByEksternIdOrNull(eksternId) } returns tilkjentYtelse
        every { andelTilkjentYtelseRepository.findByTilkjentYtelseId(tilkjentYtelse.id) } returns andelerTilkjentYtelse
        every { økonomiKlient.iverksettOppdrag(utbetalingsoppdrag) } returns Ressurs.success("")
        every { tilkjentYtelseRepository.save(oppdatertTilkjentYtelse) } returns oppdatertTilkjentYtelse

        tilkjentYtelseService.iverksettUtbetalingsoppdrag(eksternId)

        verify { tilkjentYtelseRepository.findByEksternIdOrNull(eksternId) }
        verify { andelTilkjentYtelseRepository.findByTilkjentYtelseId(tilkjentYtelse.id) }
        verify { økonomiKlient.iverksettOppdrag(utbetalingsoppdrag) }
        verify { tilkjentYtelseRepository.save(oppdatertTilkjentYtelse) }
    }

    @Test
    fun `opphør aktiv tilkjent ytelse`() {
        val opphørDato = LocalDate.now()
        val vedtaksdato = LocalDate.now()

        val originalTilkjentYtelse = DataGenerator.tilfeldigTilkjentYtelse().copy(
                id=1,
                status = TilkjentYtelseStatus.AKTIV
        )

        val avsluttetOriginalTilkjentYtelse = originalTilkjentYtelse.copy(
                status = TilkjentYtelseStatus.AVSLUTTET
        )

        val opphørtTilkjentYtelse = originalTilkjentYtelse.tilOpphør(opphørDato)

        val eksternId = originalTilkjentYtelse.eksternId

        val andelerTilkjentYtelse = DataGenerator.flereTilfeldigeAndelerTilkjentYtelse(originalTilkjentYtelse.id,3)
        val utbetalingsoppdrag = lagUtbetalingsoppdrag("VL",opphørtTilkjentYtelse,andelerTilkjentYtelse)
        val opphørtTilkjentYtelseSendtUtbetalingsoppdrag = opphørtTilkjentYtelse.copy(
                status = TilkjentYtelseStatus.SENDT_TIL_IVERKSETTING,
                utbetalingsoppdrag = utbetalingsoppdrag
        )

        every { tilkjentYtelseRepository.findByEksternIdOrNull(eksternId) } returns originalTilkjentYtelse
        every { tilkjentYtelseRepository.save(avsluttetOriginalTilkjentYtelse) } returns avsluttetOriginalTilkjentYtelse
        every { tilkjentYtelseRepository.save(any<TilkjentYtelse>()) } returns opphørtTilkjentYtelse

        every { andelTilkjentYtelseRepository.findByTilkjentYtelseId(originalTilkjentYtelse.id) } returns andelerTilkjentYtelse
        every { økonomiKlient.iverksettOppdrag(utbetalingsoppdrag) } returns Ressurs.success("")
        every { tilkjentYtelseRepository.save(opphørtTilkjentYtelseSendtUtbetalingsoppdrag) } returns opphørtTilkjentYtelseSendtUtbetalingsoppdrag

        tilkjentYtelseService.opphørUtbetalingsoppdrag(eksternId, opphørDato)

        verify { tilkjentYtelseRepository.findByEksternIdOrNull(eksternId) }
        verify { tilkjentYtelseRepository.save(avsluttetOriginalTilkjentYtelse) }
        verify { tilkjentYtelseRepository.save(any<TilkjentYtelse>()) }
        verify { andelTilkjentYtelseRepository.findByTilkjentYtelseId(originalTilkjentYtelse.id) }
        verify { økonomiKlient.iverksettOppdrag(utbetalingsoppdrag) }
        verify { tilkjentYtelseRepository.save(opphørtTilkjentYtelseSendtUtbetalingsoppdrag) }
    }
}