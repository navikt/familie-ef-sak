package no.nav.familie.ef.sak.forvaltning

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus.FATTER_VEDTAK
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus.FERDIGSTILT
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus.IVERKSETTER_VEDTAK
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus.OPPRETTET
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus.SATT_PÅ_VENT
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus.UTREDES
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.oppgave.OppgaveRepository
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.ef.sak.oppgave.TilordnetRessursService
import no.nav.familie.kontrakter.felles.oppgave.OppgavePrioritet
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.oppgave.StatusEnum.FEILREGISTRERT
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.Properties
import java.util.UUID

typealias EFOppgave = no.nav.familie.ef.sak.oppgave.Oppgave

@Service
@TaskStepBeskrivelse(
    taskStepType = GjennoprettOppgavePåBehandlingTask.TYPE,
    maxAntallFeil = 1,
    settTilManuellOppfølgning = true,
    beskrivelse = "Finn og logg metadata for oppgave knyttet til behandling",
)
class GjennoprettOppgavePåBehandlingTask(
    private val tilordnetRessursService: TilordnetRessursService,
    private val behandligService: BehandlingService,
    private val oppgaveService: OppgaveService,
    private val oppgaveRepository: OppgaveRepository,
) : AsyncTaskStep {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun doTask(task: Task) {
        logger.info("Gjenoppretter oppgave for behandling ${task.payload}")
        val behandling = behandligService.hentBehandling(UUID.fromString(task.payload))
        feilHvis(behandling.status.erFerdigbehandlet()) { "Behandling er ferdig behandlet" }
        ferdigstillReferanseTilIkkeeksisterendeEksternOppgave(behandling)
        opprettNyOppgave(behandling)
    }

    private fun ferdigstillReferanseTilIkkeeksisterendeEksternOppgave(behandling: Behandling) {
        val efOppgave = oppgaveRepository.findByBehandlingIdAndErFerdigstiltIsFalseAndTypeIn(behandling.id, setOf(Oppgavetype.BehandleSak, Oppgavetype.GodkjenneVedtak, Oppgavetype.BehandleUnderkjentVedtak))
        if (efOppgave != null) {
            ferdigstillGammelOppgave(efOppgave)
        }
    }

    private fun ferdigstillGammelOppgave(efOppgave: EFOppgave) {
        oppgaveRepository.update(efOppgave.copy(erFerdigstilt = true))
    }

    private fun opprettNyOppgave(behandling: Behandling) {
        val erFeilregistrert = erFeilregistrert(behandling)
        val beskrivelse: String =
            when (erFeilregistrert) {
                true -> "Opprinnelig oppgave er feilregistrert. For å kunne utføre behandling har det blitt opprettet en ny oppgave."
                false -> "Opprinnelig oppgave har blitt flyttet eller endret. For å kunne utføre behandling har det blitt opprettet en ny oppgave."
            }
        oppgaveService.opprettOppgave(behandling.id, finnOppgavetype(behandling), prioritet = OppgavePrioritet.HOY, beskrivelse = beskrivelse, fristFerdigstillelse = LocalDate.now())
    }

    private fun erFeilregistrert(behandling: Behandling): Boolean {
        val opprinneligOppgave = tilordnetRessursService.hentIkkeFerdigstiltOppgaveForBehandling(behandling.id, setOf(Oppgavetype.BehandleSak, Oppgavetype.GodkjenneVedtak, Oppgavetype.BehandleUnderkjentVedtak))
        return opprinneligOppgave?.status == FEILREGISTRERT
    }

    private fun finnOppgavetype(behandling: Behandling) =
        when (behandling.status) {
            FATTER_VEDTAK -> Oppgavetype.GodkjenneVedtak
            else -> Oppgavetype.BehandleSak
        }

    companion object {
        const val TYPE = "GjennoprettOppgavePåBehandlingForvaltningsTask"

        fun opprettTask(behandlingId: UUID): Task =
            Task(
                type = TYPE,
                payload = behandlingId.toString(),
                properties = Properties(),
            )
    }

    private fun BehandlingStatus.erFerdigbehandlet(): Boolean =
        when (this) {
            FERDIGSTILT, IVERKSETTER_VEDTAK -> true
            FATTER_VEDTAK, UTREDES, SATT_PÅ_VENT, OPPRETTET -> false
        }
}
