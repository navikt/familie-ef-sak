package no.nav.familie.ef.sak.ekstern

import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.fagsak.domain.Stønadstype
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PdlClient
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.identer
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.ef.sak.tilkjentytelse.domain.TilkjentYtelse
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping(
    path = ["/api/ekstern/behandling"],
    consumes = [MediaType.APPLICATION_JSON_VALUE],
    produces = [MediaType.APPLICATION_JSON_VALUE]
)
@Validated
class EksternBehandlingController(
    private val pdlClient: PdlClient,
    private val behandlingRepository: BehandlingRepository,
    private val tilkjentYtelseService: TilkjentYtelseService
) {


    /**
     * Blir brukt av mottak for å sjekke om en perosn allerede har en behandling i ef-sak
     * Kunde ha flyttet ut funksjonaliteten i en egen service,
     * men for å unngå att andre bruker den (med kall mot pdl) så ble alt her
     *
     * Hvis man ikke sender type blir alle typer sjekket om det finnes noen.
     */
    @PostMapping("finnes")
    @ProtectedWithClaims(issuer = "azuread", claimMap = ["roles=access_as_application"])
    fun finnesBehandlingForPerson(
        @RequestParam("type") stønadstype: Stønadstype?,
        @RequestBody request: PersonIdent
    ): Ressurs<Boolean> {
        val personidenter = pdlClient.hentPersonidenter(request.ident, historikk = true).identer()
        return finnesBehandlingFor(personidenter, stønadstype)
    }

    /**
     * Hvis man har alle identer til en person så kan man sende inn alle direkte, for å unngå oppslag mot pdl
     * Dette er alltså ikke ett bolk-oppslag for flere ulike personer
     */
    @PostMapping("finnes/flere-identer")
    @ProtectedWithClaims(issuer = "azuread", claimMap = ["roles=access_as_application"])
    fun finnesBehandlingForPersonIdenter(
        @RequestParam("type") stønadstype: Stønadstype?,
        @RequestBody personidenter: Set<String>
    ): Ressurs<Boolean> {
        return finnesBehandlingFor(personidenter, stønadstype)
    }

    @GetMapping("hent/{personidenter}")
    @ProtectedWithClaims(issuer = "azuread", claimMap = ["roles=access_as_application"])
    fun erBehandlingerUtdaterteForPersonidenter(
        @PathVariable personidenter: Set<String>
    ): Ressurs<Boolean> {
        if (personidenter.isEmpty()) {
            return Ressurs.failure("Finner ikke identer til personen")
        }
        if (personidenter.any { it.length != 11 }) {
            return Ressurs.failure("Støtter kun identer av typen fnr/dnr")
        }

        val behandlinger = hentAlleBehandlingIDer(personidenter)
        if (behandlinger.isEmpty()) {
            return Ressurs.failure("Det finnes ingen behandlinger for oppgitte personidenter")
        }
        return erBehandlingerUtdaterte(behandlinger)
    }

    private fun hentAlleBehandlingIDer(personidenter: Set<String>): Set<UUID> {
        var behandlinger = mutableSetOf<UUID>()
        Stønadstype.values().forEach {
            behandlingRepository.finnSisteBehandlingSomIkkeErBlankett(it, personidenter)?.let { behandlinger.add(it.id) }
        }
        return behandlinger
    }

    private fun erBehandlingerUtdaterte(behandlingIDer: Set<UUID>): Ressurs<Boolean> {
        var tilkjenteYtelser = mutableSetOf<TilkjentYtelse>()
        behandlingIDer.forEach { tilkjenteYtelser.add(tilkjentYtelseService.hentForBehandling(it)) }
        val senesteTomDatoAvAndeler : LocalDate = tilkjenteYtelser.maxOf { it.andelerTilkjentYtelse.maxOf { it.stønadTom } }
        return Ressurs.success(senesteTomDatoAvAndeler < LocalDate.now().minusYears(1))
    }

    private fun finnesBehandlingFor(
        personidenter: Set<String>,
        stønadstype: Stønadstype?
    ): Ressurs<Boolean> {
        if (personidenter.isEmpty()) {
            return Ressurs.failure("Finner ikke identer til personen")
        }
        if (personidenter.any { it.length != 11 }) {
            return Ressurs.failure("Støtter kun identer av typen fnr/dnr")
        }

        return if (stønadstype != null) {
            Ressurs.success(eksistererBehandlingSomIkkeErBlankett(stønadstype, personidenter))
        } else {
            Ressurs.success(Stønadstype.values().any { eksistererBehandlingSomIkkeErBlankett(it, personidenter) })
        }
    }

    /**
     * Hvis siste behandling er teknisk opphør, skal vi returnere false,
     * hvis ikke så skal vi returnere true hvis det finnes en behandling
     */
    private fun eksistererBehandlingSomIkkeErBlankett(
        stønadstype: Stønadstype,
        personidenter: Set<String>
    ): Boolean {
        return behandlingRepository.finnSisteBehandlingSomIkkeErBlankett(stønadstype, personidenter)?.let {
            it.type != BehandlingType.TEKNISK_OPPHØR
        } ?: false
    }


}