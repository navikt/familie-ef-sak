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
class BarnController(val barnService: BarnService) {

    @PostMapping("nye-barn")
    fun finnNyeBarnSidenGjeldendeBehandlingForPerson(@RequestBody personIdent: PersonIdent): Ressurs<List<String>> {
        return Ressurs.success(barnService.finnNyeBarnSidenGjeldendeBehandlingForPersonIdent(personIdent))
    }

    @GetMapping("fagsak/{fagsakId}/nye-barn")
    fun finnNyeBarnSidenGjeldendeBehandlingForFagsak(@PathVariable("fagsakId")
                                                     fagsakId: UUID): Ressurs<List<BarnMinimumDto>> {
        return Ressurs.success(barnService.finnNyeBarnSidenGjeldendeBehandlingForFagsak(fagsakId))
    }
}