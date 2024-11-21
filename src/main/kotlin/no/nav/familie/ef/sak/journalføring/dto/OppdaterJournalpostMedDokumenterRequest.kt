package no.nav.familie.ef.sak.journalføring.dto

import no.nav.familie.kontrakter.felles.journalpost.LogiskVedlegg

data class OppdaterJournalpostMedDokumenterRequest(
    val dokumenttitler: Map<String, String>? = null,
    val logiskeVedlegg: Map<String, List<LogiskVedlegg>>? = null,
)
