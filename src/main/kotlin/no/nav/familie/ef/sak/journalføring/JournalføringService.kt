package no.nav.familie.ef.sak.journalføring

import no.nav.familie.ef.sak.barn.BarnService
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.behandling.migrering.InfotrygdPeriodeValideringService
import no.nav.familie.ef.sak.behandlingsflyt.task.BehandlingsstatistikkTask
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.infrastruktur.exception.ApiFeil
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvisIkke
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.iverksett.IverksettService
import no.nav.familie.ef.sak.journalføring.dto.BarnSomSkalFødes
import no.nav.familie.ef.sak.journalføring.dto.DokumentVariantformat
import no.nav.familie.ef.sak.journalføring.dto.JournalføringRequest
import no.nav.familie.ef.sak.journalføring.dto.JournalføringTilNyBehandlingRequest
import no.nav.familie.ef.sak.journalføring.dto.UstrukturertDokumentasjonType
import no.nav.familie.ef.sak.journalføring.dto.VilkårsbehandleNyeBarn
import no.nav.familie.ef.sak.journalføring.dto.skalJournalførePåEksisterendeBehandling
import no.nav.familie.ef.sak.journalføring.dto.valider
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PdlClient
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadService
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.ef.sak.DokumentBrevkode
import no.nav.familie.kontrakter.ef.søknad.SøknadBarnetilsyn
import no.nav.familie.kontrakter.ef.søknad.SøknadOvergangsstønad
import no.nav.familie.kontrakter.ef.søknad.SøknadSkolepenger
import no.nav.familie.kontrakter.felles.Behandlingstema
import no.nav.familie.kontrakter.felles.BrukerIdType
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.dokarkiv.DokarkivBruker
import no.nav.familie.kontrakter.felles.dokarkiv.DokumentInfo
import no.nav.familie.kontrakter.felles.dokarkiv.OppdaterJournalpostRequest
import no.nav.familie.kontrakter.felles.dokarkiv.Sak
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.kontrakter.felles.journalpost.Bruker
import no.nav.familie.kontrakter.felles.journalpost.Dokumentvariant
import no.nav.familie.kontrakter.felles.journalpost.Dokumentvariantformat
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.familie.kontrakter.felles.journalpost.JournalposterForBrukerRequest
import no.nav.familie.kontrakter.felles.journalpost.Journalposttype
import no.nav.familie.kontrakter.felles.journalpost.Journalstatus
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class JournalføringService(
    private val journalpostClient: JournalpostClient,
    private val behandlingService: BehandlingService,
    private val søknadService: SøknadService,
    private val fagsakService: FagsakService,
    private val pdlClient: PdlClient,
    private val grunnlagsdataService: GrunnlagsdataService,
    private val iverksettService: IverksettService,
    private val taskRepository: TaskRepository,
    private val barnService: BarnService,
    private val oppgaveService: OppgaveService,
    private val featureToggleService: FeatureToggleService,
    private val infotrygdPeriodeValideringService: InfotrygdPeriodeValideringService
) {

    fun hentJournalpost(journalpostId: String): Journalpost {
        return journalpostClient.hentJournalpost(journalpostId)
    }

    fun finnJournalposter(
        personIdent: String,
        antall: Int = 20,
        typer: List<Journalposttype> = Journalposttype.values().toList()
    ): List<Journalpost> {
        return journalpostClient.finnJournalposter(
            JournalposterForBrukerRequest(
                brukerId = Bruker(
                    id = personIdent,
                    type = BrukerIdType.FNR
                ),
                antall = antall,
                tema = listOf(Tema.ENF),
                journalposttype = typer
            )
        )
    }

    fun hentDokument(
        journalpostId: String,
        dokumentInfoId: String,
        dokumentVariantformat: DokumentVariantformat = DokumentVariantformat.ARKIV
    ): ByteArray {
        return journalpostClient.hentDokument(journalpostId, dokumentInfoId, dokumentVariantformat)
    }

    @Transactional
    fun fullførJournalpost(journalføringRequest: JournalføringRequest, journalpostId: String): Long {
        journalføringRequest.valider()
        val journalpost = hentJournalpost(journalpostId)
        brukerfeilHvis(journalpost.avsenderMottaker == null) {
            "Avsender mangler og må settes på journalposten i gosys. " +
                "Når endringene er gjort, trykker du på \"Lagre utkast\" før du går tilbake til EF Sak og journalfører."
        }

        return if (journalføringRequest.skalJournalførePåEksisterendeBehandling()) {
            journalførSøknadTilEksisterendeBehandling(journalføringRequest, journalpost)
        } else {
            journalførSøknadTilNyBehandling(journalføringRequest, journalpost)
        }
    }

    private fun journalførSøknadTilEksisterendeBehandling(
        journalføringRequest: JournalføringRequest,
        journalpost: Journalpost
    ): Long {
        val saksbehandler = SikkerhetContext.hentSaksbehandler(true)
        val behandling: Behandling = hentBehandling(journalføringRequest)
        val fagsak = fagsakService.fagsakMedOppdatertPersonIdent(journalføringRequest.fagsakId)
        knyttJournalpostTilBehandling(journalpost, behandling)
        if (journalpost.journalstatus != Journalstatus.JOURNALFOERT) {
            oppdaterJournalpost(journalpost, journalføringRequest.dokumentTitler, fagsak.eksternId.id, saksbehandler)
            ferdigstillJournalføring(journalpost.journalpostId, journalføringRequest.journalførendeEnhet, saksbehandler)
        }
        ferdigstillJournalføringsoppgave(journalføringRequest)
        return journalføringRequest.oppgaveId.toLong()
    }

    private fun journalførSøknadTilNyBehandling(
        journalføringRequest: JournalføringRequest,
        journalpost: Journalpost
    ): Long {
        val saksbehandler = SikkerhetContext.hentSaksbehandler(true)
        val behandlingstype = journalføringRequest.behandling.behandlingstype
            ?: throw ApiFeil("Kan ikke journalføre til ny behandling uten behandlingstype", BAD_REQUEST)
        val fagsak = fagsakService.hentFagsak(journalføringRequest.fagsakId)
        validerJournalføringNyBehandling(journalpost, journalføringRequest)

        validerStateIInfotrygdHvisManIkkeHarBehandlingFraFør(fagsak)

        val behandling = opprettBehandlingOgPopulerGrunnlagsdata(
            behandlingstype = behandlingstype,
            fagsak = fagsak,
            journalpost = journalpost,
            barnSomSkalFødes = journalføringRequest.barnSomSkalFødes,
            ustrukturertDokumentasjonType = journalføringRequest.behandling.ustrukturertDokumentasjonType,
            vilkårsbehandleNyeBarn = journalføringRequest.vilkårsbehandleNyeBarn
        )

        if (journalpost.journalstatus != Journalstatus.JOURNALFOERT) {
            oppdaterJournalpost(journalpost, journalføringRequest.dokumentTitler, fagsak.eksternId.id, saksbehandler)
            ferdigstillJournalføring(journalpost.journalpostId, journalføringRequest.journalførendeEnhet, saksbehandler)
        }

        ferdigstillJournalføringsoppgave(journalføringRequest)
        opprettBehandlingsstatistikkTask(behandling.id, journalføringRequest.oppgaveId.toLong())

        return opprettSaksbehandlingsoppgave(behandling, saksbehandler)
    }

    private fun validerJournalføringNyBehandling(
        journalpost: Journalpost,
        journalføringRequest: JournalføringRequest
    ) {
        val ustrukturertDokumentasjonType = journalføringRequest.behandling.ustrukturertDokumentasjonType
        feilHvis(
            journalpost.harStrukturertSøknad() &&
                ustrukturertDokumentasjonType != UstrukturertDokumentasjonType.IKKE_VALGT
        ) {
            "Kan ikke sende inn ustrukturertDokumentasjonType når journalposten har strukturert søknad"
        }
        brukerfeilHvis(
            !journalpost.harStrukturertSøknad() &&
                ustrukturertDokumentasjonType == UstrukturertDokumentasjonType.IKKE_VALGT
        ) {
            "Må sende inn behandlingsårsak når journalposten mangler digital søknad"
        }
        feilHvis(journalpost.harStrukturertSøknad() && journalføringRequest.vilkårsbehandleNyeBarn != VilkårsbehandleNyeBarn.IKKE_VALGT) {
            "Kan ikke velge å vilkårsbehandle nye barn når man har strukturert søknad"
        }
        brukerfeilHvis(
            ustrukturertDokumentasjonType == UstrukturertDokumentasjonType.ETTERSENDING &&
                journalføringRequest.behandling.behandlingstype != BehandlingType.REVURDERING
        ) {
            "Kan ikke journalføre ettersending på ny førstegangsbehandling"
        }
    }

    @Transactional
    fun opprettBehandlingMedSøknadsdataFraEnFerdigstiltJournalpost(
        journalføringRequest: JournalføringTilNyBehandlingRequest,
        journalpostId: String
    ): Long {
        val saksbehandler = SikkerhetContext.hentSaksbehandler(true)
        val journalpost = hentJournalpost(journalpostId)
        brukerfeilHvisIkke(journalpost.journalstatus == Journalstatus.JOURNALFOERT || journalpost.journalstatus == Journalstatus.FERDIGSTILT) {
            "Denne journalposten er ikke journalført og skal håndteres på vanlig måte"
        }
        val fagsak = fagsakService.hentFagsak(journalføringRequest.fagsakId)
        validerStateIInfotrygdHvisManIkkeHarBehandlingFraFør(fagsak)

        val behandling = opprettBehandlingOgPopulerGrunnlagsdata(
            behandlingstype = journalføringRequest.behandlingstype,
            fagsak = fagsak,
            journalpost = journalpost,
            barnSomSkalFødes = emptyList()
        )

        opprettBehandlingsstatistikkTask(behandling.id)

        return opprettSaksbehandlingsoppgave(behandling, saksbehandler)
    }

    private fun opprettBehandlingOgPopulerGrunnlagsdata(
        behandlingstype: BehandlingType,
        fagsak: Fagsak,
        journalpost: Journalpost,
        barnSomSkalFødes: List<BarnSomSkalFødes>,
        ustrukturertDokumentasjonType: UstrukturertDokumentasjonType = UstrukturertDokumentasjonType.IKKE_VALGT,
        vilkårsbehandleNyeBarn: VilkårsbehandleNyeBarn = VilkårsbehandleNyeBarn.IKKE_VALGT
    ): Behandling {

        val behandlingsårsak = if (journalpost.harStrukturertSøknad()) {
            BehandlingÅrsak.SØKNAD
        } else {
            ustrukturertDokumentasjonType.behandlingÅrsak()
        }
        val behandling = behandlingService.opprettBehandling(
            behandlingType = behandlingstype,
            fagsakId = fagsak.id,
            behandlingsårsak = behandlingsårsak
        )
        iverksettService.startBehandling(behandling, fagsak)
        if (journalpost.harStrukturertSøknad()) {
            settSøknadPåBehandling(journalpost.journalpostId, fagsak, behandling.id)
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
            vilkårsbehandleNyeBarn = vilkårsbehandleNyeBarn
        )
        return behandling
    }

    private fun validerStateIInfotrygdHvisManIkkeHarBehandlingFraFør(fagsak: Fagsak) {
        if (!behandlingService.finnesBehandlingForFagsak(fagsak.id)) {
            when (fagsak.stønadstype) {
                StønadType.OVERGANGSSTØNAD ->
                    infotrygdPeriodeValideringService.validerKanJournalføreUtenÅMigrereOvergangsstønad(
                        fagsak.hentAktivIdent(),
                        fagsak.stønadstype
                    )
                StønadType.BARNETILSYN, StønadType.SKOLEPENGER ->
                    infotrygdPeriodeValideringService.validerHarIkkeÅpenSakIInfotrygd(fagsak)
            }
        }
    }

    private fun opprettBehandlingsstatistikkTask(behandlingId: UUID, oppgaveId: Long? = null) {
        taskRepository.save(BehandlingsstatistikkTask.opprettMottattTask(behandlingId = behandlingId, oppgaveId = oppgaveId))
    }

    fun hentSøknadFraJournalpostForOvergangsstønad(journalpostId: String): SøknadOvergangsstønad {
        val dokumentinfo = hentOriginaldokument(journalpostId, DokumentBrevkode.OVERGANGSSTØNAD)
        return journalpostClient.hentOvergangsstønadSøknad(journalpostId, dokumentinfo.dokumentInfoId)
    }

    fun hentSøknadFraJournalpostForBarnetilsyn(journalpostId: String): SøknadBarnetilsyn {
        val dokumentinfo = hentOriginaldokument(journalpostId, DokumentBrevkode.BARNETILSYN)
        return journalpostClient.hentBarnetilsynSøknad(journalpostId, dokumentinfo.dokumentInfoId)
    }

    fun hentSøknadFraJournalpostForSkolepenger(journalpostId: String): SøknadSkolepenger {
        val dokumentinfo = hentOriginaldokument(journalpostId, DokumentBrevkode.SKOLEPENGER)
        return journalpostClient.hentSkolepengerSøknad(journalpostId, dokumentinfo.dokumentInfoId)
    }

    private fun hentOriginaldokument(
        journalpostId: String,
        dokumentBrevkode: DokumentBrevkode
    ): no.nav.familie.kontrakter.felles.journalpost.DokumentInfo {
        val dokumenter = hentJournalpost(journalpostId).dokumenter ?: error("Fant ingen dokumenter på journalposten")
        return dokumenter.firstOrNull {
            DokumentBrevkode.erGyldigBrevkode(it.brevkode.toString()) &&
                dokumentBrevkode == DokumentBrevkode.fraBrevkode(it.brevkode.toString()) &&
                harOriginalDokument(it)
        } ?: throw ApiFeil("Det finnes ingen søknad i journalposten for å opprette en ny behandling", BAD_REQUEST)
    }

    private fun ferdigstillJournalføring(journalpostId: String, journalførendeEnhet: String, saksbehandler: String) {
        journalpostClient.ferdigstillJournalpost(journalpostId, journalførendeEnhet, saksbehandler)
    }

    private fun opprettSaksbehandlingsoppgave(behandling: Behandling, navIdent: String): Long {
        return oppgaveService.opprettOppgave(
            behandlingId = behandling.id,
            oppgavetype = Oppgavetype.BehandleSak,
            tilordnetNavIdent = navIdent
        )
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
            behandling.id
        )
    }

    private fun settSøknadPåBehandling(journalpostId: String, fagsak: Fagsak, behandlingId: UUID) {
        when (fagsak.stønadstype) {
            StønadType.OVERGANGSSTØNAD -> {
                val søknad = hentSøknadFraJournalpostForOvergangsstønad(journalpostId)
                søknadService.lagreSøknadForOvergangsstønad(søknad, behandlingId, fagsak.id, journalpostId)
            }
            StønadType.BARNETILSYN -> {
                val søknad = hentSøknadFraJournalpostForBarnetilsyn(journalpostId)
                søknadService.lagreSøknadForBarnetilsyn(søknad, behandlingId, fagsak.id, journalpostId)
            }
            StønadType.SKOLEPENGER -> {
                val søknad = hentSøknadFraJournalpostForSkolepenger(journalpostId)
                søknadService.lagreSøknadForSkolepenger(søknad, behandlingId, fagsak.id, journalpostId)
            }
        }
    }

    private fun harOriginalDokument(dokument: no.nav.familie.kontrakter.felles.journalpost.DokumentInfo): Boolean =
        dokument.dokumentvarianter?.contains(Dokumentvariant(variantformat = Dokumentvariantformat.ORIGINAL))
            ?: false

    private fun oppdaterJournalpost(
        journalpost: Journalpost,
        dokumenttitler: Map<String, String>?,
        eksternFagsakId: Long,
        saksbehandler: String
    ) {
        val oppdatertJournalpost =
            OppdaterJournalpostRequest(
                bruker = journalpost.bruker?.let {
                    DokarkivBruker(idType = BrukerIdType.valueOf(it.type.toString()), id = it.id)
                },
                tema = journalpost.tema?.let { Tema.valueOf(it) },
                behandlingstema = journalpost.behandlingstema?.let { Behandlingstema.fromValue(it) },
                tittel = journalpost.tittel,
                journalfoerendeEnhet = journalpost.journalforendeEnhet,
                sak = Sak(
                    fagsakId = eksternFagsakId.toString(),
                    fagsaksystem = Fagsystem.EF,
                    sakstype = "FAGSAK"
                ),
                dokumenter = dokumenttitler?.let {
                    journalpost.dokumenter?.map { dokumentInfo ->
                        DokumentInfo(
                            dokumentInfoId = dokumentInfo.dokumentInfoId,
                            tittel = dokumenttitler[dokumentInfo.dokumentInfoId]
                                ?: dokumentInfo.tittel,
                            brevkode = dokumentInfo.brevkode
                        )
                    }
                }
            )
        journalpostClient.oppdaterJournalpost(oppdatertJournalpost, journalpost.journalpostId, saksbehandler)
    }
}
