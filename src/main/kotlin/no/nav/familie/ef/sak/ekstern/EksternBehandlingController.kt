package no.nav.familie.ef.sak.ekstern

import no.nav.familie.ef.sak.felles.util.FnrUtil
import no.nav.familie.ef.sak.felles.util.FnrUtil.validerIdent
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(
    path = ["/api/ekstern/behandling"],
    consumes = [MediaType.APPLICATION_JSON_VALUE],
    produces = [MediaType.APPLICATION_JSON_VALUE]
)
class EksternBehandlingController(
    private val eksternBehandlingService: EksternBehandlingService
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

    /**
     * Skal bare brukes av familie-ef-mottak for å vurdere om en journalføring skal automatisk ferdigstilles
     * eller manuelt gjennomgås.
     */
    @PostMapping("kan-opprette-forstegangsbehandling")
    @ProtectedWithClaims(issuer = "azuread", claimMap = ["roles=access_as_application"])
    fun kanOppretteFørstegangsbehandling(
        @RequestBody personIdent: PersonIdent,
        @RequestParam type: StønadType
    ): Ressurs<Boolean> {
        if (!SikkerhetContext.kallKommerFraFamilieEfMottak()) {
            throw Feil(message = "Kallet utføres ikke av en autorisert klient", httpStatus = HttpStatus.UNAUTHORIZED)
        }
        validerIdent(personIdent.ident)
        return Ressurs.success(eksternBehandlingService.kanOppretteFørstegangsbehandling(personIdent.ident, type))
    }
}
