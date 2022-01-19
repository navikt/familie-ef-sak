package no.nav.familie.ef.sak.behandlingsflyt.steg

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.behandlingsflyt.task.BehandlingsstatistikkTask
import no.nav.familie.ef.sak.behandlingsflyt.task.FerdigstillOppgaveTask
import no.nav.familie.ef.sak.behandlingsflyt.task.OpprettOppgaveTask
import no.nav.familie.ef.sak.behandlingsflyt.task.OpprettOppgaveTask.OpprettOppgaveTaskData
import no.nav.familie.ef.sak.behandlingsflyt.task.PollStatusFraIverksettTask
import no.nav.familie.ef.sak.blankett.JournalførBlankettTask
import no.nav.familie.ef.sak.brev.VedtaksbrevRepository
import no.nav.familie.ef.sak.brev.domain.Vedtaksbrev
import no.nav.familie.ef.sak.brev.domain.VedtaksbrevKonstanter.IKKE_SATT_IDENT_PÅ_GAMLE_VEDTAKSBREV
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.ef.sak.iverksett.IverksettingDtoMapper
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.ef.sak.vedtak.TotrinnskontrollService
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vedtak.dto.BeslutteVedtakDto
import no.nav.familie.ef.sak.vedtak.dto.ResultatType
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
                         private val behandlingService: BehandlingService,
                         private val vedtakService: VedtakService) : BehandlingSteg<BeslutteVedtakDto> {

    override fun validerSteg(behandling: Behandling) {
        if (behandling.steg != stegType()) {
            throw Feil("Behandling er i feil steg=${behandling.steg}")
        }

    }

    override fun utførOgReturnerNesteSteg(behandling: Behandling, data: BeslutteVedtakDto): StegType {
        fagsakService.fagsakMedOppdatertPersonIdent(behandling.fagsakId)
        val saksbehandler = totrinnskontrollService.lagreTotrinnskontrollOgReturnerBehandler(behandling, data)
        val beslutter = SikkerhetContext.hentSaksbehandler(strict = true)
        val oppgaveId = ferdigstillOppgave(behandling)

        return if (data.godkjent) {
            vedtakService.oppdaterBeslutter(behandling.id, SikkerhetContext.hentSaksbehandler(strict = true))
            when (behandling.type) {
                BehandlingType.BLANKETT -> {
                    opprettTaskForJournalførBlankett(behandling)
                    stegType().hentNesteSteg(behandling.type)
                }
                else -> {
                    val vedtaksbrev = vedtaksbrevRepository.findByIdOrThrow(behandling.id)
                    validerBeslutterVedtaksbrev(vedtaksbrev)
                    val fil = vedtaksbrev.beslutterPdf ?: throw Feil("Beslutter-pdf er null, beslutter må kontrollere brevet.")
                    val iverksettDto = iverksettingDtoMapper.tilDto(behandling, beslutter)
                    oppdaterResultatPåBehandling(behandling.id)
                    opprettPollForStatusOppgave(behandling.id)
                    opprettTaskForBehandlingsstatistikk(behandling.id, oppgaveId)
                    iverksettClient.iverksett(iverksettDto, fil)
                    StegType.VENTE_PÅ_STATUS_FRA_IVERKSETT
                }
            }
        } else {
            vedtaksbrevRepository.deleteById(behandling.id)
            opprettBehandleUnderkjentVedtakOppgave(behandling, saksbehandler)
            StegType.SEND_TIL_BESLUTTER
        }
    }

    private fun opprettTaskForBehandlingsstatistikk(behandlingId: UUID, oppgaveId: Long?) =
            taskRepository.save(BehandlingsstatistikkTask.opprettBesluttetTask(behandlingId = behandlingId,
                                                                               oppgaveId = oppgaveId))

    fun oppdaterResultatPåBehandling(behandlingId: UUID) {
        val vedtak = vedtakService.hentVedtak(behandlingId)
        when (vedtak.resultatType) {
            ResultatType.INNVILGE -> behandlingService.oppdaterResultatPåBehandling(behandlingId, BehandlingResultat.INNVILGET)
            ResultatType.OPPHØRT -> behandlingService.oppdaterResultatPåBehandling(behandlingId, BehandlingResultat.OPPHØRT)
            ResultatType.AVSLÅ -> behandlingService.oppdaterResultatPåBehandling(behandlingId, BehandlingResultat.AVSLÅTT)
            else -> error("Støtter ikke resultattypen=${vedtak.resultatType}")
        }
    }

    // TODO mattis -> hva skjer egentlig her, Validerer vi når vi skal se på den allerede genererte pdf'en?
    private fun validerBeslutterVedtaksbrev(vedtaksbrev: Vedtaksbrev) {
        // IKKE SATT = kun på gamle saker. Disse er heller ikke anonymisert, så her burde det være mulig å sammenlikne navn i signatur.


        when (vedtaksbrev.beslutterident) {
            IKKE_SATT_IDENT_PÅ_GAMLE_VEDTAKSBREV -> validerSammebeslutterSignaturnavn(vedtaksbrev)
            else -> validerSammeBeslutterIdent(vedtaksbrev)
        }


    }

    private fun validerSammeBeslutterIdent(vedtaksbrev: Vedtaksbrev) {
        feilHvis(vedtaksbrev.beslutterident != SikkerhetContext.hentSaksbehandler(true)) { "En annen saksbehandler har signert vedtaksbrevet" }
    }

    private fun validerSammebeslutterSignaturnavn(vedtaksbrev: Vedtaksbrev) {
        feilHvis(vedtaksbrev.besluttersignatur != SikkerhetContext.hentSaksbehandlerNavn(strict = true)) {
            "En annen saksbehandler har signert vedtaksbrevet"
        }
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
