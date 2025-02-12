package no.nav.familie.ef.sak.behandlingsflyt.steg

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.behandling.ÅrsakRevurderingService
import no.nav.familie.ef.sak.behandlingsflyt.task.BehandlingsstatistikkTask
import no.nav.familie.ef.sak.behandlingsflyt.task.FerdigstillOppgaveTask
import no.nav.familie.ef.sak.behandlingsflyt.task.OpprettOppgaveTask
import no.nav.familie.ef.sak.behandlingsflyt.task.OpprettOppgaveTask.OpprettOppgaveTaskData
import no.nav.familie.ef.sak.behandlingshistorikk.BehandlingshistorikkService
import no.nav.familie.ef.sak.behandlingshistorikk.domain.StegUtfall
import no.nav.familie.ef.sak.beregning.ValiderOmregningService
import no.nav.familie.ef.sak.brev.VedtaksbrevRepository
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.infrastruktur.exception.ApiFeil
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvisIkke
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext.NAVIDENT_REGEX
import no.nav.familie.ef.sak.oppfølgingsoppgave.OppfølgingsoppgaveService
import no.nav.familie.ef.sak.oppgave.TilordnetRessursService
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.ef.sak.simulering.SimuleringService
import no.nav.familie.ef.sak.tilbakekreving.TilbakekrevingService
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vedtak.dto.ResultatType
import no.nav.familie.ef.sak.vedtak.dto.ResultatType.INNVILGE
import no.nav.familie.ef.sak.vedtak.dto.ResultatType.INNVILGE_UTEN_UTBETALING
import no.nav.familie.ef.sak.vedtak.dto.SendTilBeslutterDto
import no.nav.familie.ef.sak.vilkår.VurderingService
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.internal.TaskService
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Service
class SendTilBeslutterSteg(
    private val taskService: TaskService,
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
    private val vedtaksbrevRepository: VedtaksbrevRepository,
    private val vedtakService: VedtakService,
    private val simuleringService: SimuleringService,
    private val tilbakekrevingService: TilbakekrevingService,
    private val vurderingService: VurderingService,
    private val validerOmregningService: ValiderOmregningService,
    private val årsakRevurderingService: ÅrsakRevurderingService,
    private val behandlingshistorikkService: BehandlingshistorikkService,
    private val tilordnetRessursService: TilordnetRessursService,
    private val oppfølgingsoppgaveService: OppfølgingsoppgaveService,
) : BehandlingSteg<SendTilBeslutterDto?> {
    override fun validerSteg(saksbehandling: Saksbehandling) {
        validerSaksbehandlingHarSammeStegtype(saksbehandling)
        validerAtSaksbehandlerErAnsvarligForBehandling(saksbehandling)
        validerBrev(saksbehandling)
        validerTilbakekrevingsvalgUtført(saksbehandling)
        validerRiktigTilstandVedInvilgelse(saksbehandling)
        validerSaksbehandlersignatur(saksbehandling)
        validerHarGammelGOgKanLagres(saksbehandling)
        validerHarGyldigRevurderingsinformasjon(saksbehandling)
        validerAtDetFinnesOppgave(saksbehandling)
    }

    private fun validerHarGyldigRevurderingsinformasjon(saksbehandling: Saksbehandling) {
        årsakRevurderingService.validerHarGyldigRevurderingsinformasjon(saksbehandling)
    }

    private fun validerHarGammelGOgKanLagres(saksbehandling: Saksbehandling) {
        validerOmregningService.validerHarGammelGOgKanLagres(saksbehandling)
    }

    private fun validerTilbakekrevingsvalgUtført(saksbehandling: Saksbehandling) {
        brukerfeilHvis(saksbehandlerMåTaStillingTilTilbakekreving(saksbehandling)) {
            "Feilutbetaling detektert. Må ta stilling til feilutbetalingsvarsel under simulering"
        }
    }

    private fun validerBrev(saksbehandling: Saksbehandling) {
        if (saksbehandling.skalSendeBrev &&
            !vedtaksbrevRepository.existsById(saksbehandling.id)
        ) {
            throw Feil("Brev mangler for behandling=${saksbehandling.id}")
        }
    }

    private fun validerSaksbehandlingHarSammeStegtype(saksbehandling: Saksbehandling) {
        if (saksbehandling.steg != stegType()) {
            throw ApiFeil("Behandling er i feil steg=${saksbehandling.steg}", HttpStatus.BAD_REQUEST)
        }
    }

    private fun validerAtDetFinnesOppgave(saksbehandling: Saksbehandling) {
        feilHvis(tilordnetRessursService.hentEFOppgaveSomIkkeErFerdigstilt(saksbehandling.id, setOf(Oppgavetype.BehandleSak, Oppgavetype.BehandleUnderkjentVedtak)) == null) {
            "Oppgaven for behandlingen er ikke tilgjengelig. Vennligst vent og prøv igjen om litt."
        }
    }

    private fun validerAtSaksbehandlerErAnsvarligForBehandling(saksbehandling: Saksbehandling) {
        feilHvis(!tilordnetRessursService.tilordnetRessursErInnloggetSaksbehandler(saksbehandling.id)) {
            "Behandlingen har en ny eier og kan derfor ikke sendes til totrinnskontroll av deg"
        }
    }

    private fun validerRiktigTilstandVedInvilgelse(saksbehandling: Saksbehandling) {
        val vedtaksresultat = vedtakService.hentVedtaksresultat(saksbehandling.id)
        if (vedtaksresultat == INNVILGE_UTEN_UTBETALING || vedtaksresultat == INNVILGE) {
            brukerfeilHvisIkke(vurderingService.erAlleVilkårOppfylt(saksbehandling.id)) {
                "Kan ikke innvilge hvis ikke alle vilkår er oppfylt for behandlingId: ${saksbehandling.id}"
            }
        }
    }

    private fun saksbehandlerMåTaStillingTilTilbakekreving(saksbehandling: Saksbehandling): Boolean {
        if (erIkkeRelevantForTilbakekreving(saksbehandling)) {
            return false
        }
        val feilutbetaling =
            simuleringService.hentLagretSimuleringsoppsummering(saksbehandling.id).feilutbetaling > BigDecimal.ZERO
        val harIkkeTattStillingTil =
            !tilbakekrevingService.harSaksbehandlerTattStillingTilTilbakekreving(saksbehandling.id)
        if (feilutbetaling && harIkkeTattStillingTil) {
            return !tilbakekrevingService.finnesÅpenTilbakekrevingsBehandling(saksbehandling.id)
        }

        return false
    }

    private fun erIkkeRelevantForTilbakekreving(saksbehandling: Saksbehandling): Boolean {
        val resultatType = vedtakService.hentVedtaksresultat(saksbehandling.id)
        return saksbehandling.type == BehandlingType.FØRSTEGANGSBEHANDLING ||
            resultatType == ResultatType.AVSLÅ ||
            resultatType == ResultatType.HENLEGGE
    }

    override fun utførSteg(
        saksbehandling: Saksbehandling,
        data: SendTilBeslutterDto?,
    ) {
        behandlingService.oppdaterStatusPåBehandling(saksbehandling.id, BehandlingStatus.FATTER_VEDTAK)
        vedtakService.oppdaterSaksbehandler(saksbehandling.id, SikkerhetContext.hentSaksbehandler())

        if (vedtakService.hentVedtak(saksbehandling.id).skalVedtakBesluttes()) {
            opprettGodkjennVedtakOppgave(saksbehandling)
        }

        ferdigstillOppgave(saksbehandling)
        opprettTaskForBehandlingsstatistikk(saksbehandling.id)
        if (data != null) {
            oppfølgingsoppgaveService.lagreOppgaveIderForFerdigstilling(
                saksbehandling.id,
                data.fremleggsoppgaveIderSomSkalFerdigstilles,
            )

            oppfølgingsoppgaveService.lagreOppgaverForOpprettelse(
                saksbehandling.id,
                data,
            )
        }
    }

    private fun opprettTaskForBehandlingsstatistikk(behandlingId: UUID) = taskService.save(BehandlingsstatistikkTask.opprettVedtattTask(behandlingId = behandlingId))

    private fun ferdigstillOppgave(saksbehandling: Saksbehandling) {
        val aktivIdent = fagsakService.hentAktivIdent(saksbehandling.fagsakId)

        val besluttetVedtakHendelse =
            behandlingshistorikkService.finnSisteBehandlingshistorikk(saksbehandling.id, StegType.BESLUTTE_VEDTAK)
        val oppgavetype =
            if (besluttetVedtakHendelse?.utfall == StegUtfall.BESLUTTE_VEDTAK_UNDERKJENT) {
                Oppgavetype.BehandleUnderkjentVedtak
            } else {
                Oppgavetype.BehandleSak
            }

        taskService.save(
            FerdigstillOppgaveTask.opprettTask(
                behandlingId = saksbehandling.id,
                oppgavetype,
                null,
                aktivIdent,
            ),
        )
    }

    private fun opprettGodkjennVedtakOppgave(
        saksbehandling: Saksbehandling,
    ) {
        taskService.save(
            OpprettOppgaveTask.opprettTask(
                OpprettOppgaveTaskData(
                    behandlingId = saksbehandling.id,
                    oppgavetype = Oppgavetype.GodkjenneVedtak,
                    beskrivelse = "Sendt til godkjenning av ${SikkerhetContext.hentSaksbehandlerNavn(true)}.",
                    tilordnetNavIdent = utledBeslutterIdent(saksbehandling),
                ),
            ),
        )
    }

    private fun utledBeslutterIdent(saksbehandling: Saksbehandling): String? =
        behandlingshistorikkService
            .finnSisteBehandlingshistorikk(saksbehandling.id, StegType.BESLUTTE_VEDTAK)
            ?.let { vedtak ->
                val beslutterIdent = vedtak.opprettetAv.takeIf { NAVIDENT_REGEX.matches(it) }
                val skalTilordneNavIdent =
                    beslutterIdent != SikkerhetContext.hentSaksbehandler() &&
                        vedtak.endretTid.isAfter(LocalDateTime.now().minusWeeks(3))
                beslutterIdent.takeIf { skalTilordneNavIdent }
            }

    private fun validerSaksbehandlersignatur(saksbehandling: Saksbehandling) {
        if (saksbehandling.skalIkkeSendeBrev) return

        val vedtaksbrev = vedtaksbrevRepository.findByIdOrThrow(saksbehandling.id)

        brukerfeilHvis(vedtaksbrev.saksbehandlerident != SikkerhetContext.hentSaksbehandler()) {
            "En annen saksbehandler har signert vedtaksbrevet"
        }
    }

    override fun stegType(): StegType = StegType.SEND_TIL_BESLUTTER
}
