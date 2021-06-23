package no.nav.familie.ef.sak.service.steg

import no.nav.familie.ef.sak.api.Feil
import no.nav.familie.ef.sak.api.beregning.VedtakService
import no.nav.familie.ef.sak.api.dto.BeslutteVedtakDto
import no.nav.familie.ef.sak.blankett.JournalførBlankettTask
import no.nav.familie.ef.sak.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.ef.sak.mapper.IverksettingDtoMapper
import no.nav.familie.ef.sak.repository.VedtaksbrevRepository
import no.nav.familie.ef.sak.repository.domain.Behandling
import no.nav.familie.ef.sak.repository.domain.BehandlingType
import no.nav.familie.ef.sak.repository.domain.Fil
import no.nav.familie.ef.sak.repository.domain.Vedtaksbrev
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.ef.sak.service.FagsakService
import no.nav.familie.ef.sak.service.OppgaveService
import no.nav.familie.ef.sak.service.TotrinnskontrollService
import no.nav.familie.ef.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.task.BehandlingsstatistikkTask
import no.nav.familie.ef.sak.task.FerdigstillOppgaveTask
import no.nav.familie.ef.sak.task.OpprettOppgaveTask
import no.nav.familie.ef.sak.task.OpprettOppgaveTask.OpprettOppgaveTaskData
import no.nav.familie.ef.sak.task.PollStatusFraIverksettTask
import no.nav.familie.kontrakter.ef.iverksett.Hendelse
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
class BeslutteVedtakSteg(private val taskRepository: TaskRepository,
                         private val fagsakService: FagsakService,
                         private val oppgaveService: OppgaveService,
                         private val featureToggleService: FeatureToggleService,
                         private val iverksettClient: IverksettClient,
                         private val iverksettingDtoMapper: IverksettingDtoMapper,
                         private val totrinnskontrollService: TotrinnskontrollService,
                         private val vedtakService: VedtakService,
                         private val vedtaksbrevRepository: VedtaksbrevRepository) : BehandlingSteg<BeslutteVedtakDto> {

    override fun validerSteg(behandling: Behandling) {
        if (behandling.steg != stegType()) {
            throw Feil("Behandling er i feil steg=${behandling.steg}")
        }

    }

    override fun utførOgReturnerNesteSteg(behandling: Behandling, data: BeslutteVedtakDto): StegType {
        val saksbehandler = totrinnskontrollService.lagreTotrinnskontrollOgReturnerBehandler(behandling, data)
        val beslutter = SikkerhetContext.hentSaksbehandler(strict = true)

        val oppgaveId = ferdigstillOppgave(behandling)

        return if (data.godkjent) {
            if (behandling.type != BehandlingType.BLANKETT) {
                val vedtaksbrev = vedtaksbrevRepository.findByIdOrThrow(behandling.id)
                val fil = utledVedtaksbrev(vedtaksbrev)
                val iverksettDto = iverksettingDtoMapper.tilDto(behandling, beslutter)
                iverksettClient.iverksett(iverksettDto, fil)
                opprettPollForStatusOppgave(behandling.id)
                opprettTaskForBehandlingsstatistikk(behandling.id, oppgaveId)
                StegType.VENTE_PÅ_STATUS_FRA_IVERKSETT
            } else {
                opprettTaskForJournalførBlankett(behandling)
                stegType().hentNesteSteg(behandling.type)
            }
        } else {
            vedtaksbrevRepository.deleteById(behandling.id)
            opprettBehandleUnderkjentVedtakOppgave(behandling, saksbehandler)
            StegType.SEND_TIL_BESLUTTER
        }
    }

    private fun opprettTaskForBehandlingsstatistikk(behandlingId: UUID, oppgaveId: Long?) {
        val vedtak = vedtakService.hentVedtak(behandlingId)

        taskRepository.save(BehandlingsstatistikkTask.opprettTask(behandlingId = behandlingId,
                                                                  hendelse = Hendelse.VEDTATT,
                                                                  hendelseTidspunkt = vedtak.opprettetTid, // Kan hente ut fra behandlingsstatistikken
                                                                  gjeldendeSaksbehandler = vedtak.saksbehandlerId, // Hent fra vedtaket
                                                                  oppgaveId = oppgaveId
        ))
        taskRepository.save(BehandlingsstatistikkTask.opprettTask(behandlingId = behandlingId,
                                                                  hendelse = Hendelse.BESLUTTET,
                                                                  hendelseTidspunkt = LocalDateTime.now(),
                                                                  gjeldendeSaksbehandler = vedtak.beslutterId, // Hent fra vedtaket
                                                                  oppgaveId = oppgaveId

        ))

    }

    private fun utledVedtaksbrev(vedtaksbrev: Vedtaksbrev): Fil {
        require(vedtaksbrev.beslutterPdf != null) { "For å iverksette må det finnes en pdf" }
        require(vedtaksbrev.besluttersignatur == SikkerhetContext.hentSaksbehandlerNavn(strict = true)) {
            "En annen saksbehandler har signert vedtaksbrevet"
        }
        return vedtaksbrev.beslutterPdf
    }

    private fun ferdigstillOppgave(behandling: Behandling): Long? {
        val oppgavetype = Oppgavetype.GodkjenneVedtak
        val aktivIdent = fagsakService.hentAktivIdent(behandling.fagsakId)
        return oppgaveService.hentOppgaveSomIkkeErFerdigstilt(oppgavetype, behandling)?.let {
            taskRepository.save(FerdigstillOppgaveTask.opprettTask(behandlingId = behandling.id, oppgavetype, it.gsakOppgaveId, aktivIdent))
            return it.gsakOppgaveId
        }
    }

    private fun opprettBehandleUnderkjentVedtakOppgave(behandling: Behandling, navIdent: String) {
        taskRepository.save(OpprettOppgaveTask.opprettTask(
                OpprettOppgaveTaskData(behandlingId = behandling.id,
                                       oppgavetype = Oppgavetype.BehandleUnderkjentVedtak,
                                       tilordnetNavIdent = navIdent)))
    }

    private fun opprettTaskForJournalførBlankett(behandling: Behandling) {
        val aktivIdent = fagsakService.hentAktivIdent(behandling.fagsakId)
        taskRepository.save(JournalførBlankettTask.opprettTask(behandling, aktivIdent))
    }

    private fun opprettPollForStatusOppgave(behandlingId: UUID) {
        taskRepository.save(PollStatusFraIverksettTask.opprettTask(behandlingId))
    }

    override fun stegType(): StegType {
        return StegType.BESLUTTE_VEDTAK
    }

    override fun utførSteg(behandling: Behandling, data: BeslutteVedtakDto) {
        error("Bruker utførOgReturnerNesteSteg")
    }

    override fun settInnHistorikk(): Boolean = false
}
