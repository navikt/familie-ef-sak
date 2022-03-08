package no.nav.familie.ef.sak.blankett

import no.nav.familie.ef.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.behandlingsflyt.steg.BehandlingSteg
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType
import no.nav.familie.ef.sak.behandlingsflyt.task.FerdigstillBehandlingTask
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.journalføring.JournalpostClient
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.ef.sak.vedtak.TotrinnskontrollService
import no.nav.familie.kontrakter.felles.journalpost.Journalposttype
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class BlankettSteg(
        private val behandlingService: BehandlingService,
        private val behandlingRepository: BehandlingRepository,
        private val journalpostClient: JournalpostClient,
        private val arbeidsfordelingService: ArbeidsfordelingService,
        private val blankettRepository: BlankettRepository,
        private val totrinnskontrollService: TotrinnskontrollService,
        private val taskRepository: TaskRepository) : BehandlingSteg<Void?> {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun validerSteg(saksbehandling: Saksbehandling) {
        if (saksbehandling.steg != stegType()) {
            throw Feil("Behandling er i feil steg=${saksbehandling.steg}")
        }
    }

    override fun utførSteg(saksbehandling: Saksbehandling, data: Void?) {
        val journalposter = behandlingService.hentBehandlingsjournalposter(saksbehandling.id)
        val journalpostForBehandling = journalpostClient.hentJournalpost(journalposter.first().journalpostId)
        val personIdent = behandlingRepository.finnAktivIdent(saksbehandling.id)
        val enhet = arbeidsfordelingService.hentNavEnhetIdEllerBrukMaskinellEnhetHvisNull(personIdent)
        val blankettPdf = blankettRepository.findByIdOrThrow(saksbehandling.id).pdf.bytes
        val beslutter = totrinnskontrollService.hentBeslutter(saksbehandling.id)
        if (beslutter == null) {
            logger.info("steg=${stegType()} fant ikke beslutter på behandling=$saksbehandling")
        }

        val arkiverDokumentRequest = BlankettHelper.lagArkiverBlankettRequestMotInfotrygd(personIdent,
                                                                                          blankettPdf,
                                                                                          enhet,
                                                                                          journalpostForBehandling.sak?.fagsakId,
                                                                                          saksbehandling.id)
        val journalpostRespons = journalpostClient.arkiverDokument(arkiverDokumentRequest, beslutter)
        behandlingService.leggTilBehandlingsjournalpost(journalpostRespons.journalpostId, Journalposttype.N, saksbehandling.id)

        ferdigstillBehandling(saksbehandling)
    }


    private fun ferdigstillBehandling(behandling: Saksbehandling) {
        taskRepository.save(FerdigstillBehandlingTask.opprettTask(behandling))
    }

    override fun stegType(): StegType = StegType.JOURNALFØR_BLANKETT


}