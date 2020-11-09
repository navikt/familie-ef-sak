package no.nav.familie.ef.sak.api.oppgave

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import no.nav.familie.ef.sak.api.ManglerTilgang
import no.nav.familie.ef.sak.repository.domain.Oppgave
import no.nav.familie.ef.sak.service.OppgaveService
import no.nav.familie.ef.sak.service.TilgangService
import no.nav.familie.ef.sak.util.RessursUtils
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.*
import kotlin.test.assertEquals

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
            tilgangService.validerHarSaksbehandlerrolle()
        } throws ManglerTilgang("Bruker mangler tilgang")

        assertThrows<ManglerTilgang> {
            oppgaveController.fordelOppgave(123, "dummy saksbehandler")
        }
    }

    @Test
    internal fun `skal hente oppgave`() {
        every {
            tilgangService.validerTilgangTilPersonMedBarn(any())
        } just Runs

        every {
            tilgangService.validerHarSaksbehandlerrolle()
        } just Runs


        val oppgave: Oppgave = Oppgave(UUID.randomUUID(), UUID.randomUUID(), 123, Oppgavetype.BehandleSak)

        every {
            oppgaveService.hentEfOppgave(any())
        } returns oppgave

        val returnertOppgaveRessurs = oppgaveController.hentOppgave(123)

        assertThat(returnertOppgaveRessurs.status).isEqualTo(Ressurs.Status.SUKSESS)
        assertThat(returnertOppgaveRessurs.data!!.behandlingId).isEqualTo(oppgave.behandlingId)
        assertThat(returnertOppgaveRessurs.data!!.gsakOppgaveId).isEqualTo(oppgave.gsakOppgaveId)
    }

    @Test
    internal fun `skal returnere funksjonell feil når oppgave ikke finnes hos oss`() {
        every {
            tilgangService.validerTilgangTilPersonMedBarn(any())
        } just Runs

        every {
            tilgangService.validerHarSaksbehandlerrolle()
        } just Runs


        every {
            oppgaveService.hentEfOppgave(any())
        } returns null

        val returnertOppgaveRessurs = oppgaveController.hentOppgave(123)

        assertThat(returnertOppgaveRessurs.status).isEqualTo(Ressurs.Status.FUNKSJONELL_FEIL)
    }
}