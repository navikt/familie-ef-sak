package no.nav.familie.ef.sak.journalføring

import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.infrastruktur.exception.ApiFeil
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.journalføring.dto.JournalføringRequestV2
import no.nav.familie.ef.sak.journalføring.dto.Journalføringsårsak
import no.nav.familie.ef.sak.journalføring.dto.NyAvsender
import no.nav.familie.ef.sak.journalføring.dto.VilkårsbehandleNyeBarn
import no.nav.familie.kontrakter.ef.sak.DokumentBrevkode
import no.nav.familie.kontrakter.felles.Behandlingstema
import no.nav.familie.kontrakter.felles.BrukerIdType
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.dokarkiv.AvsenderMottaker
import no.nav.familie.kontrakter.felles.dokarkiv.DokarkivBruker
import no.nav.familie.kontrakter.felles.dokarkiv.DokumentInfo
import no.nav.familie.kontrakter.felles.dokarkiv.OppdaterJournalpostRequest
import no.nav.familie.kontrakter.felles.dokarkiv.Sak
import no.nav.familie.kontrakter.felles.journalpost.AvsenderMottakerIdType
import no.nav.familie.kontrakter.felles.journalpost.Bruker
import no.nav.familie.kontrakter.felles.journalpost.Dokumentvariantformat
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import org.springframework.http.HttpStatus

object JournalføringHelper {
    fun validerGyldigAvsender(
        journalpost: Journalpost,
        request: JournalføringRequestV2,
    ) {
        if (journalpost.manglerAvsenderMottaker()) {
            brukerfeilHvis(request.nyAvsender == null) {
                "Kan ikke journalføre uten avsender"
            }
            brukerfeilHvis(!request.nyAvsender.erBruker && request.nyAvsender.navn.isNullOrBlank()) {
                "Må sende inn navn på ny avsender"
            }
            brukerfeilHvis(request.nyAvsender.erBruker && request.nyAvsender.personIdent.isNullOrBlank()) {
                "Må sende inn ident på ny avsender hvis det er bruker"
            }
        } else {
            brukerfeilHvis(request.nyAvsender != null) {
                "Kan ikke endre avsender på journalpost som har avsender fra før"
            }
        }
    }

    fun utledNyAvsender(
        nyAvsender: NyAvsender?,
        bruker: Bruker?,
    ): AvsenderMottaker? =
        when (nyAvsender?.erBruker) {
            null -> null
            true -> AvsenderMottaker(id = nyAvsender.personIdent, idType = AvsenderMottakerIdType.FNR, navn = nyAvsender.navn!!)
            false -> AvsenderMottaker(id = null, idType = null, navn = nyAvsender.navn!!)
        }

    fun validerJournalføringNyBehandling(
        journalpost: Journalpost,
        journalføringRequest: JournalføringRequestV2,
    ) {
        val årsak = journalføringRequest.årsak
        if (journalpost.harStrukturertSøknad()) {
            feilHvis(årsak != Journalføringsårsak.DIGITAL_SØKNAD) {
                "Årsak til journalføring må være digital søknad siden det foreligger en digital søknad på journalposten"
            }
            feilHvis(journalføringRequest.vilkårsbehandleNyeBarn != VilkårsbehandleNyeBarn.IKKE_VALGT) {
                "Kan ikke velge å vilkårsbehandle nye barn når man har strukturert søknad"
            }
        } else {
            brukerfeilHvis(årsak == Journalføringsårsak.DIGITAL_SØKNAD) {
                "Må velge mellom PAPIRSØKNAD, ETTERSENDING eller KLAGE når journalposten mangler en digital søknad"
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
        dokument.dokumentvarianter?.any { it.variantformat == Dokumentvariantformat.ORIGINAL }
            ?: false

    fun lagOppdaterJournalpostRequest(
        journalpost: Journalpost,
        eksternFagsakId: Long,
        dokumenttitler: Map<String, String>?,
        nyAvsender: AvsenderMottaker?,
    ) = OppdaterJournalpostRequest(
        avsenderMottaker = nyAvsender,
        bruker =
            journalpost.bruker?.let {
                DokarkivBruker(idType = BrukerIdType.valueOf(it.type.toString()), id = it.id)
            },
        tema = journalpost.tema?.let { Tema.valueOf(it) },
        behandlingstema = journalpost.behandlingstema?.let { Behandlingstema.fromValue(it) },
        tittel = journalpost.tittel ?: "Enslig mor eller far",
        journalfoerendeEnhet = journalpost.journalforendeEnhet,
        sak =
            Sak(
                fagsakId = eksternFagsakId.toString(),
                fagsaksystem = Fagsystem.EF,
                sakstype = "FAGSAK",
            ),
        dokumenter =
            dokumenttitler?.let {
                journalpost.dokumenter?.map { dokumentInfo ->
                    DokumentInfo(
                        dokumentInfoId = dokumentInfo.dokumentInfoId,
                        tittel =
                            dokumenttitler[dokumentInfo.dokumentInfoId]
                                ?: dokumentInfo.tittel,
                        brevkode = dokumentInfo.brevkode,
                    )
                }
            },
    )

    fun lagOppdaterJournalpostRequest(
        journalpost: Journalpost,
        dokumenttitler: Map<String, String>?,
    ) = OppdaterJournalpostRequest(
        dokumenter =
            dokumenttitler?.let {
                journalpost.dokumenter?.map { dokumentInfo ->
                    DokumentInfo(
                        dokumentInfoId = dokumentInfo.dokumentInfoId,
                        tittel =
                            dokumenttitler[dokumentInfo.dokumentInfoId]
                                ?: dokumentInfo.tittel,
                        brevkode = dokumentInfo.brevkode,
                    )
                }
            },
    )

    fun utledNesteBehandlingstype(behandlinger: List<Behandling>): BehandlingType = if (behandlinger.all { it.resultat == BehandlingResultat.HENLAGT }) BehandlingType.FØRSTEGANGSBEHANDLING else BehandlingType.REVURDERING
}
