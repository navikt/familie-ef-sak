package no.nav.familie.ef.sak.journalføring

import no.nav.familie.kontrakter.ef.sak.DokumentBrevkode
import no.nav.familie.kontrakter.felles.journalpost.DokumentInfo
import no.nav.familie.kontrakter.felles.journalpost.Dokumentvariantformat
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.familie.kontrakter.felles.journalpost.Journalposttype

fun Journalpost.harStrukturertSøknad(): Boolean =
    this.dokumenter?.any {
        it.harStrukturertSøknad()
    } ?: false

fun DokumentInfo.harStrukturertSøknad() =
    DokumentBrevkode.erGyldigBrevkode(this.brevkode.toString()) &&
        this.harOriginaldokument()

fun DokumentInfo.harOriginaldokument() =
    this.dokumentvarianter?.any { it.variantformat == Dokumentvariantformat.ORIGINAL }
        ?: false

fun Journalpost.harUgyldigAvsenderMottaker(): Boolean = this.journalposttype != Journalposttype.N && this.avsenderMottaker?.navn.isNullOrBlank()

fun Journalpost.manglerAvsenderMottaker(): Boolean = (this.avsenderMottaker?.erLikBruker != true && this.avsenderMottaker?.navn.isNullOrBlank()) || this.avsenderMottaker?.id.isNullOrBlank()
