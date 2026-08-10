package no.nav.familie.ef.sak.opplysninger.personopplysninger

import no.nav.familie.ef.sak.AuditLoggerEvent
import no.nav.familie.ef.sak.behandlingsflyt.steg.BehandlerRolle
import no.nav.familie.ef.sak.fagsak.FagsakPersonService
import no.nav.familie.ef.sak.felles.dto.PersonIdentDto
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.PersonopplysningerDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.endringer.EndringerIPersonOpplysningerService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.endringer.EndringerIPersonopplysningerDto
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.navkontor.NavKontorEnhet
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping(path = ["/api/personopplysninger"], produces = [APPLICATION_JSON_VALUE])
@Validated
class PersonopplysningerController(
    private val personopplysningerService: PersonopplysningerService,
    private val endringerIPersonOpplysningerService: EndringerIPersonOpplysningerService,
    private val tilgangService: TilgangService,
    private val fagsakPersonService: FagsakPersonService,
) {
    @GetMapping("/behandling/{behandlingId}")
    fun personopplysninger(
        @PathVariable behandlingId: UUID,
    ): Ressurs<PersonopplysningerDto> {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.ACCESS)
        return Ressurs.success(personopplysningerService.hentPersonopplysningerForBehandling(behandlingId))
    }

    @GetMapping("/behandling/{behandlingId}/endringer")
    fun hentEndringerPersonopplysninger(
        @PathVariable behandlingId: UUID,
    ): Ressurs<EndringerIPersonopplysningerDto> {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.ACCESS)
        return Ressurs.success(endringerIPersonOpplysningerService.hentEndringerPersonopplysninger(behandlingId))
    }

    @GetMapping("/fagsak-person/{fagsakPersonId}")
    fun personopplysningerFraFagsakPersonId(
        @PathVariable fagsakPersonId: UUID,
    ): Ressurs<PersonopplysningerDto> {
        tilgangService.validerTilgangTilFagsakPerson(fagsakPersonId, AuditLoggerEvent.ACCESS)
        val aktivIdent = fagsakPersonService.hentAktivIdent(fagsakPersonId)
        val personopplysninger = personopplysningerService.hentPersonopplysningerFraRegister(aktivIdent)
        validerFullmaktIkkeUkjentForSaksbehandler(personopplysninger)
        return Ressurs.success(personopplysninger)
    }

    /**
     * Om fullmakt er ukjent (`null`) avgjøres alltid av svaret fra tilgangsmaskinen (se [FullmaktClient]) - ikke av
     * hvilken rolle innlogget bruker har. Rollen avgjør kun hvordan dette skal presenteres videre:
     * - Veileder (bl.a. utviklere i produksjon, men også andre som kun har denne rollen) får se personopplysninger
     *   med fullmakt markert som ukjent i grensesnittet.
     * - En reell saksbehandler/beslutter får i stedet en tydelig feilmelding om at fullmaktsdata mangler,
     *   med et hint om årsaken fra tilgangsmaskinen der det er tilgjengelig.
     */
    private fun validerFullmaktIkkeUkjentForSaksbehandler(personopplysninger: PersonopplysningerDto) {
        feilHvis(
            personopplysninger.fullmakt == null && tilgangService.harTilgangTilRolle(BehandlerRolle.SAKSBEHANDLER),
            HttpStatus.FORBIDDEN,
        ) {
            "Du har ikke tilgang til fullmaktsdata for denne personen" +
                (personopplysninger.fullmaktIkkeTilgangÅrsak?.let { " ($it)" } ?: "") +
                "."
        }
    }

    @PostMapping("/nav-kontor")
    fun hentNavKontorTilFagsak(
        @RequestBody personIdent: PersonIdentDto,
    ): Ressurs<NavKontorEnhet?> {
        tilgangService.validerTilgangTilPersonMedBarn(personIdent.personIdent, AuditLoggerEvent.ACCESS)
        return Ressurs.success(personopplysningerService.hentNavKontor(personIdent.personIdent))
    }
}
