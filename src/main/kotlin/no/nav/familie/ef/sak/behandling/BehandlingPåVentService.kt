package no.nav.familie.ef.sak.behandling

import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus.SATT_PÅ_VENT
import no.nav.familie.ef.sak.behandling.dto.SettPåVentRequest
import no.nav.familie.ef.sak.behandling.dto.TaAvVentStatus
import no.nav.familie.ef.sak.behandling.dto.TaAvVentStatusDto
import no.nav.familie.ef.sak.behandling.dto.beskrivelse
import no.nav.familie.ef.sak.behandlingsflyt.task.BehandlingsstatistikkTask
import no.nav.familie.ef.sak.behandlingsflyt.task.OpprettOppgaveTask
import no.nav.familie.ef.sak.behandlingshistorikk.BehandlingshistorikkService
import no.nav.familie.ef.sak.behandlingshistorikk.domain.StegUtfall
import no.nav.familie.ef.sak.felles.util.dagensDatoMedTidNorskFormat
import no.nav.familie.ef.sak.infrastruktur.exception.ApiFeil
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.oppgave.OppgaveClient
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.ef.sak.oppgave.OppgaveSubtype
import no.nav.familie.ef.sak.oppgave.TilordnetRessursService
import no.nav.familie.ef.sak.vedtak.NullstillVedtakService
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.internal.TaskService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

private const val INGEN_MAPPE = "<ingen>"

@Service
class BehandlingPåVentService(
    private val behandlingService: BehandlingService,
    private val behandlingshistorikkService: BehandlingshistorikkService,
    private val taskService: TaskService,
    private val nullstillVedtakService: NullstillVedtakService,
    private val oppgaveService: OppgaveService,
    private val tilordnetRessursService: TilordnetRessursService,
) {
    val logger: Logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun settPåVent(
        behandlingId: UUID,
        settPåVentRequest: SettPåVentRequest,
    ) {
        val behandling = behandlingService.hentBehandling(behandlingId)

        validerKanSettePåVent(behandling)

        behandlingService.oppdaterStatusPåBehandling(behandlingId, SATT_PÅ_VENT)
        opprettHistorikkInnslag(behandling, StegUtfall.SATT_PÅ_VENT)
        taskService.save(BehandlingsstatistikkTask.opprettVenterTask(behandlingId))

        oppdaterVerdierPåOppgave(settPåVentRequest)

        if (!settPåVentRequest.oppfølgingsoppgaverMotLokalKontor.isNullOrEmpty()) {
            opprettVurderHenvendelseOppgaveTasks(
                behandlingId,
                settPåVentRequest.oppfølgingsoppgaverMotLokalKontor,
                settPåVentRequest.innstillingsoppgaveBeskjed,
            )
        }
    }

    private fun oppdaterVerdierPåOppgave(settPåVentRequest: SettPåVentRequest) {
        val oppgave = oppgaveService.hentOppgave(settPåVentRequest.oppgaveId)

        val endretAvEnhetsnr = oppgaveService.hentSaksbehandler(SikkerhetContext.hentSaksbehandler()).enhet

        val beskrivelse = utledOppgavebeskrivelse(oppgave, settPåVentRequest)

        oppgaveService.oppdaterOppgave(
            Oppgave(
                id = settPåVentRequest.oppgaveId,
                tilordnetRessurs = settPåVentRequest.saksbehandler,
                prioritet = settPåVentRequest.prioritet,
                fristFerdigstillelse = settPåVentRequest.frist,
                mappeId = settPåVentRequest.mappe,
                beskrivelse = beskrivelse,
                versjon = settPåVentRequest.oppgaveVersjon,
                endretAvEnhetsnr = endretAvEnhetsnr,
            ),
        )
    }

    private fun opprettVurderHenvendelseOppgaveTasks(
        behandlingId: UUID,
        vurderHenvendelseOppgaver: List<OppgaveSubtype>,
        innstillingsoppgaveBeskjed: String?,
    ) {
        val saksbehandling = behandlingService.hentSaksbehandling(behandlingId)

        validerKanOppretteVurderHenvendelseOppgave(saksbehandling, vurderHenvendelseOppgaver)

        vurderHenvendelseOppgaver.forEach {
            taskService.save(
                OpprettOppgaveTask.opprettTask(
                    OpprettOppgaveTask.OpprettOppgaveTaskData(
                        behandlingId = saksbehandling.id,
                        oppgavetype = Oppgavetype.VurderHenvendelse,
                        vurderHenvendelseOppgaveSubtype = it,
                        beskrivelse = it.beskrivelse(innstillingsoppgaveBeskjed),
                    ),
                ),
            )
        }
    }

    private fun utledOppgavebeskrivelse(
        oppgave: Oppgave,
        settPåVentRequest: SettPåVentRequest,
    ): String {
        val tilordnetSaksbehandler = utledTilordnetSaksbehandlerBeskrivelse(oppgave, settPåVentRequest)

        val prioritet = utledPrioritetBeskrivelse(oppgave, settPåVentRequest)

        val frist = utledOppgavefristBeskrivelse(oppgave, settPåVentRequest)

        val mappe = utledMappeBeskrivelse(oppgave, settPåVentRequest)

        val harEndringer =
            tilordnetSaksbehandler.isNotBlank() || prioritet.isNotBlank() || frist.isNotBlank() || mappe.isNotBlank()

        val beskrivelse = utledNyBeskrivelse(harEndringer, settPåVentRequest)

        val skalOppdatereBeskrivelse = harEndringer || beskrivelse.isNotBlank()
        val tidligereBeskrivelse =
            if (skalOppdatereBeskrivelse && oppgave.beskrivelse?.isNotBlank() == true) {
                "\n${oppgave.beskrivelse.orEmpty()}"
            } else {
                oppgave.beskrivelse.orEmpty()
            }

        val prefix = utledBeskrivelsePrefix()

        val nyBeskrivelse =
            if (skalOppdatereBeskrivelse) {
                prefix + beskrivelse + tilordnetSaksbehandler + prioritet + frist + mappe + tidligereBeskrivelse
            } else {
                tidligereBeskrivelse
            }

        return nyBeskrivelse.trimEnd()
    }

    private fun utledPrioritetBeskrivelse(
        oppgave: Oppgave,
        settPåVentRequest: SettPåVentRequest,
    ): String =
        if (oppgave.prioritet != settPåVentRequest.prioritet) {
            "Oppgave endret fra prioritet ${oppgave.prioritet?.name} til ${settPåVentRequest.prioritet}\n"
        } else {
            ""
        }

    private fun utledNyBeskrivelse(
        harEndringer: Boolean,
        settPåVentRequest: SettPåVentRequest,
    ): String =
        when {
            settPåVentRequest.beskrivelse.isBlank() -> ""
            harEndringer -> "${settPåVentRequest.beskrivelse}\n"
            else -> "${settPåVentRequest.beskrivelse}\n"
        }

    private fun utledBeskrivelsePrefix(): String {
        val innloggetSaksbehandlerIdent = SikkerhetContext.hentSaksbehandlerEllerSystembruker()
        val saksbehandlerNavn = SikkerhetContext.hentSaksbehandlerNavn(strict = false)

        val prefix = "--- ${dagensDatoMedTidNorskFormat()} $saksbehandlerNavn ($innloggetSaksbehandlerIdent) ---\n"
        return prefix
    }

    private fun utledMappeBeskrivelse(
        oppgave: Oppgave,
        settPåVentRequest: SettPåVentRequest,
    ): String {
        val mapper =
            oppgaveService.finnMapper(
                oppgave.tildeltEnhetsnr ?: throw Feil("Kan ikke finne mapper når oppgave mangler enhet"),
            )

        val eksisterendeMappenavn = mapper.find { it.id.toLong() == oppgave.mappeId }?.navn
        val nyMappeNavn = mapper.find { it.id.toLong() == settPåVentRequest.mappe }?.navn

        val eksisterendeMappe = eksisterendeMappenavn ?: INGEN_MAPPE
        val nyMappe = nyMappeNavn ?: INGEN_MAPPE

        return if (eksisterendeMappe == nyMappe) "" else "Oppgave flyttet fra mappe $eksisterendeMappe til ${nyMappe}\n"
    }

    private fun utledOppgavefristBeskrivelse(
        oppgave: Oppgave,
        settPåVentRequest: SettPåVentRequest,
    ): String {
        val eksisterendeFrist = oppgave.fristFerdigstillelse ?: INGEN_MAPPE
        val nyFrist = settPåVentRequest.frist
        return if (eksisterendeFrist == nyFrist) "" else "Oppgave endret frist fra $eksisterendeFrist til ${nyFrist}\n"
    }

    private fun utledTilordnetSaksbehandlerBeskrivelse(
        oppgave: Oppgave,
        settPåVentRequest: SettPåVentRequest,
    ): String {
        val eksisterendeSaksbehandler = oppgave.tilordnetRessurs ?: INGEN_MAPPE
        val nySaksbehandler =
            if (settPåVentRequest.saksbehandler == "") INGEN_MAPPE else settPåVentRequest.saksbehandler

        return if (eksisterendeSaksbehandler == nySaksbehandler) {
            ""
        } else {
            "Oppgave flyttet fra saksbehandler $eksisterendeSaksbehandler til ${nySaksbehandler}\n"
        }
    }

    private fun validerKanSettePåVent(
        behandling: Behandling,
    ) {
        brukerfeilHvis(behandling.status.behandlingErLåstForVidereRedigering()) {
            "Kan ikke sette behandling med status ${behandling.status} på vent"
        }
        brukerfeilHvis(!tilordnetRessursService.tilordnetRessursErInnloggetSaksbehandler(behandling.id)) {
            "Behandlingen har en ny eier og kan derfor ikke settes på vent av deg"
        }

        validerHarBehandleSakOppgave(behandling)
    }

    private fun validerKanOppretteVurderHenvendelseOppgave(
        saksbehandling: Saksbehandling,
        vurderHenvendelseOppgaver: List<OppgaveSubtype>,
    ) {
        if (vurderHenvendelseOppgaver.contains(OppgaveSubtype.INFORMERE_OM_SØKT_OVERGANGSSTØNAD)) {
            feilHvis(saksbehandling.stønadstype != StønadType.OVERGANGSSTØNAD) {
                "Kan ikke lagre task for opprettelse av oppgave om informering om søkt overgangsstønad  på behandling med id ${saksbehandling.id} fordi behandlingen ikke er tilknyttet overgangsstønad"
            }
        }

        if (vurderHenvendelseOppgaver.contains(OppgaveSubtype.INNSTILLING_VEDRØRENDE_UTDANNING)) {
            feilHvis(saksbehandling.stønadstype == StønadType.BARNETILSYN) {
                "Kan ikke lagre task for opprettelse av oppgave om innstilling om utdanning på behandling med id ${saksbehandling.id} fordi behandlingen hverken er tilknyttet overgangsstønad eller skolepenger"
            }
        }
    }

    @Transactional
    fun taAvVent(behandlingId: UUID) {
        val behandling = behandlingService.hentBehandling(behandlingId)
        val kanTaAvVent = kanTaAvVent(behandling)
        behandlingService.oppdaterStatusPåBehandling(behandlingId, BehandlingStatus.UTREDES)
        opprettHistorikkInnslag(behandling, StegUtfall.TATT_AV_VENT)
        when (kanTaAvVent.status) {
            TaAvVentStatus.OK -> {}
            TaAvVentStatus.ANNEN_BEHANDLING_MÅ_FERDIGSTILLES ->
                throw ApiFeil(
                    "Annen behandling må ferdigstilles før denne kan aktiveres på nytt",
                    HttpStatus.BAD_REQUEST,
                )

            TaAvVentStatus.MÅ_NULSTILLE_VEDTAK -> {
                val nyForrigeBehandlingId = kanTaAvVent.nyForrigeBehandlingId ?: error("Mangler nyForrigeBehandlingId")
                behandlingService.oppdaterForrigeBehandlingId(behandlingId, nyForrigeBehandlingId)
                nullstillVedtakService.nullstillVedtak(behandlingId)
            }
        }
        fordelOppgaveTilSaksbehandler(behandlingId)
        taskService.save(BehandlingsstatistikkTask.opprettPåbegyntTask(behandlingId))
    }

    private fun fordelOppgaveTilSaksbehandler(behandlingId: UUID) {
        val oppgave = tilordnetRessursService.hentIkkeFerdigstiltOppgaveForBehandling(behandlingId)
        val oppgaveId = oppgave?.id
        if (oppgaveId != null) {
            oppgaveService.fordelOppgave(oppgaveId, SikkerhetContext.hentSaksbehandler(), oppgave.versjon, SikkerhetContext.hentSaksbehandler())
        } else {
            logger.warn("Finner ingen oppgave å oppdatere")
        }
    }

    private fun opprettHistorikkInnslag(
        behandling: Behandling,
        stegUtfall: StegUtfall,
    ) {
        behandlingshistorikkService.opprettHistorikkInnslag(behandling.id, behandling.steg, stegUtfall, null)
    }

    fun kanTaAvVent(behandlingId: UUID): TaAvVentStatusDto {
        val behandling = behandlingService.hentBehandling(behandlingId)
        return kanTaAvVent(behandling)
    }

    private fun kanTaAvVent(behandling: Behandling): TaAvVentStatusDto {
        brukerfeilHvis(behandling.status != SATT_PÅ_VENT) {
            "Kan ikke ta behandling med status ${behandling.status} av vent"
        }

        val behandlinger = behandlingService.hentBehandlinger(behandling.fagsakId)
        if (behandlinger.any { it.id != behandling.id && it.status != SATT_PÅ_VENT && !it.erAvsluttet() }) {
            return TaAvVentStatusDto(TaAvVentStatus.ANNEN_BEHANDLING_MÅ_FERDIGSTILLES)
        }
        val sisteIverksatte = behandlingService.finnSisteIverksatteBehandling(behandling.fagsakId)
        return if (sisteIverksatte == null || sisteIverksatte.id == behandling.forrigeBehandlingId) {
            TaAvVentStatusDto(TaAvVentStatus.OK)
        } else {
            TaAvVentStatusDto(TaAvVentStatus.MÅ_NULSTILLE_VEDTAK, sisteIverksatte.id)
        }
    }

    private fun validerHarBehandleSakOppgave(behandling: Behandling) {
        val saksbehandling = behandlingService.hentSaksbehandling(behandling.id)

        val harIkkeBehandleSakOppgave =
            oppgaveService.hentOppgaveSomIkkeErFerdigstilt(
                oppgavetype = Oppgavetype.BehandleSak,
                saksbehandling = saksbehandling,
            ) == null

        val harIkkeBehandleUnderkjentVedtakOppgave =
            oppgaveService.hentOppgaveSomIkkeErFerdigstilt(
                oppgavetype = Oppgavetype.BehandleUnderkjentVedtak,
                saksbehandling = saksbehandling,
            ) == null

        if (harIkkeBehandleSakOppgave && harIkkeBehandleUnderkjentVedtakOppgave) {
            throw ApiFeil(
                feil = "Systemet har ikke rukket å opprette behandle sak oppgave enda. Prøv igjen om litt.",
                httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
            )
        }
    }
}
