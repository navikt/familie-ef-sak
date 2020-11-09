package no.nav.familie.ef.sak.api.journalføring

import no.nav.familie.kontrakter.felles.journalpost.Journalpost

data class JournalføringResponse(val journalpost: Journalpost, val personIdent: String)