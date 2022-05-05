package no.nav.familie.ef.sak.avstemming

import io.mockk.every
import io.mockk.mockk
import io.mockk.verifyOrder
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.kontrakter.ef.iverksett.KonsistensavstemmingTilkjentYtelseDto
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

internal class AvstemmingServiceTest {

    private val iverksettClient = mockk<IverksettClient>(relaxed = true)
    private val tilkjentYtelseService = mockk<TilkjentYtelseService>()

    private val service = AvstemmingService(iverksettClient, tilkjentYtelseService)

    @Test
    internal fun `skal sende start og slutt melding før og etter avstemminger`() {
        every { tilkjentYtelseService.finnTilkjentYtelserTilKonsistensavstemming(any(), any()) } returns
                listOf(KonsistensavstemmingTilkjentYtelseDto(UUID.randomUUID(),
                                                             1,
                                                             1L,
                                                             "1", listOf()))
        service.konsistensavstemOppdrag(StønadType.OVERGANGSSTØNAD, LocalDate.now())

        verifyOrder {
            iverksettClient.sendStartmeldingKonsistensavstemming(any(), any())
            iverksettClient.sendKonsistensavstemming(any(), any())
            iverksettClient.sendSluttmeldingKonsistensavstemming(any(), any())
        }
    }
}