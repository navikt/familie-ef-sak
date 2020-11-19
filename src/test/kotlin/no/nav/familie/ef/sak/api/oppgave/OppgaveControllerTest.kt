package no.nav.familie.ef.sak.api.oppgave

import io.mockk.*
import no.nav.familie.ef.sak.api.ManglerTilgang
import no.nav.familie.ef.sak.integration.PdlClient
import no.nav.familie.ef.sak.integration.dto.pdl.PdlAktørId
import no.nav.familie.ef.sak.integration.dto.pdl.PdlHentIdenter
import no.nav.familie.ef.sak.integration.dto.pdl.PdlIdent
import no.nav.familie.ef.sak.repository.domain.Oppgave
import no.nav.familie.ef.sak.service.OppgaveService
import no.nav.familie.ef.sak.service.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveRequest
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveResponseDto
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.*

internal class OppgaveControllerTest {

    private val tilgangService: TilgangService = mockk()
    private val oppgaveService: OppgaveService = mockk()
    private val pdlClient: PdlClient = mockk()


    private val oppgaveController: OppgaveController = OppgaveController(oppgaveService, tilgangService, pdlClient)

    @Test
    internal fun `skal sende med aktoerId i request `() {
        val finnOppgaveRequestSlot = slot<FinnOppgaveRequest>()

        every {
            tilgangService.validerTilgangTilPersonMedBarn(any())
        } just Runs

        every {
            tilgangService.validerHarSaksbehandlerrolle()
        } just Runs

        every { pdlClient.hentAktørId("4321") } returns PdlHentIdenter(PdlAktørId(listOf(PdlIdent("1234"))))

        every { oppgaveService.hentOppgaver(capture(finnOppgaveRequestSlot)) } returns FinnOppgaveResponseDto(0, listOf())
        oppgaveController.hentOppgaver(FinnOppgaveRequestDto(ident = "4321"))

        assertThat(finnOppgaveRequestSlot.captured.aktoerId).isEqualTo("1234")
    }

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


        val oppgave = Oppgave(UUID.randomUUID(), UUID.randomUUID(), 123, Oppgavetype.BehandleSak)

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