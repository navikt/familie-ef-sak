package no.nav.familie.ef.sak.iverksett.oppgaveterminbarn

import no.nav.security.token.support.core.api.Unprotected
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@Unprotected
@RequestMapping(path = ["/api/oppgaver-for-terminbarn"])
class OppgaverTerminbarnDryrunController(private val forberedOppgaverTerminbarnService: ForberedOppgaverTerminbarnService) {

    @GetMapping("/dryrun")
    fun opprettTask(): ResponseEntity<Unit> {
        forberedOppgaverTerminbarnService.forberedOppgaverForUfødteTerminbarn(dryRun = true)
        return ResponseEntity(HttpStatus.OK)
    }
}
