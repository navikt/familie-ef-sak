package no.nav.familie.ef.sak.journalføring

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.journalføring.JournalføringHelper.validerMottakerFinnes
import no.nav.familie.ef.sak.journalføring.dto.JournalføringKlageRequest
import no.nav.familie.ef.sak.journalføring.dto.JournalføringRequest
import no.nav.familie.ef.sak.journalføring.dto.skalJournalførePåEksisterendeBehandling
import no.nav.familie.ef.sak.klage.KlageService
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class JournalføringKlageService(
    private val behandlingService: BehandlingService,
    private val fagsakService: FagsakService,
    private val oppgaveService: OppgaveService,
    private val journalpostService: JournalpostService,
    private val klageService: KlageService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun fullførJournalpost(journalføringRequest: JournalføringKlageRequest, journalpostId: String) {
        val journalpost = journalpostService.hentJournalpost(journalpostId)
        validerMottakerFinnes(journalpost)

        return if (journalføringRequest.skalJournalførePåEksisterendeBehandling()) {
            journalførSøknadTilEksisterendeBehandling(journalføringRequest, journalpost)
        } else {
            journalførSøknadTilNyBehandling(journalføringRequest, journalpost)
        }
    }

    private fun journalførSøknadTilEksisterendeBehandling(
        journalføringRequest: JournalføringKlageRequest,
        journalpost: Journalpost
    ) {
        val saksbehandler = SikkerhetContext.hentSaksbehandler(true)
        val behandling: Behandling = hentBehandling(journalføringRequest)
        val fagsak = fagsakService.fagsakMedOppdatertPersonIdent(journalføringRequest.fagsakId)
        logger.info(
            "Journalfører journalpost=${journalpost.journalpostId} på eksisterende" +
                " klageBehandling=${journalføringRequest.behandlingId} på " +
                "fagsak=${fagsak.id} stønadstype=${fagsak.stønadstype} "
        )
        knyttJournalpostTilBehandling(journalpost, behandling)
        journalpostService.oppdaterOgFerdigstillJournalpost(
            journalpost = journalpost,
            dokumenttitler = journalføringRequest.dokumentTitler,
            journalførendeEnhet = journalføringRequest.journalførendeEnhet,
            fagsak = fagsak,
            saksbehandler = saksbehandler
        )
        ferdigstillJournalføringsoppgave(journalføringRequest)
    }

    private fun journalførSøknadTilNyBehandling(
        journalføringRequest: JournalføringKlageRequest,
        journalpost: Journalpost
    ) {
        val saksbehandler = SikkerhetContext.hentSaksbehandler(true)
        val fagsak = fagsakService.hentFagsak(journalføringRequest.fagsakId)
        logger.info(
            "Journalfører journalpost=${journalpost.journalpostId} på ny klagebehandling på " +
                "fagsak=${fagsak.id} stønadstype=${fagsak.stønadstype}"
        )

        val klageMottatt = journalpost.datoMottatt?.toLocalDate() ?: journalføringRequest.mottattDato
        brukerfeilHvis(klageMottatt == null) {
            "Mangler dato mottatt"
        }

        klageService.opprettKlage(fagsak, klageMottatt)

        journalpostService.oppdaterOgFerdigstillJournalpost(
            journalpost = journalpost,
            dokumenttitler = journalføringRequest.dokumentTitler,
            journalførendeEnhet = journalføringRequest.journalførendeEnhet,
            fagsak = fagsak,
            saksbehandler = saksbehandler
        )

        ferdigstillJournalføringsoppgave(journalføringRequest)
    }

    private fun ferdigstillJournalføringsoppgave(journalføringRequest: JournalføringKlageRequest) {
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

}
