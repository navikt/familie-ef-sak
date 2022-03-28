package no.nav.familie.ef.sak.journalføring

import no.nav.familie.kontrakter.ef.sak.DokumentBrevkode
import no.nav.familie.kontrakter.felles.journalpost.DokumentInfo
import no.nav.familie.kontrakter.felles.journalpost.Dokumentvariantformat
import no.nav.familie.kontrakter.felles.journalpost.Journalpost

fun Journalpost.harStrukturertSøknad(): Boolean = this.dokumenter?.any {
    it.harStrukturertSøknad()
} ?: false

fun DokumentInfo.harStrukturertSøknad() =
        DokumentBrevkode.erGyldigBrevkode(this.brevkode.toString())
        && this.harOriginaldokument()


fun DokumentInfo.harOriginaldokument() = this.dokumentvarianter?.any { it.variantformat == Dokumentvariantformat.ORIGINAL }
                                         ?: false
