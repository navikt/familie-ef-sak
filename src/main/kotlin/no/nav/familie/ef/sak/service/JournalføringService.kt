package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.api.journalføring.JournalføringRequest
import no.nav.familie.ef.sak.domene.DokumentVariantformat
import no.nav.familie.ef.sak.integration.JournalpostClient
import no.nav.familie.ef.sak.integration.PdlClient
import no.nav.familie.ef.sak.iverksett.IverksettService
import no.nav.familie.ef.sak.repository.domain.Behandling
import no.nav.familie.ef.sak.repository.domain.BehandlingType
import no.nav.familie.ef.sak.repository.domain.Fagsak
import no.nav.familie.ef.sak.repository.domain.Stønadstype
import no.nav.familie.ef.sak.task.BehandlingsstatistikkTask
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
import no.nav.familie.kontrakter.felles.journalpost.Bruker
import no.nav.familie.kontrakter.felles.journalpost.Dokumentvariant
import no.nav.familie.kontrakter.felles.journalpost.Dokumentvariantformat
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.familie.kontrakter.felles.journalpost.JournalposterForBrukerRequest
import no.nav.familie.kontrakter.felles.journalpost.Journalposttype
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class JournalføringService(private val journalpostClient: JournalpostClient,
                           private val behandlingService: BehandlingService,
                           private val søknadService: SøknadService,
                           private val fagsakService: FagsakService,
                           private val pdlClient: PdlClient,
                           private val grunnlagsdataService: GrunnlagsdataService,
                           private val iverksettService: IverksettService,
                           private val taskRepository: TaskRepository,
                           private val oppgaveService: OppgaveService) {

    fun hentJournalpost(journalpostId: String): Journalpost {
        return journalpostClient.hentJournalpost(journalpostId)
    }

    fun finnJournalposter(personIdent: String,
                          antall: Int = 20,
                          typer: List<Journalposttype> = Journalposttype.values().toList()): List<Journalpost> {
        return journalpostClient.finnJournalposter(JournalposterForBrukerRequest(brukerId = Bruker(id = personIdent,
                                                                                                   type = BrukerIdType.FNR),
                                                                                 antall = antall,
                                                                                 tema = listOf(Tema.ENF),
                                                                                 journalposttype = typer))
    }

    fun hentDokument(journalpostId: String,
                     dokumentInfoId: String,
                     dokumentVariantformat: DokumentVariantformat = DokumentVariantformat.ARKIV): ByteArray {
        return journalpostClient.hentDokument(journalpostId, dokumentInfoId, dokumentVariantformat)
    }

    @Transactional
    fun fullførJournalpost(journalføringRequest: JournalføringRequest, journalpostId: String): Long {
        val behandling: Behandling = hentBehandling(journalføringRequest)
        val journalpost = hentJournalpost(journalpostId)
        val fagsak = fagsakService.hentFagsak(journalføringRequest.fagsakId)

        iverksettService.startBehandling(fagsak)
        settSøknadPåBehandling(journalpostId, fagsak, behandling.id)
        knyttJournalpostTilBehandling(journalpost, behandling)
        grunnlagsdataService.opprettGrunnlagsdata(behandling.id)
        oppdaterJournalpost(journalpost, journalføringRequest.dokumentTitler, fagsak.eksternId.id)
        ferdigstillJournalføring(journalpostId, journalføringRequest.journalførendeEnhet)
        ferdigstillJournalføringsoppgave(journalføringRequest)
        opprettBehandlingsstatistikkTask(behandling.id, journalføringRequest.oppgaveId.toLong())

        return opprettSaksbehandlingsoppgave(behandling, journalføringRequest.navIdent)

    }

    private fun opprettBehandlingsstatistikkTask(behandlingId: UUID, oppgaveId: Long) {
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

    fun hentIdentForJournalpost(journalpost: Journalpost): String {
        return journalpost.bruker?.let {
            when (it.type) {
                BrukerIdType.FNR -> it.id
                BrukerIdType.AKTOERID -> pdlClient.hentPersonidenter(it.id).identer.first().ident
                BrukerIdType.ORGNR -> error("Kan ikke hente journalpost=${journalpost.journalpostId} for orgnr")
            }
        } ?: error("Kan ikke hente journalpost=${journalpost.journalpostId} uten bruker")
    }

    private fun hentOriginaldokument(journalpostId: String,
                                     dokumentBrevkode: DokumentBrevkode): no.nav.familie.kontrakter.felles.journalpost.DokumentInfo {
        return hentJournalpost(journalpostId).dokumenter
                       ?.first {
                           dokumentBrevkode == DokumentBrevkode.fraBrevkode(it.brevkode.toString()) && harOriginalDokument(
                                   it)
                       } ?: error("Fant ingen søknad")
    }

    private fun ferdigstillJournalføring(journalpostId: String, journalførendeEnhet: String) {
        journalpostClient.ferdigstillJournalpost(journalpostId, journalførendeEnhet)
    }

    private fun opprettSaksbehandlingsoppgave(behandling: Behandling, navIdent: String): Long {
        return oppgaveService.opprettOppgave(behandlingId = behandling.id,
                                             oppgavetype = Oppgavetype.BehandleSak,
                                             tilordnetNavIdent = navIdent)
    }

    private fun ferdigstillJournalføringsoppgave(journalføringRequest: JournalføringRequest) {
        oppgaveService.ferdigstillOppgave(journalføringRequest.oppgaveId.toLong())
    }

    private fun hentBehandling(journalføringRequest: JournalføringRequest): Behandling =
            hentEksisterendeBehandling(journalføringRequest.behandling.behandlingsId)
            ?: opprettBehandlingMedBehandlingstype(journalføringRequest.behandling.behandlingstype,
                                                   journalføringRequest.fagsakId)


    private fun opprettBehandlingMedBehandlingstype(behandlingsType: BehandlingType?, fagsakId: UUID): Behandling {
        return behandlingService.opprettBehandling(behandlingType = behandlingsType!!,
                                                   fagsakId = fagsakId)
    }

    private fun hentEksisterendeBehandling(behandlingId: UUID?): Behandling? {
        return behandlingId?.let { behandlingService.hentBehandling(it) }
    }

    private fun knyttJournalpostTilBehandling(journalpost: Journalpost, behandling: Behandling) {
        behandlingService.leggTilBehandlingsjournalpost(journalpost.journalpostId, journalpost.journalposttype, behandling.id)
    }

    private fun settSøknadPåBehandling(journalpostId: String, fagsak: Fagsak, behandlingId: UUID) {
        when (fagsak.stønadstype) {
            Stønadstype.OVERGANGSSTØNAD -> {
                val søknad = hentSøknadFraJournalpostForOvergangsstønad(journalpostId)
                søknadService.lagreSøknadForOvergangsstønad(søknad, behandlingId, fagsak.id, journalpostId)
            }
            Stønadstype.BARNETILSYN -> {
                val søknad = hentSøknadFraJournalpostForBarnetilsyn(journalpostId)
                søknadService.lagreSøknadForBarnetilsyn(søknad, behandlingId, fagsak.id, journalpostId)
            }
            Stønadstype.SKOLEPENGER -> {
                val søknad = hentSøknadFraJournalpostForSkolepenger(journalpostId)
                søknadService.lagreSøknadForSkolepenger(søknad, behandlingId, fagsak.id, journalpostId)
            }
        }
    }

    private fun harOriginalDokument(dokument: no.nav.familie.kontrakter.felles.journalpost.DokumentInfo): Boolean =
            dokument.dokumentvarianter?.contains(Dokumentvariant(variantformat = Dokumentvariantformat.ORIGINAL))
            ?: false

    private fun oppdaterJournalpost(journalpost: Journalpost, dokumenttitler: Map<String, String>?, eksternFagsakId: Long) {
        val oppdatertJournalpost =
                OppdaterJournalpostRequest(bruker = journalpost.bruker?.let {
                    DokarkivBruker(idType = BrukerIdType.valueOf(it.type.toString()), id = it.id)
                },
                                           tema = journalpost.tema?.let { Tema.valueOf(it) }, // TODO: Funker dette?
                                           behandlingstema = journalpost.behandlingstema?.let { Behandlingstema.fromValue(it) },// TODO: Funker dette?
                                           tittel = journalpost.tittel,
                                           journalfoerendeEnhet = journalpost.journalforendeEnhet,
                                           sak = Sak(fagsakId = eksternFagsakId.toString(),
                                                     fagsaksystem = Fagsystem.EF,
                                                     sakstype = "FAGSAK"),
                                           dokumenter = dokumenttitler?.let {
                                               journalpost.dokumenter?.map { dokumentInfo ->
                                                   DokumentInfo(dokumentInfoId = dokumentInfo.dokumentInfoId,
                                                                tittel = dokumenttitler[dokumentInfo.dokumentInfoId]
                                                                         ?: dokumentInfo.tittel,
                                                                brevkode = dokumentInfo.brevkode)
                                               }
                                           })
        journalpostClient.oppdaterJournalpost(oppdatertJournalpost, journalpost.journalpostId)
    }


}
