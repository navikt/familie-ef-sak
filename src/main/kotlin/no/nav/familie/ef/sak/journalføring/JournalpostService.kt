package no.nav.familie.ef.sak.journalføring

import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.journalføring.dto.DokumentVariantformat
import no.nav.familie.kontrakter.ef.sak.DokumentBrevkode
import no.nav.familie.kontrakter.ef.søknad.SøknadBarnetilsyn
import no.nav.familie.kontrakter.ef.søknad.SøknadOvergangsstønad
import no.nav.familie.kontrakter.ef.søknad.SøknadSkolepenger
import no.nav.familie.kontrakter.felles.BrukerIdType
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.journalpost.Bruker
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.familie.kontrakter.felles.journalpost.JournalposterForBrukerRequest
import no.nav.familie.kontrakter.felles.journalpost.Journalposttype
import no.nav.familie.kontrakter.felles.journalpost.Journalstatus
import org.springframework.stereotype.Service

@Service
class JournalpostService(private val journalpostClient: JournalpostClient) {

    fun hentJournalpost(journalpostId: String): Journalpost {
        return journalpostClient.hentJournalpost(journalpostId)
    }

    fun finnJournalposter(
        personIdent: String,
        antall: Int = 20,
        typer: List<Journalposttype> = Journalposttype.values().toList()
    ): List<Journalpost> {
        return journalpostClient.finnJournalposter(
            JournalposterForBrukerRequest(
                brukerId = Bruker(
                    id = personIdent,
                    type = BrukerIdType.FNR
                ),
                antall = antall,
                tema = listOf(Tema.ENF),
                journalposttype = typer
            )
        )
    }

    fun hentDokument(
        journalpostId: String,
        dokumentInfoId: String,
        dokumentVariantformat: DokumentVariantformat = DokumentVariantformat.ARKIV
    ): ByteArray {
        return journalpostClient.hentDokument(journalpostId, dokumentInfoId, dokumentVariantformat)
    }

    fun hentSøknadFraJournalpostForOvergangsstønad(journalpost: Journalpost): SøknadOvergangsstønad {
        val dokumentinfo = JournalføringHelper.plukkUtOriginaldokument(journalpost, DokumentBrevkode.OVERGANGSSTØNAD)
        return journalpostClient.hentOvergangsstønadSøknad(journalpost.journalpostId, dokumentinfo.dokumentInfoId)
    }

    fun hentSøknadFraJournalpostForBarnetilsyn(journalpost: Journalpost): SøknadBarnetilsyn {
        val dokumentinfo = JournalføringHelper.plukkUtOriginaldokument(journalpost, DokumentBrevkode.BARNETILSYN)
        return journalpostClient.hentBarnetilsynSøknad(journalpost.journalpostId, dokumentinfo.dokumentInfoId)
    }

    fun hentSøknadFraJournalpostForSkolepenger(journalpost: Journalpost): SøknadSkolepenger {
        val dokumentinfo = JournalføringHelper.plukkUtOriginaldokument(journalpost, DokumentBrevkode.SKOLEPENGER)
        return journalpostClient.hentSkolepengerSøknad(journalpost.journalpostId, dokumentinfo.dokumentInfoId)
    }

    fun oppdaterOgFerdigstillJournalpostMaskinelt(
        journalpost: Journalpost,
        journalførendeEnhet: String,
        fagsak: Fagsak
    ) = oppdaterOgFerdigstillJournalpost(
        journalpost = journalpost,
        dokumenttitler = null,
        journalførendeEnhet = journalførendeEnhet,
        fagsak = fagsak,
        saksbehandler = null
    )

    fun oppdaterOgFerdigstillJournalpost(
        journalpost: Journalpost,
        dokumenttitler: Map<String, String>?,
        journalførendeEnhet: String,
        fagsak: Fagsak,
        saksbehandler: String?
    ) {
        if (journalpost.journalstatus != Journalstatus.JOURNALFOERT) {
            oppdaterJournalpostMedFagsakOgDokumenttitler(
                journalpost = journalpost,
                dokumenttitler = dokumenttitler,
                eksternFagsakId = fagsak.eksternId.id,
                saksbehandler = saksbehandler
            )
            ferdigstillJournalføring(
                journalpostId = journalpost.journalpostId,
                journalførendeEnhet = journalførendeEnhet,
                saksbehandler = saksbehandler
            )
        }
    }

    private fun ferdigstillJournalføring(journalpostId: String, journalførendeEnhet: String, saksbehandler: String? = null) {
        journalpostClient.ferdigstillJournalpost(journalpostId, journalførendeEnhet, saksbehandler)
    }

    private fun oppdaterJournalpostMedFagsakOgDokumenttitler(
        journalpost: Journalpost,
        dokumenttitler: Map<String, String>? = null,
        eksternFagsakId: Long,
        saksbehandler: String? = null
    ) {
        val oppdatertJournalpost = JournalføringHelper.lagOppdaterJournalpostRequest(journalpost, eksternFagsakId, dokumenttitler)
        journalpostClient.oppdaterJournalpost(oppdatertJournalpost, journalpost.journalpostId, saksbehandler)
    }
}
