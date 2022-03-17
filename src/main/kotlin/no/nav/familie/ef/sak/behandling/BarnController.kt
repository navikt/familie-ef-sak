package no.nav.familie.ef.sak.behandling

import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.BarnMinimumDto
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping(path = ["/api/behandling/barn"])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class BarnController(val nyeBarnService: NyeBarnService) {

    @Deprecated("nye-eller-tidligere-fodte-barn")
    @PostMapping("nye-barn")
    fun finnNyeBarnSidenGjeldendeBehandlingForPerson(@RequestBody personIdent: PersonIdent): Ressurs<List<String>> {
        return Ressurs.success(nyeBarnService.finnNyeBarnSidenGjeldendeBehandlingForPersonIdent(personIdent))
    }

    @PostMapping("nye-eller-tidligere-fodte-barn")
    // for å unngå att vi oppretter oppgaver for nye barn så sjekkes roles
    @ProtectedWithClaims(issuer = "azuread", claimMap = ["roles=access_as_application"])
    fun finnNyeEllerTidligereFødteBarn(@RequestBody personIdent: PersonIdent): Ressurs<NyeBarnDto> {
        return Ressurs.success(nyeBarnService.finnNyeEllerTidligereFødteBarn(personIdent))
    }

    @GetMapping("fagsak/{fagsakId}/nye-barn")
    fun finnNyeBarnSidenGjeldendeBehandlingForFagsak(@PathVariable("fagsakId")
                                                     fagsakId: UUID): Ressurs<List<BarnMinimumDto>> {
        return Ressurs.success(nyeBarnService.finnNyeBarnSidenGjeldendeBehandlingForFagsak(fagsakId))
    }
}