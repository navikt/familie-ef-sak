package no.nav.familie.ef.sak.ekstern

import no.nav.familie.ef.sak.AuditLoggerEvent
import no.nav.familie.ef.sak.felles.util.FnrUtil
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvisIkke
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.kontrakter.ef.søknad.KanSendePåminnelseRequest
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.klage.KanOppretteRevurderingResponse
import no.nav.familie.kontrakter.felles.klage.OpprettRevurderingResponse
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(
    path = ["/api/ekstern/behandling"],
    produces = [MediaType.APPLICATION_JSON_VALUE],
)
@ProtectedWithClaims(issuer = "azuread")
class EksternBehandlingController(
    private val tilgangService: TilgangService,
    private val eksternBehandlingService: EksternBehandlingService,
) {

    /**
     * Hvis man har alle identer til en person så kan man sende inn alle direkte, for å unngå oppslag mot pdl
     * Dette er alltså ikke ett bolk-oppslag for flere ulike personer
     */
    @PostMapping("har-loepende-stoenad")
    @ProtectedWithClaims(issuer = "azuread", claimMap = ["roles=access_as_application"])
    fun harAktivStønad(@RequestBody personidenter: Set<String>): Ressurs<Boolean> {
        if (personidenter.isEmpty()) {
            return Ressurs.failure("Minst en ident påkrevd for søk")
        }
        if (personidenter.any { it.length != 11 }) {
            return Ressurs.failure("Støtter kun identer av typen fnr/dnr")
        }
        return Ressurs.success(eksternBehandlingService.harLøpendeStønad(personidenter))
    }

    @PostMapping("har-loepende-barnetilsyn")
    @ProtectedWithClaims(issuer = "azuread", claimMap = ["roles=access_as_application"])
    fun harLøpendeBarnetilsyn(@RequestBody personIdent: PersonIdent): Ressurs<Boolean> {
        return Ressurs.success(eksternBehandlingService.harLøpendeBarnetilsyn(personIdent.ident))
    }

    @GetMapping("kan-opprette-revurdering-klage/{eksternFagsakId}")
    fun kanOppretteRevurdering(@PathVariable eksternFagsakId: Long): Ressurs<KanOppretteRevurderingResponse> {
        tilgangService.validerTilgangTilEksternFagsak(eksternFagsakId, AuditLoggerEvent.CREATE)
        tilgangService.validerHarSaksbehandlerrolle()
        feilHvisIkke(SikkerhetContext.kallKommerFraKlage(), HttpStatus.UNAUTHORIZED) {
            "Kallet utføres ikke av en autorisert klient"
        }
        return Ressurs.success(eksternBehandlingService.kanOppretteRevurdering(eksternFagsakId))
    }

    @PostMapping("opprett-revurdering-klage/{eksternFagsakId}")
    fun opprettRevurderingKlage(@PathVariable eksternFagsakId: Long): Ressurs<OpprettRevurderingResponse> {
        tilgangService.validerTilgangTilEksternFagsak(eksternFagsakId, AuditLoggerEvent.CREATE)
        tilgangService.validerHarSaksbehandlerrolle()
        feilHvisIkke(SikkerhetContext.kallKommerFraKlage(), HttpStatus.UNAUTHORIZED) {
            "Kallet utføres ikke av en autorisert klient"
        }
        return Ressurs.success(eksternBehandlingService.opprettRevurderingKlage(eksternFagsakId))
    }

    @PostMapping("kan-sende-påminnelse-til-bruker")
    @ProtectedWithClaims(issuer = "azuread", claimMap = ["roles=access_as_application"])
    fun kanSendeSmsPåminnelseTilSøker(
        @RequestBody kanSendePåminnelseRequest: KanSendePåminnelseRequest,
    ): Ressurs<Boolean> {
        if (!SikkerhetContext.kallKommerFraFamilieEfMottak()) {
            throw Feil(message = "Kallet utføres ikke av en autorisert klient", httpStatus = HttpStatus.UNAUTHORIZED)
        }
        FnrUtil.validerIdent(kanSendePåminnelseRequest.personIdent)
        return Ressurs.success(
            eksternBehandlingService.tilhørendeBehandleSakOppgaveErPåbegynt(
                kanSendePåminnelseRequest.personIdent,
                kanSendePåminnelseRequest.stønadType,
                kanSendePåminnelseRequest.innsendtSøknadTidspunkt,
            ),
        )
    }
}
