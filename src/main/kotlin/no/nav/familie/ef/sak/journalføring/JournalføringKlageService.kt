package no.nav.familie.ef.sak.journalføring

import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.journalføring.JournalføringHelper.validerGyldigAvsender
import no.nav.familie.ef.sak.journalføring.JournalføringHelper.validerMottakerFinnes
import no.nav.familie.ef.sak.journalføring.dto.JournalføringKlageRequest
import no.nav.familie.ef.sak.journalføring.dto.JournalføringRequestV2
import no.nav.familie.ef.sak.journalføring.dto.Journalføringsårsak
import no.nav.familie.ef.sak.journalføring.dto.skalJournalførePåEksisterendeBehandling
import no.nav.familie.ef.sak.klage.KlageService
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.familie.kontrakter.felles.klage.KlagebehandlingDto
import no.nav.familie.prosessering.internal.TaskService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class JournalføringKlageService(
    private val fagsakService: FagsakService,
    private val oppgaveService: OppgaveService,
    private val journalpostService: JournalpostService,
    private val klageService: KlageService,
    private val taskService: TaskService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Deprecated("Bruk V2")
    @Transactional
    fun fullførJournalpost(
        journalføringRequest: JournalføringKlageRequest,
        journalpostId: String,
    ) {
        val journalpost = journalpostService.hentJournalpost(journalpostId)
        validerMottakerFinnes(journalpost)

        return if (journalføringRequest.skalJournalførePåEksisterendeBehandling()) {
            journalførKlageTilEksisterendeBehandling(journalføringRequest, journalpost)
        } else {
            journalførKlageTilNyBehandling(journalføringRequest, journalpost)
        }
    }

    @Transactional
    fun fullførJournalpostV2(
        journalføringRequest: JournalføringRequestV2,
        journalpost: Journalpost,
    ) {
        validerGyldigAvsender(journalpost, journalføringRequest)

        return if (journalføringRequest.skalJournalføreTilNyBehandling()) {
            journalførKlageTilNyBehandling(journalføringRequest, journalpost)
        } else {
            journalførKlageTilEksisterendeBehandling(journalføringRequest, journalpost)
        }
    }

    @Deprecated("Bruk V2")
    private fun journalførKlageTilEksisterendeBehandling(
        journalføringRequest: JournalføringKlageRequest,
        journalpost: Journalpost,
    ) {
        val behandlingId = journalføringRequest.behandling.behandlingId ?: error("Mangler behandlingId")
        val saksbehandler = SikkerhetContext.hentSaksbehandler()
        val fagsak = fagsakService.fagsakMedOppdatertPersonIdent(journalføringRequest.fagsakId)
        validerKlagebehandlinger(fagsak, behandlingId)

        logger.info(
            "Journalfører journalpost=${journalpost.journalpostId} på eksisterende " +
                "klageBehandling=$behandlingId på " +
                "fagsak=${fagsak.id} stønadstype=${fagsak.stønadstype} ",
        )

        if (journalføringRequest.klageGjelderTilbakekreving) {
            oppdaterOppgaveTilÅGjeldeTilbakekreving(behandlingId)
        }

        journalpostService.oppdaterOgFerdigstillJournalpost(
            journalpost = journalpost,
            dokumenttitler = journalføringRequest.dokumentTitler,
            journalførendeEnhet = journalføringRequest.journalførendeEnhet,
            fagsak = fagsak,
            saksbehandler = saksbehandler,
        )
        ferdigstillJournalføringsoppgave(journalføringRequest.oppgaveId.toLong())
    }

    private fun journalførKlageTilEksisterendeBehandling(
        journalføringRequest: JournalføringRequestV2,
        journalpost: Journalpost,
    ) {
        val saksbehandler = SikkerhetContext.hentSaksbehandler()
        val fagsak = fagsakService.fagsakMedOppdatertPersonIdent(journalføringRequest.fagsakId)
        val nyAvsender =
            JournalføringHelper.utledNyAvsender(journalføringRequest.nyAvsender, journalpost.bruker)

        logger.info(
            "Journalfører journalpost=${journalpost.journalpostId} på eksisterende " +
                "fagsak=${fagsak.id} stønadstype=${fagsak.stønadstype} ",
        )

        journalpostService.oppdaterOgFerdigstillJournalpost(
            journalpost = journalpost,
            dokumenttitler = journalføringRequest.dokumentTitler,
            journalførendeEnhet = journalføringRequest.journalførendeEnhet,
            fagsak = fagsak,
            logiskeVedlegg = journalføringRequest.logiskeVedlegg,
            saksbehandler = saksbehandler,
            nyAvsender = nyAvsender,
        )
        ferdigstillJournalføringsoppgave(journalføringRequest.oppgaveId.toLong())
    }

    @Deprecated("Bruk V2")
    private fun journalførKlageTilNyBehandling(
        journalføringRequest: JournalføringKlageRequest,
        journalpost: Journalpost,
    ) {
        val saksbehandler = SikkerhetContext.hentSaksbehandler()
        val fagsak = fagsakService.fagsakMedOppdatertPersonIdent(journalføringRequest.fagsakId)
        val mottattDato = journalføringRequest.behandling.mottattDato
        logger.info(
            "Journalfører journalpost=${journalpost.journalpostId} på ny klagebehandling på " +
                "fagsak=${fagsak.id} stønadstype=${fagsak.stønadstype} mottattDato=$mottattDato",
        )

        val klageMottatt = mottattDato ?: journalpost.datoMottatt?.toLocalDate()
        feilHvis(klageMottatt == null) {
            "Mangler dato mottatt"
        }

        klageService.opprettKlage(fagsak, klageMottatt, journalføringRequest.klageGjelderTilbakekreving)

        journalpostService.oppdaterOgFerdigstillJournalpost(
            journalpost = journalpost,
            dokumenttitler = journalføringRequest.dokumentTitler,
            journalførendeEnhet = journalføringRequest.journalførendeEnhet,
            fagsak = fagsak,
            saksbehandler = saksbehandler,
        )

        ferdigstillJournalføringsoppgave(journalføringRequest.oppgaveId.toLong())
    }

    private fun journalførKlageTilNyBehandling(
        journalføringRequest: JournalføringRequestV2,
        journalpost: Journalpost,
    ) {
        val saksbehandler = SikkerhetContext.hentSaksbehandler()
        val fagsak = fagsakService.fagsakMedOppdatertPersonIdent(journalføringRequest.fagsakId)
        val mottattDato = journalføringRequest.mottattDato
        val nyAvsender =
            JournalføringHelper.utledNyAvsender(journalføringRequest.nyAvsender, journalpost.bruker)
        logger.info(
            "Journalfører journalpost=${journalpost.journalpostId} på ny klagebehandling på " +
                "fagsak=${fagsak.id} stønadstype=${fagsak.stønadstype} mottattDato=$mottattDato",
        )

        val klageMottatt = mottattDato ?: journalpost.datoMottatt?.toLocalDate()
        feilHvis(klageMottatt == null) {
            "Mangler dato mottatt"
        }

        klageService.opprettKlage(fagsak, klageMottatt, journalføringRequest.årsak == Journalføringsårsak.KLAGE_TILBAKEKREVING)

        journalpostService.oppdaterOgFerdigstillJournalpost(
            journalpost = journalpost,
            dokumenttitler = journalføringRequest.dokumentTitler,
            journalførendeEnhet = journalføringRequest.journalførendeEnhet,
            fagsak = fagsak,
            logiskeVedlegg = journalføringRequest.logiskeVedlegg,
            saksbehandler = saksbehandler,
            nyAvsender = nyAvsender,
        )

        ferdigstillJournalføringsoppgave(journalføringRequest.oppgaveId.toLong())
    }

    private fun validerKlagebehandlinger(
        fagsak: Fagsak,
        behandlingId: UUID,
    ) {
        val klagebehandlinger = hentKlagebehandlinger(fagsak)
        klagebehandlinger.singleOrNull { it.id == behandlingId }
            ?: error("Klagebehandlinger for person=${fagsak.fagsakPersonId} mangler behandlingId=$behandlingId")
    }

    private fun hentKlagebehandlinger(fagsak: Fagsak): List<KlagebehandlingDto> {
        val klagebehandlinger = klageService.hentBehandlinger(fagsak.fagsakPersonId)
        return when (fagsak.stønadstype) {
            StønadType.OVERGANGSSTØNAD -> klagebehandlinger.overgangsstønad
            StønadType.BARNETILSYN -> klagebehandlinger.barnetilsyn
            StønadType.SKOLEPENGER -> klagebehandlinger.skolepenger
        }
    }

    private fun ferdigstillJournalføringsoppgave(oppgaveId: Long) {
        oppgaveService.ferdigstillOppgave(oppgaveId)
    }

    private fun oppdaterOppgaveTilÅGjeldeTilbakekreving(behandlingId: UUID) {
        taskService.save(
            OppdaterOppgaveTilÅGjeldeTilbakekrevingTask.opprettTask(
                behandlingId = behandlingId,
            ),
        )
    }
}
