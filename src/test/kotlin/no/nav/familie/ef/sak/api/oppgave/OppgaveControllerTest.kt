package no.nav.familie.ef.sak.api.oppgave

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import no.nav.familie.ef.sak.api.ManglerTilgang
import no.nav.familie.ef.sak.service.OppgaveService
import no.nav.familie.ef.sak.service.TilgangService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class OppgaveControllerTest {

    val tilgangService: TilgangService = mockk()
    val oppgaveService: OppgaveService = mockk()

    val oppgaveController: OppgaveController = OppgaveController(oppgaveService, tilgangService)

    @Test
    internal fun `skal feile hvis bruker er veileder`() {
        every {
            tilgangService.validerTilgangTilPersonMedBarn(any())
        } just Runs

        every {
            tilgangService.validerTilgangTilRolle(any())
        } throws ManglerTilgang("Bruker mangler tilgang")

        assertThrows<ManglerTilgang> {
            oppgaveController.fordelOppgave(123, "dummy saksbehandler")
        }
    }
}