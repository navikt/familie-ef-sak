package no.nav.familie.ef.sak.oppgave

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ef.sak.infrastruktur.exception.ApiFeil
import no.nav.familie.ef.sak.infrastruktur.exception.ManglerTilgang
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.ef.sak.oppgave.dto.FinnOppgaveRequestDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PdlClient
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdent
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdenter
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveRequest
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveResponseDto
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

internal class OppgaveControllerTest {

    private val tilgangService: TilgangService = mockk()
    private val oppgaveService: OppgaveService = mockk()
    private val pdlClient: PdlClient = mockk()


    private val oppgaveController: OppgaveController = OppgaveController(oppgaveService, tilgangService, pdlClient)


    @Test
    internal fun `skal kaste feil hvis ident ikke er på gyldig format`() {
        every { pdlClient.hentAktørIder(any()) } returns PdlIdenter(listOf(PdlIdent("1234", false)))
        every { oppgaveService.hentOppgaver(any()) } returns FinnOppgaveResponseDto(0, listOf())

        oppgaveController.hentOppgaver(FinnOppgaveRequestDto(ident = null))
        oppgaveController.hentOppgaver(FinnOppgaveRequestDto(ident = ""))
        oppgaveController.hentOppgaver(FinnOppgaveRequestDto(ident = "12345678901"))

        listOf("1", "ab", "1234567890", "123456789012").forEach {
            assertThatThrownBy { oppgaveController.hentOppgaver(FinnOppgaveRequestDto(ident = it)) }
        }
    }

    @Test
    internal fun `skal sende med aktoerId i request `() {
        val finnOppgaveRequestSlot = slot<FinnOppgaveRequest>()
        tilgangOgRolleJustRuns()
        every { pdlClient.hentAktørIder("12345678901") } returns PdlIdenter(listOf(PdlIdent("1234", false)))
        every { oppgaveService.hentOppgaver(capture(finnOppgaveRequestSlot)) } returns FinnOppgaveResponseDto(0, listOf())
        oppgaveController.hentOppgaver(FinnOppgaveRequestDto(ident = "12345678901"))
        assertThat(finnOppgaveRequestSlot.captured.aktørId).isEqualTo("1234")
    }

    @Test
    internal fun `skal ikke feile hvis ident er tom`() {
        val finnOppgaveRequestSlot = slot<FinnOppgaveRequest>()
        tilgangOgRolleJustRuns()
        every { oppgaveService.hentOppgaver(capture(finnOppgaveRequestSlot)) } returns FinnOppgaveResponseDto(0, listOf())
        oppgaveController.hentOppgaver(FinnOppgaveRequestDto(ident = " "))
        verify(exactly = 0) { pdlClient.hentAktørIder(any()) }
        assertThat(finnOppgaveRequestSlot.captured.aktørId).isEqualTo(null)
    }

    @Test
    internal fun `skal ikke feile hvis ident er null`() {
        val finnOppgaveRequestSlot = slot<FinnOppgaveRequest>()
        tilgangOgRolleJustRuns()
        every { oppgaveService.hentOppgaver(capture(finnOppgaveRequestSlot)) } returns FinnOppgaveResponseDto(0, listOf())
        oppgaveController.hentOppgaver(FinnOppgaveRequestDto(ident = null))
        verify(exactly = 0) { pdlClient.hentAktørIder(any()) }
        assertThat(finnOppgaveRequestSlot.captured.aktørId).isEqualTo(null)
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
        tilgangOgRolleJustRuns()


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
        tilgangOgRolleJustRuns()


        every {
            oppgaveService.hentEfOppgave(any())
        } returns null

        val returnertOppgaveRessurs = oppgaveController.hentOppgave(123)

        assertThat(returnertOppgaveRessurs.status).isEqualTo(Ressurs.Status.FUNKSJONELL_FEIL)
    }

    private fun tilgangOgRolleJustRuns() {
        every {
            tilgangService.validerTilgangTilPersonMedBarn(any())
        } just Runs

        every {
            tilgangService.validerHarSaksbehandlerrolle()
        } just Runs
    }
}