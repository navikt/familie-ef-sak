package no.nav.familie.ef.sak.journalføring

import no.nav.familie.kontrakter.ef.sak.DokumentBrevkode
import no.nav.familie.kontrakter.felles.Behandlingstema
import no.nav.familie.kontrakter.felles.journalpost.DokumentInfo
import no.nav.familie.kontrakter.felles.journalpost.Dokumentvariantformat
import no.nav.familie.kontrakter.felles.journalpost.Journalpost

fun Journalpost.harStrukturertSøknad(): Boolean {
    val dokumentBrevkode = this.behandlingstema?.let {
        try {
            val behandlingstema = Behandlingstema.fromValue(it)
            finnDokumentBrevkode(behandlingstema)
        } catch (e: Exception) {
            return false
        }
    } ?: return false

    return this.dokumenter?.any {
        it.harStrukturertSøknad(dokumentBrevkode)
    } ?: false
}

fun DokumentInfo.harStrukturertSøknad(dokumentBrevkode: DokumentBrevkode) =
        DokumentBrevkode.erGyldigBrevkode(this.brevkode.toString())
        && dokumentBrevkode == DokumentBrevkode.fraBrevkode(this.brevkode.toString())
        && this.harOriginaldokument()

private fun finnDokumentBrevkode(behandlingstema: Behandlingstema): DokumentBrevkode? {
    return when (behandlingstema) {
        Behandlingstema.Overgangsstønad -> DokumentBrevkode.OVERGANGSSTØNAD
        Behandlingstema.Barnetilsyn -> DokumentBrevkode.BARNETILSYN
        Behandlingstema.Skolepenger -> DokumentBrevkode.SKOLEPENGER
        else -> null
    }
}

fun DokumentInfo.harOriginaldokument() = this.dokumentvarianter?.any { it.variantformat == Dokumentvariantformat.ORIGINAL }
                                         ?: false
