package no.nav.familie.ef.sak.api.ekstern

import no.nav.familie.ef.sak.integration.PdlClient
import no.nav.familie.ef.sak.integration.dto.pdl.identer
import no.nav.familie.ef.sak.repository.BehandlingRepository
import no.nav.familie.ef.sak.repository.domain.Stønadstype
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/ekstern/behandling"],
                consumes = [MediaType.APPLICATION_JSON_VALUE],
                produces = [MediaType.APPLICATION_JSON_VALUE])
@Validated
class EksternBehandlingController(private val pdlClient: PdlClient,
                                  private val behandlingRepository: BehandlingRepository) {

    /**
     * Blir brukt av mottak for å sjekke om en perosn allerede har en behandling i ef-sak
     * Kunde ha flyttet ut funksjonaliteten i en egen service,
     * men for å unngå att andre bruker den (med kall mot pdl) så ble alt her
     *
     * Hvis man ikke sender type blir alle typer sjekket om det finnes noen.
     */
    @PostMapping("finnes")
    @ProtectedWithClaims(issuer = "azuread", claimMap = ["roles=access_as_application"])
    fun finnesBehandlingForPerson(@RequestParam("type") stønadstype: Stønadstype?,
                                  @RequestBody request: PersonIdent): Ressurs<Boolean> {
        val personidenter = pdlClient.hentPersonidenter(request.ident, historikk = true).identer()
        if(personidenter.isEmpty()) {
            return Ressurs.failure("Finner ikke identer til personen")
        }
        return if (stønadstype != null) {
            Ressurs.success(eksistererBehandlingSomIkkeErBlankett(stønadstype, personidenter))
        } else {
            Ressurs.success(Stønadstype.values().any { eksistererBehandlingSomIkkeErBlankett(it, personidenter) })
        }
    }

    private fun eksistererBehandlingSomIkkeErBlankett(stønadstype: Stønadstype,
                                                      personidenter: Set<String>): Boolean {
        return behandlingRepository.eksistererBehandlingSomIkkeErBlankett(stønadstype, personidenter)
    }

}