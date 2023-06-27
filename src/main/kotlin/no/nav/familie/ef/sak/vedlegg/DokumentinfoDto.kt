package no.nav.familie.ef.sak.vedlegg

import no.nav.familie.kontrakter.felles.journalpost.AvsenderMottaker
import no.nav.familie.kontrakter.felles.journalpost.Journalposttype
import no.nav.familie.kontrakter.felles.journalpost.Journalstatus
import no.nav.familie.kontrakter.felles.journalpost.Utsendingsinfo
import java.time.LocalDateTime

data class JournalposterDto(
    val dokumenterKnyttetTilBehandlingen: List<DokumentinfoDto>,
    val andreDokumenter: List<DokumentinfoDto>,
)

data class DokumentinfoDto(
    val dokumentinfoId: String,
    val filnavn: String?,
    val tittel: String,
    val journalpostId: String,
    val dato: LocalDateTime?,
    val tema: String?,
    val journalstatus: Journalstatus,
    val journalposttype: Journalposttype,
    val logiskeVedlegg: List<LogiskVedleggDto>,
    val avsenderMottaker: AvsenderMottaker?,
    val utsendingsinfo: Utsendingsinfo?,
)

data class LogiskVedleggDto(val tittel: String)
