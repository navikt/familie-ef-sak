package no.nav.familie.ef.sak.behandlingsflyt.steg

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.BehandlingshistorikkService
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.blankett.JournalførBlankettTask
import no.nav.familie.ef.sak.domene.Fil
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.ef.sak.iverksett.IverksettingDtoMapper
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.ef.sak.vedtak.TotrinnskontrollService
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.behandlingsflyt.task.BehandlingsstatistikkTask
import no.nav.familie.ef.sak.behandlingsflyt.task.FerdigstillOppgaveTask
import no.nav.familie.ef.sak.behandlingsflyt.task.OpprettOppgaveTask
import no.nav.familie.ef.sak.behandlingsflyt.task.OpprettOppgaveTask.OpprettOppgaveTaskData
import no.nav.familie.ef.sak.behandlingsflyt.task.PollStatusFraIverksettTask
import no.nav.familie.ef.sak.vedtak.dto.BeslutteVedtakDto
import no.nav.familie.ef.sak.vedtak.dto.ResultatType
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vedtak.domain.Vedtaksbrev
import no.nav.familie.ef.sak.vedtak.VedtaksbrevRepository
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class BeslutteVedtakSteg(private val taskRepository: TaskRepository,
                         private val fagsakService: FagsakService,
                         private val oppgaveService: OppgaveService,
                         private val iverksettClient: IverksettClient,
                         private val iverksettingDtoMapper: IverksettingDtoMapper,
                         private val totrinnskontrollService: TotrinnskontrollService,
                         private val vedtaksbrevRepository: VedtaksbrevRepository,
                         private val behandlingshistorikkService: BehandlingshistorikkService,
                         private val behandlingService: BehandlingService,
                         private val vedtakService: VedtakService) : BehandlingSteg<BeslutteVedtakDto> {

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
            vedtakService.oppdaterBeslutter(behandling.id, SikkerhetContext.hentSaksbehandler(strict = true))
            if (behandling.type != BehandlingType.BLANKETT) {
                val vedtaksbrev = vedtaksbrevRepository.findByIdOrThrow(behandling.id)
                val fil = utledVedtaksbrev(vedtaksbrev)
                val iverksettDto = iverksettingDtoMapper.tilDto(behandling, beslutter)
                oppdaterResultatPåBehandling(behandling.id)
                opprettPollForStatusOppgave(behandling.id)
                opprettTaskForBehandlingsstatistikk(behandling.id, oppgaveId)
                iverksettClient.iverksett(iverksettDto, fil)
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
        val vedtakstidspunkt = behandlingshistorikkService.finnSisteBehandlingshistorikk(behandlingId, StegType.SEND_TIL_BESLUTTER)?.endretTid ?: error("Mangler behandlingshistorikk for vedtak") // TODO: Bruk vedtak.endretTid når det kommer på plass

        taskRepository.save(BehandlingsstatistikkTask.opprettVedtattTask(behandlingId = behandlingId,
                                                                  hendelseTidspunkt = vedtakstidspunkt,
                                                                  oppgaveId = oppgaveId))

        taskRepository.save(BehandlingsstatistikkTask.opprettBesluttetTask(behandlingId = behandlingId,
                                                                  oppgaveId = oppgaveId))

    }

    fun oppdaterResultatPåBehandling(behandlingId: UUID) {
        val vedtak = vedtakService.hentVedtak(behandlingId)
        when (vedtak.resultatType) {
            ResultatType.INNVILGE -> behandlingService.oppdaterResultatPåBehandling(behandlingId, BehandlingResultat.INNVILGET)
        }
    }

    private fun utledVedtaksbrev(vedtaksbrev: Vedtaksbrev): Fil {
        feilHvis(vedtaksbrev.beslutterPdf == null) { "For å iverksette må vedtaksbrevet signeres" }
        feilHvis(vedtaksbrev.besluttersignatur != SikkerhetContext.hentSaksbehandlerNavn(strict = true)) {
            "En annen saksbehandler har signert vedtaksbrevet"
        }
        return vedtaksbrev.beslutterPdf!!
    }

    private fun ferdigstillOppgave(behandling: Behandling): Long? {
        val oppgavetype = Oppgavetype.GodkjenneVedtak
        val aktivIdent = fagsakService.hentAktivIdent(behandling.fagsakId)
        return oppgaveService.hentOppgaveSomIkkeErFerdigstilt(oppgavetype, behandling)?.let {
            taskRepository.save(FerdigstillOppgaveTask.opprettTask(behandlingId = behandling.id,
                                                                   oppgavetype = oppgavetype,
                                                                   oppgaveId = it.gsakOppgaveId,
                                                                   personIdent = aktivIdent))
            it.gsakOppgaveId
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
