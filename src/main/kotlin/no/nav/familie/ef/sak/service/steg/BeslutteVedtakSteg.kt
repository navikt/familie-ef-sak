package no.nav.familie.ef.sak.service.steg

import no.nav.familie.ef.sak.api.Feil
import no.nav.familie.ef.sak.api.dto.BeslutteVedtakDto
import no.nav.familie.ef.sak.blankett.JournalførBlankettTask
import no.nav.familie.ef.sak.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.ef.sak.mapper.IverksettingDtoMapper
import no.nav.familie.ef.sak.repository.VedtaksbrevRepository
import no.nav.familie.ef.sak.repository.domain.Behandling
import no.nav.familie.ef.sak.repository.domain.BehandlingType
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.ef.sak.service.FagsakService
import no.nav.familie.ef.sak.service.OppgaveService
import no.nav.familie.ef.sak.service.TotrinnskontrollService
import no.nav.familie.ef.sak.service.VedtaksbrevService
import no.nav.familie.ef.sak.task.FerdigstillOppgaveTask
import no.nav.familie.ef.sak.task.IverksettMotOppdragTask
import no.nav.familie.ef.sak.task.OpprettOppgaveTask
import no.nav.familie.ef.sak.task.OpprettOppgaveTask.OpprettOppgaveTaskData
import no.nav.familie.ef.sak.task.PollStatusFraIverksettTask
import no.nav.familie.ef.sak.vedtaksbrev.IverksettClient
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class BeslutteVedtakSteg(private val taskRepository: TaskRepository,
                         private val fagsakService: FagsakService,
                         private val oppgaveService: OppgaveService,
                         private val featureToggleService: FeatureToggleService,
                         private val iverksettClient: IverksettClient,
                         private val iverksettingDtoMapper: IverksettingDtoMapper,
                         private val totrinnskontrollService: TotrinnskontrollService,
                         private val vedtaksbrevRepository: VedtaksbrevRepository,
                         private val vedtaksbrevService: VedtaksbrevService) : BehandlingSteg<BeslutteVedtakDto> {

    override fun validerSteg(behandling: Behandling) {
        if (behandling.steg != stegType()) {
            throw Feil("Behandling er i feil steg=${behandling.steg}")
        }

        val vedtaksbrev = vedtaksbrevRepository.findByIdOrThrow(behandling.id)
        if (vedtaksbrev.beslutterPdf === null || vedtaksbrev.besluttersignatur === null) {
            throw Feil("Behandling=${behandling.id} mangler gyldig vedtaksbrev")
        }

    }

    override fun utførOgReturnerNesteSteg(behandling: Behandling, data: BeslutteVedtakDto): StegType {
        val beslutter = totrinnskontrollService.lagreTotrinnskontrollOgReturnerBehandler(behandling, data)

        ferdigstillOppgave(behandling)

        return if (data.godkjent) {
            if (behandling.type != BehandlingType.BLANKETT) {
                val fil = vedtaksbrevRepository.findByIdOrThrow(behandling.id).beslutterPdf
                require(fil != null) { "For å iverksette må det finnes en pdf" }
                if (featureToggleService.isEnabled("familie.ef.sak.brukEFIverksett")) {
                    val iverksettDto = iverksettingDtoMapper.tilDto(behandling, beslutter)
                    iverksettClient.iverksett(iverksettDto, fil)
                    opprettPollForStatusOppgave(behandling.id)
                    StegType.VENTE_PÅ_STATUS_FRA_IVERKSETT
                } else {
                    opprettTaskForIverksettMotOppdrag(behandling)
                    stegType().hentNesteSteg(behandling.type)
                }
            } else {
                opprettTaskForJournalførBlankett(behandling)
                stegType().hentNesteSteg(behandling.type)
            }
        } else {
            vedtaksbrevRepository.deleteById(behandling.id)
            opprettBehandleUnderkjentVedtakOppgave(behandling, beslutter)
            StegType.SEND_TIL_BESLUTTER
        }
    }

    private fun ferdigstillOppgave(behandling: Behandling) {
        val oppgavetype = Oppgavetype.GodkjenneVedtak
        oppgaveService.hentOppgaveSomIkkeErFerdigstilt(oppgavetype, behandling)?.let {
            taskRepository.save(FerdigstillOppgaveTask.opprettTask(behandlingId = behandling.id, oppgavetype))
        }
    }

    private fun opprettBehandleUnderkjentVedtakOppgave(behandling: Behandling, navIdent: String) {
        taskRepository.save(OpprettOppgaveTask.opprettTask(
                OpprettOppgaveTaskData(behandlingId = behandling.id,
                                       oppgavetype = Oppgavetype.BehandleUnderkjentVedtak,
                                       tilordnetNavIdent = navIdent)))
    }

    private fun opprettTaskForIverksettMotOppdrag(behandling: Behandling) {
        val aktivIdent = fagsakService.hentAktivIdent(behandling.fagsakId)
        taskRepository.save(IverksettMotOppdragTask.opprettTask(behandling, aktivIdent))
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