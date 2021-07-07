package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.api.dto.DokumentinfoDto
import no.nav.familie.ef.sak.api.dto.JournalposterDto
import no.nav.familie.ef.sak.integration.JournalpostClient
import no.nav.familie.kontrakter.felles.BrukerIdType
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.journalpost.JournalposterForBrukerRequest
import no.nav.familie.kontrakter.felles.journalpost.Journalposttype
import no.nav.familie.kontrakter.felles.journalpost.DokumentInfo
import no.nav.familie.kontrakter.felles.journalpost.Bruker
import no.nav.familie.kontrakter.felles.journalpost.Dokumentvariantformat
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class VedleggService(private val behandlingService: BehandlingService,
                     private val journalpostClient: JournalpostClient) {


    fun finnJournalposter(behandlingId: UUID): JournalposterDto {
        val journalposter = behandlingService.hentBehandlingsjournalposter(behandlingId)
        val ident = behandlingService.hentAktivIdent(behandlingId)
        val dokumentinfoDtoList = journalpostClient
                .finnJournalposter(JournalposterForBrukerRequest(brukerId = Bruker(id = ident,
                                                                                   type = BrukerIdType.FNR),
                                                                 antall = 20,
                                                                 tema = listOf(Tema.ENF),
                                                                 journalposttype = Journalposttype.values().toList()))
                .flatMap { journalpost -> journalpost.dokumenter?.map { tilDokumentInfoDto(it, journalpost) } ?: emptyList() }
                .partition { dokumentInfoDto -> journalposter.any { it.journalpostId == dokumentInfoDto.journalpostId } }

        return JournalposterDto(dokumenterKnyttetTilBehandlingen = dokumentinfoDtoList.first,
                                andreDokumenter = dokumentinfoDtoList.second)
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
