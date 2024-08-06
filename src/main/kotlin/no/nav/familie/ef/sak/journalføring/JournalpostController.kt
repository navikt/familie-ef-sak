package no.nav.familie.ef.sak.journalføring

import no.nav.familie.ef.sak.AuditLoggerEvent
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvisIkke
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvisIkke
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.Toggle
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.ef.sak.journalføring.dto.JournalføringRequestV2
import no.nav.familie.ef.sak.journalføring.dto.JournalføringResponse
import no.nav.familie.ef.sak.journalføring.dto.JournalføringTilNyBehandlingRequest
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.gjeldende
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.visningsnavn
import no.nav.familie.kontrakter.felles.BrukerIdType
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.journalpost.Dokumentvariantformat
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/journalpost")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class JournalpostController(
    private val journalføringService: JournalføringService,
    private val journalføringKlageService: JournalføringKlageService,
    private val journalpostService: JournalpostService,
    private val personService: PersonService,
    private val tilgangService: TilgangService,
    private val featureToggleService: FeatureToggleService,
) {
    @GetMapping("/{journalpostId}")
    fun hentJournalPost(
        @PathVariable journalpostId: String,
    ): Ressurs<JournalføringResponse> {
        val (journalpost, personIdent) = finnJournalpostOgPersonIdent(journalpostId)
        tilgangService.validerTilgangTilPersonMedBarn(personIdent, AuditLoggerEvent.ACCESS)
        return Ressurs.success(
            JournalføringResponse(
                journalpost,
                personIdent,
                hentBrukersNavn(journalpost, personIdent),
                journalpost.harStrukturertSøknad(),
            ),
        )
    }

    @GetMapping("/{journalpostId}/dokument/{dokumentInfoId}")
    fun hentDokument(
        @PathVariable journalpostId: String,
        @PathVariable dokumentInfoId: String,
    ): Ressurs<ByteArray> {
        val (journalpost, personIdent) = finnJournalpostOgPersonIdent(journalpostId)
        tilgangService.validerTilgangTilPersonMedBarn(personIdent, AuditLoggerEvent.ACCESS)
        validerDokumentKanHentes(journalpost, dokumentInfoId, journalpostId)
        return Ressurs.success(journalpostService.hentDokument(journalpostId, dokumentInfoId))
    }

    @GetMapping(path = ["/{journalpostId}/dokument-pdf/{dokumentInfoId}", "/{journalpostId}/dokument-pdf/{dokumentInfoId}/{filnavn}"], produces = [MediaType.APPLICATION_PDF_VALUE])
    fun hentDokumentSomPdf(
        @PathVariable journalpostId: String,
        @PathVariable dokumentInfoId: String,
    ): ByteArray {
        val (journalpost, personIdent) = finnJournalpostOgPersonIdent(journalpostId)
        tilgangService.validerTilgangTilPersonMedBarn(personIdent, AuditLoggerEvent.ACCESS)
        validerDokumentKanHentes(journalpost, dokumentInfoId, journalpostId)
        return journalpostService.hentDokument(journalpostId, dokumentInfoId)
    }

    @PostMapping("/{journalpostId}/fullfor/v2")
    fun fullførJournalpostV2(
        @PathVariable journalpostId: String,
        @RequestBody journalføringRequest: JournalføringRequestV2,
    ): Ressurs<String> {
        val (journalpost, personIdent) = finnJournalpostOgPersonIdent(journalpostId)
        tilgangService.validerTilgangTilPersonMedBarn(personIdent, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolle()

        if (journalføringRequest.gjelderKlage()) {
            journalføringKlageService.fullførJournalpostV2(journalføringRequest, journalpost)
        } else {
            journalføringService.fullførJournalpostV2(journalføringRequest, journalpost)
        }
        return Ressurs.success(journalpostId)
    }

    @PostMapping("/{journalpostId}/opprett-behandling-med-soknadsdata-fra-en-ferdigstilt-journalpost")
    fun opprettBehandlingMedSøknadsdataFraEnFerdigstiltJournalpost(
        @PathVariable journalpostId: String,
        @RequestBody request: JournalføringTilNyBehandlingRequest,
    ): Ressurs<Long> {
        feilHvisIkke(featureToggleService.isEnabled(Toggle.OPPRETT_BEHANDLING_FERDIGSTILT_JOURNALPOST)) {
            "Funksjonen opprettBehandlingPåFerdigstiltJournalføring er skrudd av for denne brukeren"
        }
        val (journalpost, personIdent) = finnJournalpostOgPersonIdent(journalpostId)
        tilgangService.validerTilgangTilPersonMedBarn(personIdent, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolle()
        brukerfeilHvisIkke(journalpost.harStrukturertSøknad()) { "Journalposten inneholder ikke en digital søknad" }
        return Ressurs.success(
            journalføringService.opprettBehandlingMedSøknadsdataFraEnFerdigstiltJournalpost(
                request,
                journalpostId,
            ),
        )
    }

    private fun validerDokumentKanHentes(
        journalpost: Journalpost,
        dokumentInfoId: String,
        journalpostId: String,
    ) {
        val dokument = journalpost.dokumenter?.find { it.dokumentInfoId == dokumentInfoId }
        feilHvis(dokument == null) {
            "Finner ikke dokument med $dokumentInfoId for journalpost=$journalpostId"
        }
        brukerfeilHvisIkke(dokument.dokumentvarianter?.any { it.variantformat == Dokumentvariantformat.ARKIV } ?: false) {
            "Vedlegget er sannsynligvis under arbeid, må åpnes i gosys"
        }
    }

    private fun finnJournalpostOgPersonIdent(journalpostId: String): Pair<Journalpost, String> {
        val journalpost = journalpostService.hentJournalpost(journalpostId)
        val personIdent =
            journalpost.bruker?.let {
                when (it.type) {
                    BrukerIdType.FNR -> it.id
                    BrukerIdType.AKTOERID -> personService.hentPersonIdenter(it.id).gjeldende().ident
                    BrukerIdType.ORGNR -> error("Kan ikke hente journalpost=$journalpostId for orgnr")
                }
            } ?: error("Kan ikke hente journalpost=$journalpostId uten bruker")
        return Pair(journalpost, personIdent)
    }

    /**
     * Bruker navnet fra avsenderMottaker hvis avsenderMottaker er lik brukeren
     * Hvis ikke så hentes navnet fra PDL
     */
    private fun hentBrukersNavn(
        journalpost: Journalpost,
        personIdent: String,
    ): String =
        journalpost.avsenderMottaker
            ?.takeIf { it.erLikBruker }
            ?.navn
            ?: hentNavnFraPdl(personIdent)

    private fun hentNavnFraPdl(personIdent: String) =
        personService
            .hentPersonKortBolk(listOf(personIdent))
            .getValue(personIdent)
            .navn
            .gjeldende()
            .visningsnavn()
}
