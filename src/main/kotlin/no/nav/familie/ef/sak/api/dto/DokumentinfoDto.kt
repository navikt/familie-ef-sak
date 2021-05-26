package no.nav.familie.ef.sak.api.dto

import no.nav.familie.kontrakter.felles.journalpost.Journalposttype
import java.time.LocalDateTime

data class DokumentinfoDto(val dokumentinfoId: String,
                           val filnavn: String?,
                           val tittel: String,
                           val journalpostId: String,
                           val dato: LocalDateTime?,
                           val journalposttype: Journalposttype)