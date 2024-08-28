package no.nav.familie.ef.sak.forvaltning

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.oppgave.OppgaveRepository
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.ef.sak.oppgave.TilordnetRessursService
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.OppgavePrioritet
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.oppgave.StatusEnum
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class GjennoprettOppgavePåBehandlingTaskTest {
    private val tilordnetRessursService: TilordnetRessursService = mockk<TilordnetRessursService>()
    private val behandligService: BehandlingService = mockk<BehandlingService>()
    private val oppgaveService: OppgaveService = mockk<OppgaveService>(relaxed = true)
    private val oppgaveRepository: OppgaveRepository = mockk<OppgaveRepository>()

    private val gjennoprettOppgavePåBehandlingTask = GjennoprettOppgavePåBehandlingTask(tilordnetRessursService = tilordnetRessursService, behandligService = behandligService, oppgaveService = oppgaveService, oppgaveRepository = oppgaveRepository)

    @Test
    fun `Ikke opprett oppgave hvis behandling er FERDIGSTILT`() {
        val behandling = behandling(status = BehandlingStatus.FERDIGSTILT)
        every { behandligService.hentBehandling(behandling.id) } returns behandling
        val feil =
            org.junit.jupiter.api.assertThrows<Feil> {
                gjennoprettOppgavePåBehandlingTask.doTask(GjennoprettOppgavePåBehandlingTask.opprettTask(behandling.id))
            }
        assertThat(feil.message).isEqualTo("Behandling er ferdig behandlet")
    }

    @Test
    fun `Ikke opprett oppgave hvis behandling er IVERKSETTER_VEDTAK`() {
        val behandling = behandling(status = BehandlingStatus.IVERKSETTER_VEDTAK)
        every { behandligService.hentBehandling(behandling.id) } returns behandling

        val feil =
            org.junit.jupiter.api.assertThrows<Feil> {
                gjennoprettOppgavePåBehandlingTask.doTask(GjennoprettOppgavePåBehandlingTask.opprettTask(behandling.id))
            }

        assertThat(feil.message).isEqualTo("Behandling er ferdig behandlet")
    }

    @Test
    fun `Opprett oppgave med henvisning til feilregistrert oppgave dersom dette er status på opprinnelig oppgave `() {
        val forventetBeskrivelse = "Opprinnelig oppgave er feilregistrert. For å kunne utføre behandling har det blitt opprettet en ny oppgave."
        val behandling = behandling()

        every { behandligService.hentBehandling(behandling.id) } returns behandling
        every { oppgaveRepository.findByBehandlingIdAndErFerdigstiltIsFalseAndTypeIn(behandling.id, setOf(Oppgavetype.BehandleSak, Oppgavetype.GodkjenneVedtak, Oppgavetype.BehandleUnderkjentVedtak)) } returns null
        every { tilordnetRessursService.hentIkkeFerdigstiltOppgaveForBehandling(behandling.id, setOf(Oppgavetype.BehandleSak, Oppgavetype.GodkjenneVedtak, Oppgavetype.BehandleUnderkjentVedtak)) } returns Oppgave(status = StatusEnum.FEILREGISTRERT)

        gjennoprettOppgavePåBehandlingTask.doTask(GjennoprettOppgavePåBehandlingTask.opprettTask(behandling.id))

        verify(exactly = 1) {
            oppgaveService.opprettOppgave(
                behandlingId = behandling.id,
                oppgavetype = Oppgavetype.BehandleSak,
                vurderHenvendelseOppgaveSubtype = any(),
                tilordnetNavIdent = any(),
                beskrivelse = forventetBeskrivelse,
                mappeId = any(),
                prioritet = OppgavePrioritet.HOY,
                fristFerdigstillelse = LocalDate.now(),
            )
        }
    }
}
