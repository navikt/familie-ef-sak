package no.nav.familie.ef.sak.journalføring

import no.nav.familie.ef.sak.infrastruktur.exception.ApiFeil
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.journalføring.dto.JournalføringRequest
import no.nav.familie.ef.sak.journalføring.dto.UstrukturertDokumentasjonType
import no.nav.familie.ef.sak.journalføring.dto.VilkårsbehandleNyeBarn
import no.nav.familie.kontrakter.ef.sak.DokumentBrevkode
import no.nav.familie.kontrakter.felles.Behandlingstema
import no.nav.familie.kontrakter.felles.BrukerIdType
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.dokarkiv.DokarkivBruker
import no.nav.familie.kontrakter.felles.dokarkiv.DokumentInfo
import no.nav.familie.kontrakter.felles.dokarkiv.OppdaterJournalpostRequest
import no.nav.familie.kontrakter.felles.dokarkiv.Sak
import no.nav.familie.kontrakter.felles.journalpost.Dokumentvariant
import no.nav.familie.kontrakter.felles.journalpost.Dokumentvariantformat
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.familie.kontrakter.felles.journalpost.Journalposttype
import org.springframework.http.HttpStatus

object JournalføringHelper {
    /**
     * [Journalposttype.N] brukes for innskannede dokumentm, samme validering finnes i dokarkiv
     */
    fun validerMottakerFinnes(journalpost: Journalpost) {
        brukerfeilHvis(journalpost.harUgyldigAvsenderMottaker()) {
            "Avsender mangler og må settes på journalposten i gosys. " +
                "Når endringene er gjort, trykker du på \"Lagre utkast\" før du går tilbake til EF Sak og journalfører."
        }
    }

    fun validerJournalføringNyBehandling(
        journalpost: Journalpost,
        journalføringRequest: JournalføringRequest,
    ) {
        val ustrukturertDokumentasjonType = journalføringRequest.behandling.ustrukturertDokumentasjonType
        if (journalpost.harStrukturertSøknad()) {
            feilHvis(ustrukturertDokumentasjonType != UstrukturertDokumentasjonType.IKKE_VALGT) {
                "Kan ikke sende inn dokumentasjonstype når journalposten har strukturert søknad"
            }
            feilHvis(journalføringRequest.vilkårsbehandleNyeBarn != VilkårsbehandleNyeBarn.IKKE_VALGT) {
                "Kan ikke velge å vilkårsbehandle nye barn når man har strukturert søknad"
            }
        } else {
            brukerfeilHvis(ustrukturertDokumentasjonType == UstrukturertDokumentasjonType.IKKE_VALGT) {
                "Må sende inn dokumentasjonstype når journalposten mangler digital søknad"
            }
        }
    }

    fun plukkUtOriginaldokument(
        journalpost: Journalpost,
        dokumentBrevkode: DokumentBrevkode,
    ): no.nav.familie.kontrakter.felles.journalpost.DokumentInfo {
        val dokumenter = journalpost.dokumenter ?: error("Fant ingen dokumenter på journalposten")
        return dokumenter.firstOrNull {
            DokumentBrevkode.erGyldigBrevkode(it.brevkode.toString()) &&
                dokumentBrevkode == DokumentBrevkode.fraBrevkode(it.brevkode.toString()) &&
                harOriginalDokument(it)
        } ?: throw ApiFeil("Det finnes ingen søknad i journalposten for å opprette en ny behandling", HttpStatus.BAD_REQUEST)
    }

    private fun harOriginalDokument(dokument: no.nav.familie.kontrakter.felles.journalpost.DokumentInfo): Boolean =
        dokument.dokumentvarianter?.contains(Dokumentvariant(variantformat = Dokumentvariantformat.ORIGINAL))
            ?: false

    fun lagOppdaterJournalpostRequest(
        journalpost: Journalpost,
        eksternFagsakId: Long,
        dokumenttitler: Map<String, String>?,
    ) = OppdaterJournalpostRequest(
        bruker = journalpost.bruker?.let {
            DokarkivBruker(idType = BrukerIdType.valueOf(it.type.toString()), id = it.id)
        },
        tema = journalpost.tema?.let { Tema.valueOf(it) },
        behandlingstema = journalpost.behandlingstema?.let { Behandlingstema.fromValue(it) },
        tittel = journalpost.tittel,
        journalfoerendeEnhet = journalpost.journalforendeEnhet,
        sak = Sak(
            fagsakId = eksternFagsakId.toString(),
            fagsaksystem = Fagsystem.EF,
            sakstype = "FAGSAK",
        ),
        dokumenter = dokumenttitler?.let {
            journalpost.dokumenter?.map { dokumentInfo ->
                DokumentInfo(
                    dokumentInfoId = dokumentInfo.dokumentInfoId,
                    tittel = dokumenttitler[dokumentInfo.dokumentInfoId]
                        ?: dokumentInfo.tittel,
                    brevkode = dokumentInfo.brevkode,
                )
            }
        },
    )
}
