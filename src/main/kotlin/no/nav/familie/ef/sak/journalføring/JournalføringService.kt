package no.nav.familie.ef.sak.journalføring

import no.nav.familie.ef.sak.barn.BarnService
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.behandling.migrering.InfotrygdPeriodeValideringService
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType
import no.nav.familie.ef.sak.behandlingsflyt.task.BehandlingsstatistikkTask
import no.nav.familie.ef.sak.behandlingsflyt.task.OpprettOppgaveForOpprettetBehandlingTask
import no.nav.familie.ef.sak.behandlingsflyt.task.OpprettOppgaveForOpprettetBehandlingTask.OpprettOppgaveTaskData
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.infrastruktur.exception.ApiFeil
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvisIkke
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.iverksett.IverksettService
import no.nav.familie.ef.sak.journalføring.JournalføringHelper.validerJournalføringNyBehandling
import no.nav.familie.ef.sak.journalføring.JournalføringHelper.validerMottakerFinnes
import no.nav.familie.ef.sak.journalføring.dto.BarnSomSkalFødes
import no.nav.familie.ef.sak.journalføring.dto.JournalføringRequest
import no.nav.familie.ef.sak.journalføring.dto.JournalføringTilNyBehandlingRequest
import no.nav.familie.ef.sak.journalføring.dto.UstrukturertDokumentasjonType
import no.nav.familie.ef.sak.journalføring.dto.VilkårsbehandleNyeBarn
import no.nav.familie.ef.sak.journalføring.dto.skalJournalførePåEksisterendeBehandling
import no.nav.familie.ef.sak.journalføring.dto.valider
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataService
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadService
import no.nav.familie.ef.sak.vilkår.VurderingService
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.ef.journalføring.AutomatiskJournalføringResponse
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.familie.kontrakter.felles.journalpost.Journalstatus
import no.nav.familie.kontrakter.felles.oppgave.OppgavePrioritet
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.internal.TaskService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class JournalføringService(
    private val behandlingService: BehandlingService,
    private val søknadService: SøknadService,
    private val fagsakService: FagsakService,
    private val vurderingService: VurderingService,
    private val grunnlagsdataService: GrunnlagsdataService,
    private val iverksettService: IverksettService,
    private val taskService: TaskService,
    private val barnService: BarnService,
    private val oppgaveService: OppgaveService,
    private val journalpostService: JournalpostService,
    private val infotrygdPeriodeValideringService: InfotrygdPeriodeValideringService,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun fullførJournalpost(journalføringRequest: JournalføringRequest, journalpostId: String): Long {
        journalføringRequest.valider()
        val journalpost = journalpostService.hentJournalpost(journalpostId)
        validerMottakerFinnes(journalpost)

        return if (journalføringRequest.skalJournalførePåEksisterendeBehandling()) {
            journalførSøknadTilEksisterendeBehandling(journalføringRequest, journalpost)
        } else {
            journalførSøknadTilNyBehandling(journalføringRequest, journalpost)
        }
    }

    private fun journalførSøknadTilEksisterendeBehandling(
        journalføringRequest: JournalføringRequest,
        journalpost: Journalpost,
    ): Long {
        val saksbehandler = SikkerhetContext.hentSaksbehandler()
        val behandling: Behandling = hentBehandling(journalføringRequest)
        val fagsak = fagsakService.fagsakMedOppdatertPersonIdent(journalføringRequest.fagsakId)
        logger.info(
            "Journalfører journalpost=${journalpost.journalpostId} på eksisterende behandling=${behandling.id} på " +
                "fagsak=${fagsak.id} stønadstype=${fagsak.stønadstype} ",
        )
        knyttJournalpostTilBehandling(journalpost, behandling)
        journalpostService.oppdaterOgFerdigstillJournalpost(
            journalpost = journalpost,
            dokumenttitler = journalføringRequest.dokumentTitler,
            journalførendeEnhet = journalføringRequest.journalførendeEnhet,
            fagsak = fagsak,
            saksbehandler = saksbehandler,
        )
        ferdigstillJournalføringsoppgave(journalføringRequest)
        return journalføringRequest.oppgaveId.toLong()
    }

    private fun journalførSøknadTilNyBehandling(
        journalføringRequest: JournalføringRequest,
        journalpost: Journalpost,
    ): Long {
        val saksbehandler = SikkerhetContext.hentSaksbehandler()
        val behandlingstype = journalføringRequest.behandling.behandlingstype
            ?: throw ApiFeil("Kan ikke journalføre til ny behandling uten behandlingstype", BAD_REQUEST)
        val fagsak = fagsakService.fagsakMedOppdatertPersonIdent(journalføringRequest.fagsakId)
        logger.info(
            "Journalfører journalpost=${journalpost.journalpostId} på ny behandling på " +
                "fagsak=${fagsak.id} stønadstype=${fagsak.stønadstype} " +
                " vilkårsbehandleNyeBarn=${journalføringRequest.vilkårsbehandleNyeBarn} " +
                " dokumentasjonstype=${journalføringRequest.behandling.ustrukturertDokumentasjonType}",
        )
        validerJournalføringNyBehandling(
            journalpost,
            journalføringRequest,
        )

        infotrygdPeriodeValideringService.validerKanOppretteBehandlingGittInfotrygdData(fagsak)

        val behandling = opprettBehandlingOgPopulerGrunnlagsdata(
            behandlingstype = behandlingstype,
            fagsak = fagsak,
            journalpost = journalpost,
            barnSomSkalFødes = journalføringRequest.barnSomSkalFødes,
            ustrukturertDokumentasjonType = journalføringRequest.behandling.ustrukturertDokumentasjonType,
            vilkårsbehandleNyeBarn = journalføringRequest.vilkårsbehandleNyeBarn,
        )

        journalpostService.oppdaterOgFerdigstillJournalpost(
            journalpost = journalpost,
            dokumenttitler = journalføringRequest.dokumentTitler,
            journalførendeEnhet = journalføringRequest.journalførendeEnhet,
            fagsak = fagsak,
            saksbehandler = saksbehandler,
        )

        ferdigstillJournalføringsoppgave(journalføringRequest)
        opprettBehandlingsstatistikkTask(behandling.id, journalføringRequest.oppgaveId.toLong())

        return opprettBehandleSakOppgave(behandling, saksbehandler)
    }

    @Transactional
    fun automatiskJournalfør(
        fagsak: Fagsak,
        journalpost: Journalpost,
        journalførendeEnhet: String,
        mappeId: Long?,
        behandlingstype: BehandlingType,
        prioritet: OppgavePrioritet,
    ): AutomatiskJournalføringResponse {
        val behandling = opprettBehandlingOgPopulerGrunnlagsdata(
            behandlingstype = behandlingstype,
            fagsak = fagsak,
            journalpost = journalpost,
            barnSomSkalFødes = emptyList(),
        )

        journalpostService.oppdaterOgFerdigstillJournalpostMaskinelt(
            journalpost = journalpost,
            journalførendeEnhet = journalførendeEnhet,
            fagsak = fagsak,
        )

        opprettBehandleSakOppgaveTask(
            OpprettOppgaveTaskData(
                behandlingId = behandling.id,
                saksbehandler = SikkerhetContext.hentSaksbehandlerEllerSystembruker(),
                beskrivelse = AUTOMATISK_JOURNALFØRING_BESKRIVELSE,
                mappeId = mappeId,
                prioritet = prioritet,
            ),
        )
        return AutomatiskJournalføringResponse(
            fagsakId = fagsak.id,
            behandlingId = behandling.id,
        )
    }

    @Transactional
    fun opprettBehandlingMedSøknadsdataFraEnFerdigstiltJournalpost(
        journalføringRequest: JournalføringTilNyBehandlingRequest,
        journalpostId: String,
    ): Long {
        val saksbehandler = SikkerhetContext.hentSaksbehandler()
        val journalpost = journalpostService.hentJournalpost(journalpostId)
        brukerfeilHvisIkke(journalpost.journalstatus == Journalstatus.JOURNALFOERT || journalpost.journalstatus == Journalstatus.FERDIGSTILT) {
            "Denne journalposten er ikke journalført og skal håndteres på vanlig måte"
        }
        val fagsak = fagsakService.hentFagsak(journalføringRequest.fagsakId)
        logger.info(
            "Journalfører ferdigstilt journalpost=${journalpost.journalpostId} på ny behandling på " +
                "fagsak=${fagsak.id} stønadstype=${fagsak.stønadstype} ",
        )
        infotrygdPeriodeValideringService.validerKanOppretteBehandlingGittInfotrygdData(fagsak)

        val behandling = opprettBehandlingOgPopulerGrunnlagsdata(
            behandlingstype = journalføringRequest.behandlingstype,
            fagsak = fagsak,
            journalpost = journalpost,
            barnSomSkalFødes = emptyList(),
        )

        opprettBehandlingsstatistikkTask(behandling.id)

        return opprettBehandleSakOppgave(behandling, saksbehandler)
    }

    private fun opprettBehandlingOgPopulerGrunnlagsdata(
        behandlingstype: BehandlingType,
        fagsak: Fagsak,
        journalpost: Journalpost,
        barnSomSkalFødes: List<BarnSomSkalFødes>,
        ustrukturertDokumentasjonType: UstrukturertDokumentasjonType = UstrukturertDokumentasjonType.IKKE_VALGT,
        vilkårsbehandleNyeBarn: VilkårsbehandleNyeBarn = VilkårsbehandleNyeBarn.IKKE_VALGT,
    ): Behandling {
        val behandling = behandlingService.opprettBehandling(
            behandlingType = behandlingstype,
            fagsakId = fagsak.id,
            behandlingsårsak = utledBehandlingÅrsak(journalpost, ustrukturertDokumentasjonType),
        )
        iverksettService.startBehandling(behandling, fagsak)
        if (journalpost.harStrukturertSøknad()) {
            settSøknadPåBehandling(journalpost, fagsak, behandling.id)
        }
        knyttJournalpostTilBehandling(journalpost, behandling)
        val grunnlagsdata = grunnlagsdataService.opprettGrunnlagsdata(behandling.id)
        barnService.opprettBarnPåBehandlingMedSøknadsdata(
            behandlingId = behandling.id,
            fagsakId = fagsak.id,
            grunnlagsdataBarn = grunnlagsdata.grunnlagsdata.barn,
            stønadstype = fagsak.stønadstype,
            ustrukturertDokumentasjonType = ustrukturertDokumentasjonType,
            barnSomSkalFødes = barnSomSkalFødes,
            vilkårsbehandleNyeBarn = vilkårsbehandleNyeBarn,
        )
        val forrigeBehandling = finnForrigeIverksatteEllerAvslåtteBehandling(fagsak)
        if (skalKopiereVurderingerTilNyBehandling(ustrukturertDokumentasjonType)) {
            forrigeBehandling?.let { kopierVurderingerOgOppdaterBehandling(behandling, it.id, fagsak) }
        }
        return behandling
    }

    private fun utledBehandlingÅrsak(
        journalpost: Journalpost,
        ustrukturertDokumentasjonType: UstrukturertDokumentasjonType,
    ) = if (journalpost.harStrukturertSøknad()) {
        BehandlingÅrsak.SØKNAD
    } else {
        ustrukturertDokumentasjonType.behandlingÅrsak()
    }

    private fun kopierVurderingerOgOppdaterBehandling(
        behandling: Behandling,
        forrigeBehandlingId: UUID,
        fagsak: Fagsak,
    ) {
        val (_, metadata) = vurderingService.hentGrunnlagOgMetadata(behandling.id)
        vurderingService.kopierVurderingerTilNyBehandling(
            eksisterendeBehandlingId = forrigeBehandlingId,
            nyBehandlingsId = behandling.id,
            metadata = metadata,
            stønadType = fagsak.stønadstype,
            fagsakPersonId = fagsak.fagsakPersonId
        )
        behandlingService.oppdaterStatusPåBehandling(behandling.id, BehandlingStatus.UTREDES)
        behandlingService.oppdaterStegPåBehandling(behandling.id, StegType.BEREGNE_YTELSE)
    }

    private fun skalKopiereVurderingerTilNyBehandling(
        ustrukturertDokumentasjonType: UstrukturertDokumentasjonType,
    ): Boolean = ustrukturertDokumentasjonType.erEttersending()

    private fun finnForrigeIverksatteEllerAvslåtteBehandling(fagsak: Fagsak) =
        behandlingService.finnSisteIverksatteBehandlingMedEventuellAvslått(fagsak.id)

    private fun opprettBehandlingsstatistikkTask(behandlingId: UUID, oppgaveId: Long? = null) {
        taskService.save(
            BehandlingsstatistikkTask.opprettMottattTask(
                behandlingId = behandlingId,
                oppgaveId = oppgaveId,
            ),
        )
    }

    private fun opprettBehandleSakOppgave(behandling: Behandling, navIdent: String): Long {
        return oppgaveService.opprettOppgave(
            behandlingId = behandling.id,
            oppgavetype = Oppgavetype.BehandleSak,
            tilordnetNavIdent = navIdent,
        )
    }

    private fun opprettBehandleSakOppgaveTask(opprettOppgaveTaskData: OpprettOppgaveTaskData) {
        taskService.save(OpprettOppgaveForOpprettetBehandlingTask.opprettTask(opprettOppgaveTaskData))
    }

    private fun ferdigstillJournalføringsoppgave(journalføringRequest: JournalføringRequest) {
        oppgaveService.ferdigstillOppgave(journalføringRequest.oppgaveId.toLong())
    }

    private fun hentBehandling(journalføringRequest: JournalføringRequest): Behandling =
        hentEksisterendeBehandling(journalføringRequest.behandling.behandlingsId)
            ?: error("Finner ikke behandling med id=${journalføringRequest.behandling.behandlingsId}")

    private fun hentEksisterendeBehandling(behandlingId: UUID?): Behandling? {
        return behandlingId?.let { behandlingService.hentBehandling(it) }
    }

    private fun knyttJournalpostTilBehandling(journalpost: Journalpost, behandling: Behandling) {
        behandlingService.leggTilBehandlingsjournalpost(
            journalpost.journalpostId,
            journalpost.journalposttype,
            behandling.id,
        )
    }

    private fun settSøknadPåBehandling(journalpost: Journalpost, fagsak: Fagsak, behandlingId: UUID) {
        when (fagsak.stønadstype) {
            StønadType.OVERGANGSSTØNAD -> {
                val søknad = journalpostService.hentSøknadFraJournalpostForOvergangsstønad(journalpost)
                søknadService.lagreSøknadForOvergangsstønad(søknad, behandlingId, fagsak.id, journalpost.journalpostId)
            }

            StønadType.BARNETILSYN -> {
                val søknad = journalpostService.hentSøknadFraJournalpostForBarnetilsyn(journalpost)
                søknadService.lagreSøknadForBarnetilsyn(søknad, behandlingId, fagsak.id, journalpost.journalpostId)
            }

            StønadType.SKOLEPENGER -> {
                val søknad = journalpostService.hentSøknadFraJournalpostForSkolepenger(journalpost)
                søknadService.lagreSøknadForSkolepenger(søknad, behandlingId, fagsak.id, journalpost.journalpostId)
            }
        }
    }

    companion object {
        const val AUTOMATISK_JOURNALFØRING_BESKRIVELSE = "Automatisk journalført"
    }
}
