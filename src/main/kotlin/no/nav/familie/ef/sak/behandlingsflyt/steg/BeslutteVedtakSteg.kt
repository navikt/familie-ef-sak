package no.nav.familie.ef.sak.behandlingsflyt.steg

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandlingsflyt.task.BehandlingsstatistikkTask
import no.nav.familie.ef.sak.behandlingsflyt.task.FerdigstillOppgaveTask
import no.nav.familie.ef.sak.behandlingsflyt.task.OpprettOppgaveTask
import no.nav.familie.ef.sak.behandlingsflyt.task.OpprettOppgaveTask.OpprettOppgaveTaskData
import no.nav.familie.ef.sak.behandlingsflyt.task.PollStatusFraIverksettTask
import no.nav.familie.ef.sak.brev.VedtaksbrevService
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.ef.sak.iverksett.IverksettingDtoMapper
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.ef.sak.vedtak.TotrinnskontrollService
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vedtak.dto.BeslutteVedtakDto
import no.nav.familie.ef.sak.vedtak.dto.ResultatType
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class BeslutteVedtakSteg(
    private val taskRepository: TaskRepository,
    private val fagsakService: FagsakService,
    private val oppgaveService: OppgaveService,
    private val iverksettClient: IverksettClient,
    private val iverksettingDtoMapper: IverksettingDtoMapper,
    private val totrinnskontrollService: TotrinnskontrollService,
    private val behandlingService: BehandlingService,
    private val vedtakService: VedtakService,
    private val vedtaksbrevService: VedtaksbrevService,
    private val featureToggleService: FeatureToggleService
) : BehandlingSteg<BeslutteVedtakDto> {

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
            val iverksettDto = iverksettingDtoMapper.tilDto(saksbehandling, beslutter)
            oppdaterResultatPåBehandling(saksbehandling.id)
            opprettPollForStatusOppgave(saksbehandling.id)
            opprettTaskForBehandlingsstatistikk(saksbehandling.id, oppgaveId)
            if (saksbehandling.årsak == BehandlingÅrsak.KORRIGERING_UTEN_BREV || saksbehandling.erOmregning || saksbehandling.årsak == BehandlingÅrsak.SATSENDRING) {
                iverksettClient.iverksettUtenBrev(iverksettDto)
            } else {
                val fil = vedtaksbrevService.lagEndeligBeslutterbrev(saksbehandling)
                iverksettClient.iverksett(iverksettDto, fil)
            }
            StegType.VENTE_PÅ_STATUS_FRA_IVERKSETT
        } else {
            opprettBehandleUnderkjentVedtakOppgave(saksbehandling, saksbehandler)
            StegType.SEND_TIL_BESLUTTER
        }
    }

    private fun opprettTaskForBehandlingsstatistikk(behandlingId: UUID, oppgaveId: Long?) =
        taskRepository.save(
            BehandlingsstatistikkTask.opprettBesluttetTask(
                behandlingId = behandlingId,
                oppgaveId = oppgaveId
            )
        )

    private fun oppdaterResultatPåBehandling(behandlingId: UUID) {
        val resultat = vedtakService.hentVedtaksresultat(behandlingId)
        when (resultat) {
            ResultatType.INNVILGE, ResultatType.INNVILGE_UTEN_UTBETALING -> behandlingService.oppdaterResultatPåBehandling(
                behandlingId,
                BehandlingResultat.INNVILGET
            )
            ResultatType.OPPHØRT -> behandlingService.oppdaterResultatPåBehandling(behandlingId, BehandlingResultat.OPPHØRT)
            ResultatType.AVSLÅ -> behandlingService.oppdaterResultatPåBehandling(behandlingId, BehandlingResultat.AVSLÅTT)
            ResultatType.SANKSJONERE -> behandlingService.oppdaterResultatPåBehandling(behandlingId, BehandlingResultat.INNVILGET)
            ResultatType.HENLEGGE -> error("Støtter ikke resultattypen=$resultat for behandling=$behandlingId")
        }
    }

    private fun ferdigstillOppgave(saksbehandling: Saksbehandling): Long? {
        val oppgavetype = Oppgavetype.GodkjenneVedtak
        val aktivIdent = fagsakService.hentAktivIdent(saksbehandling.fagsakId)
        return oppgaveService.hentOppgaveSomIkkeErFerdigstilt(oppgavetype, saksbehandling)?.let {
            taskRepository.save(
                FerdigstillOppgaveTask.opprettTask(
                    behandlingId = saksbehandling.id,
                    oppgavetype = oppgavetype,
                    oppgaveId = it.gsakOppgaveId,
                    personIdent = aktivIdent
                )
            )
            it.gsakOppgaveId
        }
    }

    private fun opprettBehandleUnderkjentVedtakOppgave(saksbehandling: Saksbehandling, navIdent: String) {
        taskRepository.save(
            OpprettOppgaveTask.opprettTask(
                OpprettOppgaveTaskData(
                    behandlingId = saksbehandling.id,
                    oppgavetype = Oppgavetype.BehandleUnderkjentVedtak,
                    tilordnetNavIdent = navIdent
                )
            )
        )
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
