package no.nav.familie.ef.sak.journalføring

import no.nav.familie.kontrakter.felles.journalpost.AvsenderMottaker
import no.nav.familie.kontrakter.felles.journalpost.AvsenderMottakerIdType

object JournalføringTestUtil {
    val avsenderMottaker = AvsenderMottaker("12345678901", AvsenderMottakerIdType.FNR, "Navn Navnesen", null, true)
}
