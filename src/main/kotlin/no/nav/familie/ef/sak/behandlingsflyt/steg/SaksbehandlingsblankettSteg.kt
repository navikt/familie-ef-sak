package no.nav.familie.ef.sak.behandlingsflyt.steg

import no.nav.familie.ef.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.behandlingsflyt.task.FerdigstillBehandlingTask
import no.nav.familie.ef.sak.blankett.BlankettHelper.lagArkiverBlankettRequestMotNyLøsning
import no.nav.familie.ef.sak.blankett.BlankettService
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.journalføring.JournalpostClient
import no.nav.familie.ef.sak.vedtak.TotrinnskontrollService
import no.nav.familie.kontrakter.felles.dokarkiv.v2.ArkiverDokumentRequest
import no.nav.familie.kontrakter.felles.journalpost.Journalposttype
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class SaksbehandlingsblankettSteg(private val blankettService: BlankettService,
                                  private val taskRepository: TaskRepository,
                                  private val arbeidsfordelingService: ArbeidsfordelingService,
                                  private val totrinnskontrollService: TotrinnskontrollService,
                                  private val journalpostClient: JournalpostClient,
                                  private val behandlingService: BehandlingService,
                                  private val fagsakService: FagsakService) : BehandlingSteg<Void?> {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun utførSteg(saksbehandling: Saksbehandling, data: Void?) {
        if (saksbehandling.erMigrering()) {
            logger.info("Oppretter ikke saksbehandlingsblankett for behandling=${saksbehandling.id}, behandling er migrering")
        } else {
            val blankettPdf = blankettService.lagBlankett(saksbehandling.id)
            logger.info("Journalfører blankett for behandling=${saksbehandling.id}")
            journalførSaksbehandlingsblankett(saksbehandling, blankettPdf)
        }
        opprettFerdigstillBehandlingTask(saksbehandling)
    }

    private fun journalførSaksbehandlingsblankett(saksbehandling: Saksbehandling, blankettPdf: ByteArray) {
        val arkiverDokumentRequest = opprettArkiverDokumentRequest(saksbehandling, blankettPdf)
        val beslutter = totrinnskontrollService.hentBeslutter(saksbehandling.id)

        val journalpostRespons = journalpostClient.arkiverDokument(arkiverDokumentRequest, beslutter)

        behandlingService.leggTilBehandlingsjournalpost(journalpostRespons.journalpostId, Journalposttype.N, saksbehandling.id)
    }

    private fun opprettArkiverDokumentRequest(saksbehandling: Saksbehandling,
                                              blankettPdf: ByteArray): ArkiverDokumentRequest {
        val enhet = arbeidsfordelingService.hentNavEnhetIdEllerBrukMaskinellEnhetHvisNull(saksbehandling.ident)
        return lagArkiverBlankettRequestMotNyLøsning(saksbehandling.ident,
                                                     blankettPdf,
                                                     enhet,
                                                     saksbehandling.eksternFagsakId,
                                                     saksbehandling.id)
    }

    override fun stegType(): StegType {
        return StegType.LAG_SAKSBEHANDLINGSBLANKETT
    }

    private fun opprettFerdigstillBehandlingTask(saksbehandling: Saksbehandling) {
        taskRepository.save(FerdigstillBehandlingTask.opprettTask(saksbehandling))
    }

}