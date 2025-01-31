package no.nav.familie.ef.sak.oppgave

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ef.sak.behandling.oppgaverforferdigstilling.OppgaverForFerdigstillingService
import no.nav.familie.ef.sak.infrastruktur.exception.ManglerTilgang
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.ef.sak.oppgave.OppgaveUtil.ENHET_NR_EGEN_ANSATT
import no.nav.familie.ef.sak.oppgave.OppgaveUtil.ENHET_NR_NAY
import no.nav.familie.ef.sak.oppgave.dto.FinnOppgaveRequestDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdent
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdenter
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveRequest
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveResponseDto
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

internal class OppgaveControllerTest {
    private val tilgangService: TilgangService = mockk()
    private val oppgaveService: OppgaveService = mockk()
    private val personService: PersonService = mockk()
    private val tilordnetRessursService: TilordnetRessursService = mockk()
    private val oppgaverForFerdigstillingService: OppgaverForFerdigstillingService = mockk()

    private val oppgaveController: OppgaveController =
        OppgaveController(oppgaveService, tilgangService, personService, tilordnetRessursService, oppgaverForFerdigstillingService)

    @Test
    internal fun `skal kaste feil hvis ident ikke er på gyldig format`() {
        every { personService.hentAktørIder(any()) } returns PdlIdenter(listOf(PdlIdent("1234", false)))
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
        every { personService.hentAktørIder("12345678901") } returns PdlIdenter(listOf(PdlIdent("1234", false)))
        every { oppgaveService.hentOppgaver(capture(finnOppgaveRequestSlot)) } returns
            FinnOppgaveResponseDto(
                0,
                listOf(),
            )
        oppgaveController.hentOppgaver(FinnOppgaveRequestDto(ident = "12345678901"))
        assertThat(finnOppgaveRequestSlot.captured.aktørId).isEqualTo("1234")
    }

    @Test
    internal fun `skal sende med versjon i request `() {
        val versjonSlot = slot<Int>()
        val oppgaveIdSlot = slot<Long>()
        tilgangOgRolleJustRuns()
        every { oppgaveService.fordelOppgave(capture(oppgaveIdSlot), any(), capture(versjonSlot)) } returns 123
        oppgaveController.fordelOppgave(123, "saksbehandler", 1)
        assertThat(versjonSlot.captured).isEqualTo(1)
    }

    @Test
    internal fun `skal ikke feile hvis versjon er null `() {
        val versjonSlot = slot<Int>()
        val oppgaveIdSlot = slot<Long>()
        tilgangOgRolleJustRuns()
        every { oppgaveService.fordelOppgave(capture(oppgaveIdSlot), any()) } returns 123
        oppgaveController.fordelOppgave(123, "saksbehandler", versjon = null)
    }

    @Test
    internal fun `skal ikke feile hvis ident er tom`() {
        val finnOppgaveRequestSlot = slot<FinnOppgaveRequest>()
        tilgangOgRolleJustRuns()
        every { oppgaveService.hentOppgaver(capture(finnOppgaveRequestSlot)) } returns
            FinnOppgaveResponseDto(
                0,
                listOf(),
            )
        oppgaveController.hentOppgaver(FinnOppgaveRequestDto(ident = " "))
        verify(exactly = 0) { personService.hentAktørIder(any()) }
        assertThat(finnOppgaveRequestSlot.captured.aktørId).isEqualTo(null)
    }

    @Test
    internal fun `skal ikke feile hvis ident er null`() {
        val finnOppgaveRequestSlot = slot<FinnOppgaveRequest>()
        tilgangOgRolleJustRuns()
        every { oppgaveService.hentOppgaver(capture(finnOppgaveRequestSlot)) } returns
            FinnOppgaveResponseDto(
                0,
                listOf(),
            )
        oppgaveController.hentOppgaver(FinnOppgaveRequestDto(ident = null))
        verify(exactly = 0) { personService.hentAktørIder(any()) }
        assertThat(finnOppgaveRequestSlot.captured.aktørId).isEqualTo(null)
    }

    @Test
    internal fun `skal feile hvis bruker er veileder`() {
        every {
            tilgangService.validerTilgangTilPersonMedBarn(any(), any())
        } just Runs

        every {
            tilgangService.validerHarSaksbehandlerrolle()
        } throws ManglerTilgang("Bruker mangler tilgang", "Mangler tilgang")

        assertThrows<ManglerTilgang> {
            oppgaveController.fordelOppgave(123, "dummy saksbehandler", null)
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

    @Nested
    inner class HentMapper {
        @Test
        internal fun `skal hente mappe for egen ansatt hvis man har riktig rolle`() {
            every { tilgangService.harEgenAnsattRolle() } returns true
            every { oppgaveService.finnMapper(any<List<String>>()) } returns emptyList()

            oppgaveController.hentMapper()

            verify { oppgaveService.finnMapper(listOf(ENHET_NR_NAY, ENHET_NR_EGEN_ANSATT)) }
        }

        @Test
        internal fun `skal hente ikke mappe for egen ansatt hvis man har riktig rolle`() {
            every { tilgangService.harEgenAnsattRolle() } returns false
            every { oppgaveService.finnMapper(any<List<String>>()) } returns emptyList()

            oppgaveController.hentMapper()

            verify { oppgaveService.finnMapper(listOf(ENHET_NR_NAY)) }
        }
    }

    private fun tilgangOgRolleJustRuns() {
        every {
            tilgangService.validerTilgangTilPersonMedBarn(any(), any())
        } just Runs

        every {
            tilgangService.validerHarSaksbehandlerrolle()
        } just Runs

        every {
            tilgangService.validerSaksbehandler(any())
        } returns true
    }
}
