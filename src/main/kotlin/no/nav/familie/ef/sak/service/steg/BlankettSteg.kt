package no.nav.familie.ef.sak.service.steg

import no.nav.familie.ef.sak.api.Feil
import no.nav.familie.ef.sak.integration.JournalpostClient
import no.nav.familie.ef.sak.repository.BehandlingRepository
import no.nav.familie.ef.sak.repository.domain.Behandling
import no.nav.familie.kontrakter.felles.dokarkiv.ArkiverDokumentRequest
import no.nav.familie.kontrakter.felles.dokarkiv.Dokument
import no.nav.familie.kontrakter.felles.dokarkiv.FilType
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.stereotype.Service

@Service
class BlankettSteg(
        private val behandlingRepository: BehandlingRepository,
        private val journalpostClient: JournalpostClient,
        private val taskRepository: TaskRepository) : BehandlingSteg<Void?> {

    override fun validerSteg(behandling: Behandling) {
        if (behandling.steg != stegType()) {
            throw Feil("Behandling er i feil steg=${behandling.steg}")
        }
    }

    override fun utførSteg(behandling: Behandling, data: Void?) {

        val journalpostForBehandling = journalpostClient.hentJournalpost(behandling.journalposter.first().journalpostId)
        val arkiverDokumentRequest = ArkiverDokumentRequest(
                behandlingRepository.finnGjeldendeIdentForBehandling(behandling.id),
                true,
                listOf(Dokument("byteArrayFraDb".toByteArray(),
                                FilType.PDFA,
                                null,
                                "Blankett for overgangsstønad",
                                "OVERGANGSSTØNAD_BLANKETT")), //TODO: Hent blankett fra db
                listOf(),
                journalpostForBehandling.sak?.fagsakId)
        val response = journalpostClient.arkiverDokument(arkiverDokumentRequest)
    }

    override fun stegType(): StegType = StegType.JOURNALFØR_BLANKETT


}