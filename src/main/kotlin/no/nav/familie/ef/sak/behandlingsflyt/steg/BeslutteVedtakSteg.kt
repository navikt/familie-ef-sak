package no.nav.familie.ef.sak.behandlingsflyt.steg

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.Saksbehandling
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
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.infrastruktur.exception.ApiFeil
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvis
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
import org.springframework.http.HttpStatus
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

    override fun validerSteg(saksbehandling: Saksbehandling) {
        if (saksbehandling.steg != stegType()) {
            throw Feil("Behandling er i feil steg=${saksbehandling.steg}")
        }

    }

    override fun utførOgReturnerNesteSteg(saksbehandling: Saksbehandling, data: BeslutteVedtakDto): StegType {
        fagsakService.fagsakMedOppdatertPersonIdent(saksbehandling.fagsakId)
        val saksbehandler = totrinnskontrollService.lagreTotrinnskontrollOgReturnerBehandler(saksbehandling, data)
        val beslutter = SikkerhetContext.hentSaksbehandler(strict = true)
        val oppgaveId = ferdigstillOppgave(saksbehandling)

        return if (data.godkjent) {
            vedtakService.oppdaterBeslutter(saksbehandling.id, SikkerhetContext.hentSaksbehandler(strict = true))
            when (saksbehandling.type) {
                BehandlingType.BLANKETT -> {
                    opprettTaskForJournalførBlankett(saksbehandling)
                    stegType().hentNesteSteg(saksbehandling.type)
                }
                else -> {
                    val vedtaksbrev = vedtaksbrevRepository.findByIdOrThrow(saksbehandling.id)
                    validerBeslutterVedtaksbrev(vedtaksbrev)
                    val fil = vedtaksbrev.beslutterPdf ?: throw ApiFeil("Beslutter-pdf er null, beslutter må kontrollere brevet.",
                                                                        HttpStatus.BAD_REQUEST)
                    val iverksettDto = iverksettingDtoMapper.tilDto(saksbehandling, beslutter)
                    oppdaterResultatPåBehandling(saksbehandling.id)
                    opprettPollForStatusOppgave(saksbehandling.id)
                    opprettTaskForBehandlingsstatistikk(saksbehandling.id, oppgaveId)
                    iverksettClient.iverksett(iverksettDto, fil)
                    StegType.VENTE_PÅ_STATUS_FRA_IVERKSETT
                }
            }
        } else {
            vedtaksbrevRepository.deleteById(saksbehandling.id)
            opprettBehandleUnderkjentVedtakOppgave(saksbehandling, saksbehandler)
            StegType.SEND_TIL_BESLUTTER
        }
    }

    private fun opprettTaskForBehandlingsstatistikk(behandlingId: UUID, oppgaveId: Long?) =
            taskRepository.save(BehandlingsstatistikkTask.opprettBesluttetTask(behandlingId = behandlingId,
                                                                               oppgaveId = oppgaveId))

    fun oppdaterResultatPåBehandling(behandlingId: UUID) {
        val resultat = vedtakService.hentVedtaksresultat(behandlingId)
        when (resultat) {
            ResultatType.INNVILGE -> behandlingService.oppdaterResultatPåBehandling(behandlingId, BehandlingResultat.INNVILGET)
            ResultatType.OPPHØRT -> behandlingService.oppdaterResultatPåBehandling(behandlingId, BehandlingResultat.OPPHØRT)
            ResultatType.AVSLÅ -> behandlingService.oppdaterResultatPåBehandling(behandlingId, BehandlingResultat.AVSLÅTT)
            ResultatType.SANKSJONERE -> behandlingService.oppdaterResultatPåBehandling(behandlingId, BehandlingResultat.INNVILGET)
            ResultatType.HENLEGGE -> error("Støtter ikke resultattypen=$resultat for behandling=$behandlingId")
        }
    }

    private fun validerBeslutterVedtaksbrev(vedtaksbrev: Vedtaksbrev) {

        brukerfeilHvis(vedtaksbrev.beslutterident == null || vedtaksbrev.beslutterident.isBlank()) {
            "Beklager. Det har skjedd en feil. Last brevsiden på nytt, kontroller brevet og prøv igjen."
        }

        validerSammeBeslutterIdent(vedtaksbrev)

    }

    private fun validerSammeBeslutterIdent(vedtaksbrev: Vedtaksbrev) {
        brukerfeilHvis(vedtaksbrev.beslutterident != SikkerhetContext.hentSaksbehandler(true)) { "En annen beslutter har signert vedtaksbrevet" }
    }

    private fun ferdigstillOppgave(saksbehandling: Saksbehandling): Long? {
        val oppgavetype = Oppgavetype.GodkjenneVedtak
        val aktivIdent = fagsakService.hentAktivIdent(saksbehandling.fagsakId)
        return oppgaveService.hentOppgaveSomIkkeErFerdigstilt(oppgavetype, saksbehandling)?.let {
            taskRepository.save(FerdigstillOppgaveTask.opprettTask(behandlingId = saksbehandling.id,
                                                                   oppgavetype = oppgavetype,
                                                                   oppgaveId = it.gsakOppgaveId,
                                                                   personIdent = aktivIdent))
            it.gsakOppgaveId
        }
    }

    private fun opprettBehandleUnderkjentVedtakOppgave(saksbehandling: Saksbehandling, navIdent: String) {
        taskRepository.save(OpprettOppgaveTask.opprettTask(
                OpprettOppgaveTaskData(behandlingId = saksbehandling.id,
                                       oppgavetype = Oppgavetype.BehandleUnderkjentVedtak,
                                       tilordnetNavIdent = navIdent)))
    }

    private fun opprettTaskForJournalførBlankett(saksbehandling: Saksbehandling) {
        taskRepository.save(JournalførBlankettTask.opprettTask(saksbehandling))
    }

    private fun opprettPollForStatusOppgave(behandlingId: UUID) {
        taskRepository.save(PollStatusFraIverksettTask.opprettTask(behandlingId))
    }

    override fun stegType(): StegType {
        return StegType.BESLUTTE_VEDTAK
    }

    override fun utførSteg(saksbehandling: Saksbehandling, data: BeslutteVedtakDto) {
        error("Bruker utførOgReturnerNesteSteg")
    }

    override fun settInnHistorikk(): Boolean = false
}
