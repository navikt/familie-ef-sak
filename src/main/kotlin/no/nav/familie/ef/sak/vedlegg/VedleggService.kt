package no.nav.familie.ef.sak.vedlegg

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.Behandlingsjournalpost
import no.nav.familie.ef.sak.journalføring.JournalføringService
import no.nav.familie.kontrakter.felles.journalpost.DokumentInfo
import no.nav.familie.kontrakter.felles.journalpost.Dokumentvariantformat
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class VedleggService(private val behandlingService: BehandlingService,
                     private val journalføringService: JournalføringService) {


    fun finnJournalposter(behandlingId: UUID): JournalposterDto {
        val behandlingsjournalposter = behandlingService.hentBehandlingsjournalposter(behandlingId)
        val journalposter = finnJournalposter(behandlingId, behandlingsjournalposter)

        val dokumentinfoDtoList = journalposter
                .flatMap { journalpost -> journalpost.dokumenter?.map { tilDokumentInfoDto(it, journalpost) } ?: emptyList() }
                .partition { dokumentInfoDto -> behandlingsjournalposter.any { it.journalpostId == dokumentInfoDto.journalpostId } }

        return JournalposterDto(dokumenterKnyttetTilBehandlingen = dokumentinfoDtoList.first,
                                andreDokumenter = dokumentinfoDtoList.second)
    }

    private fun finnJournalposter(behandlingId: UUID,
                                  behandlingsjournalposter: List<Behandlingsjournalpost>): List<Journalpost> {
        val personIdent = behandlingService.hentAktivIdent(behandlingId)
        val sistejournalposter = journalføringService.finnJournalposter(personIdent)

        return sistejournalposter + hentJournalposterTilBehandlingSomIkkeErFunnet(sistejournalposter, behandlingsjournalposter)
    }

    private fun hentJournalposterTilBehandlingSomIkkeErFunnet(sistejournalposter: List<Journalpost>,
                                                              behandlingsjournalposter: List<Behandlingsjournalpost>): List<Journalpost> {
        val journalpostIderFraFunnetJournalposter = sistejournalposter.map { it.journalpostId }
        val behandlingsjournalposterIkkeFunnet =
                behandlingsjournalposter.filterNot { journalpostIderFraFunnetJournalposter.contains(it.journalpostId) }
        return behandlingsjournalposterIkkeFunnet.map { journalføringService.hentJournalpost(it.journalpostId) }
    }

    private fun tilDokumentInfoDto(dokumentInfo: DokumentInfo,
                                   journalpost: Journalpost): DokumentinfoDto {
        return DokumentinfoDto(
                dokumentinfoId = dokumentInfo.dokumentInfoId,
                filnavn = dokumentInfo.dokumentvarianter?.find { it.variantformat == Dokumentvariantformat.ARKIV }?.filnavn,
                tittel = dokumentInfo.tittel ?: "Tittel mangler",
                journalpostId = journalpost.journalpostId,
                dato = journalpost.datoMottatt,
                journalposttype = journalpost.journalposttype
        )
    }

}
