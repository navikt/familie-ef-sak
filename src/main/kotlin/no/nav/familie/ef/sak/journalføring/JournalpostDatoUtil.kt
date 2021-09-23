package no.nav.familie.ef.sak.journalføring

import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import java.time.LocalDateTime

object JournalpostDatoUtil {

    fun mestRelevanteDato(journalpost: Journalpost): LocalDateTime? {
        return journalpost.relevanteDatoer?.maxByOrNull { datoTyperSortert(it.datotype) }?.dato
    }

    private fun datoTyperSortert(datoType: String) = when (datoType) {
        "DATO_JOURNALFOERT" -> 4
        "DATO_REGISTRERT" -> 3
        "DATO_DOKUMENT" -> 2
        "DATO_OPPRETTET" -> 1
        else -> 0
    }
}