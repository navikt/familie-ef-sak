package no.nav.familie.ef.sak.ekstern.journalføring

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.kontrakter.ef.journalføring.AutomatiskJournalføringRequest
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.ef.StønadType.OVERGANGSSTØNAD
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus

internal class AutomatiskJournalføringControllerTest {
    val automatiskJournalføringService = mockk<AutomatiskJournalføringService>(relaxed = true)
    val automatiskJournalføringController = AutomatiskJournalføringController(automatiskJournalføringService)

    val request =
        AutomatiskJournalføringRequest(
            "12345678901",
            "1234",
            OVERGANGSSTØNAD,
            1234L,
        )

    @BeforeEach
    internal fun setUp() {
        mockkObject(SikkerhetContext)
    }

    @AfterEach
    internal fun tearDown() {
        unmockkObject(SikkerhetContext)
    }

    @Test
    internal fun `skal feile hvis det er en annen applikasjon enn familie-ef-mottak som kaller på automatisk journalføring`() {
        every { SikkerhetContext.kallKommerFraFamilieEfMottak() } returns false
        val feil = assertThrows<Feil> { automatiskJournalføringController.automatiskJournalfør(request) }

        assertThat(feil.httpStatus).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    internal fun `skal feile hvis en annen applikasjon enn familie-ef-mottak kaller på sjekk om behandling kan opprettes`() {
        every { SikkerhetContext.kallKommerFraFamilieEfMottak() } returns false
        val feil =
            assertThrows<Feil> {
                automatiskJournalføringController.kanOppretteBehandling(PersonIdent("12345678901"), OVERGANGSSTØNAD)
            }

        assertThat(feil.httpStatus).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    internal fun `skal automatisk journalføre hvis kallet kommer fra familie-ef-mottak`() {
        every { SikkerhetContext.kallKommerFraFamilieEfMottak() } returns true
        automatiskJournalføringController.automatiskJournalfør(request)

        verify { automatiskJournalføringService.automatiskJournalførTilBehandling(any(), any(), any(), any(), any()) }
    }

    @Test
    internal fun `skal kunne sjekke om førstegangsbehandling kan opprettes hvis kallet kommer fra familie-ef-mottak`() {
        every { SikkerhetContext.kallKommerFraFamilieEfMottak() } returns true
        automatiskJournalføringController.kanOppretteBehandling(PersonIdent("12345678901"), OVERGANGSSTØNAD)
        verify { automatiskJournalføringService.kanOppretteBehandling(any(), any()) }
    }
}
